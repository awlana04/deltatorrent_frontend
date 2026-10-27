package com.deltatorrent.infra;

import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FileLeecher {

    private static final long SLICE_SIZE = 524288L;
    private static final int BUFFER_SIZE = 4096;
    private static final int SOCKET_TIMEOUT_MS = 30000;

    public static class DownloadTask {

        private final AtomicBoolean paused =
            new AtomicBoolean(false);

        private final AtomicBoolean cancelled =
            new AtomicBoolean(false);

        private final Set<Long> completedSlices =
            new HashSet<>();

        private volatile Socket currentSocket;
        private volatile boolean running;

        public void pause() {
            if (!cancelled.get()) {
                paused.set(true);
                closeCurrentSocket();
            }
        }

        public void resume() {
            if (!cancelled.get()) {
                paused.set(false);
            }
        }

        public void cancel() {
            cancelled.set(true);
            paused.set(false);
            closeCurrentSocket();
        }

        public boolean isPaused() {
            return paused.get();
        }

        public boolean isCancelled() {
            return cancelled.get();
        }

        public boolean isRunning() {
            return running;
        }

        private void closeCurrentSocket() {
            Socket socket = currentSocket;

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }

        private synchronized boolean markCompleted(long slice) {
            return completedSlices.add(slice);
        }

        private synchronized boolean isCompleted(long slice) {
            return completedSlices.contains(slice);
        }

        private synchronized long getCompletedBytes(
            long totalSize
        ) {
            long bytes = 0;

            for (Long slice : completedSlices) {
                long position = slice * SLICE_SIZE;

                if (position < totalSize) {
                    bytes += Math.min(
                        SLICE_SIZE,
                        totalSize - position
                    );
                }
            }

            return bytes;
        }
    }

    public static DownloadTask downloadFileP2P(
        String ownerIP,
        int ownerPort,
        String fileName,
        long totalSize,
        Consumer<Double> onProgress,
        Consumer<String> onStatus
    ) {
        DownloadTask task = new DownloadTask();

        Thread thread = new Thread(() -> {
            task.running = true;

            try {
                if (ownerIP == null || ownerIP.isBlank()
                        || ownerPort < 1 || ownerPort > 65535
                        || fileName == null || fileName.isBlank()
                        || totalSize < 0) {
                    throw new IllegalArgumentException(
                        "Invalid download information."
                    );
                }

                Path folder = Paths.get("./my_downloads")
                    .toAbsolutePath()
                    .normalize();

                Files.createDirectories(folder);

                Path suppliedName = Paths.get(fileName);
                Path baseName = suppliedName.getFileName();

                if (baseName == null) {
                    throw new IllegalArgumentException(
                        "Invalid file name."
                    );
                }

                String safeName = baseName.toString();

                if (safeName.isBlank()
                        || safeName.equals(".")
                        || safeName.equals("..")) {
                    throw new IllegalArgumentException(
                        "Invalid file name."
                    );
                }

                Path destination = folder.resolve(
                    "downloaded_" + safeName
                ).normalize();

                if (!destination.startsWith(folder)) {
                    throw new SecurityException(
                        "Invalid destination path."
                    );
                }

                long totalSlices =
                    totalSize / SLICE_SIZE
                    + (totalSize % SLICE_SIZE == 0 ? 0 : 1);

                try (RandomAccessFile raf =
                        new RandomAccessFile(
                            destination.toFile(),
                            "rw"
                        )) {

                    if (raf.length() != totalSize) {
                        raf.setLength(totalSize);
                    }

                    while (true) {

                        if (task.isCancelled()) {
                            notifyStatus(
                                onStatus,
                                "Cancelled"
                            );
                            return;
                        }

                        if (task.isPaused()) {
                            notifyStatus(
                                onStatus,
                                "Paused"
                            );

                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }

                            continue;
                        }

                        long nextSlice = -1;

                        for (long i = 0; i < totalSlices; i++) {
                            if (!task.isCompleted(i)) {
                                nextSlice = i;
                                break;
                            }
                        }

                        if (nextSlice == -1) {
                            break;
                        }

                        long position = nextSlice * SLICE_SIZE;

                        long expectedBytes = Math.min(
                            SLICE_SIZE,
                            totalSize - position
                        );

                        try (Socket socket = new Socket(
                                ownerIP,
                                ownerPort
                            )) {

                            task.currentSocket = socket;

                            if (task.isPaused()
                                    || task.isCancelled()) {
                                continue;
                            }

                            socket.setSoTimeout(
                                SOCKET_TIMEOUT_MS
                            );

                            try (
                                DataOutputStream output =
                                    new DataOutputStream(
                                        socket.getOutputStream()
                                    );

                                InputStream input =
                                    socket.getInputStream()
                            ) {

                                output.writeUTF(safeName);
                                output.writeInt((int) nextSlice);
                                output.flush();

                                raf.seek(position);

                                long received = 0;
                                byte[] buffer =
                                    new byte[BUFFER_SIZE];

                                while (received < expectedBytes) {

                                    if (task.isPaused()
                                            || task.isCancelled()) {
                                        break;
                                    }

                                    int count = input.read(
                                        buffer,
                                        0,
                                        (int) Math.min(
                                            buffer.length,
                                            expectedBytes - received
                                        )
                                    );

                                    if (count == -1) {
                                        throw new EOFException(
                                            "Seeder disconnected."
                                        );
                                    }

                                    raf.write(buffer, 0, count);
                                    received += count;
                                }

                                if (received == expectedBytes) {

                                    task.markCompleted(nextSlice);

                                    if (onProgress != null) {
                                        onProgress.accept(
                                            (double)
                                                task.getCompletedBytes(
                                                    totalSize
                                                )
                                            / Math.max(1L, totalSize)
                                        );
                                    }
                                }
                            }

                        } catch (Exception e) {

                            if (task.isCancelled()) {
                                notifyStatus(
                                    onStatus,
                                    "Cancelled"
                                );
                                return;
                            }

                            if (task.isPaused()) {
                                continue;
                            }

                            throw e;

                        } finally {
                            task.currentSocket = null;
                        }

                        if (!task.isPaused()
                                && !task.isCancelled()) {
                            notifyStatus(
                                onStatus,
                                "Downloading"
                            );
                        }
                    }
                }

                if (task.isCancelled()) {
                    notifyStatus(onStatus, "Cancelled");

                } else if (task.isPaused()) {
                    notifyStatus(onStatus, "Paused");

                } else {
                    if (onProgress != null) {
                        onProgress.accept(1.0);
                    }

                    notifyStatus(onStatus, "Completed");
                }

            } catch (Exception error) {

                if (task.isCancelled()) {
                    notifyStatus(onStatus, "Cancelled");

                } else if (task.isPaused()) {
                    notifyStatus(onStatus, "Paused");

                } else {
                    notifyStatus(
                        onStatus,
                        "Error: " + error.getMessage()
                    );
                }

            } finally {
                task.currentSocket = null;
                task.running = false;
            }

        }, "File-Leecher-Thread");

        thread.setDaemon(true);
        thread.start();

        return task;
    }

    private static void notifyStatus(
        Consumer<String> onStatus,
        String status
    ) {
        if (onStatus != null) {
            onStatus.accept(status);
        }
    }
}
