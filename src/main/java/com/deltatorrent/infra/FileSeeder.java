package com.deltatorrent.infra;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileSeeder implements Runnable, AutoCloseable {

    private static final long SLICE_SIZE = 524288L;
    private static final int BUFFER_SIZE = 4096;
    private static final int SOCKET_TIMEOUT_MS = 30000;

    private final int portP2P;
    private final Path sharedFolder;

    private final AtomicBoolean running =
        new AtomicBoolean(false);

    private final AtomicBoolean started =
        new AtomicBoolean(false);

    private final CountDownLatch portReady =
        new CountDownLatch(1);

    private final ExecutorService transferPool =
        Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(
                runnable,
                "File-Seeder-Transfer"
            );
            thread.setDaemon(true);
            return thread;
        });

    private volatile ServerSocket serverSocket;
    private volatile int actualPort = -1;
    private volatile IOException startupError;

    public FileSeeder(int port, String folderPath) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException(
                "Invalid seeder port."
            );
        }

        if (folderPath == null || folderPath.isBlank()) {
            throw new IllegalArgumentException(
                "Shared folder cannot be empty."
            );
        }

        this.portP2P = port;

        this.sharedFolder = Paths.get(folderPath)
            .toAbsolutePath()
            .normalize();
    }

    @Override
    public void run() {
        if (!started.compareAndSet(false, true)) {
            System.err.println(
                "[P2P SEEDER] Seeder has already been started."
            );
            return;
        }

        try {
            Files.createDirectories(sharedFolder);

            try (ServerSocket server = new ServerSocket(
                    portP2P,
                    50,
                    InetAddress.getByName("0.0.0.0"))) {

                serverSocket = server;
                actualPort = server.getLocalPort();
                running.set(true);

                // Informa que a porta está pronta.
                portReady.countDown();

                System.out.println(
                    "[P2P SEEDER] Listening on port "
                    + actualPort
                );

                System.out.println(
                    "[P2P SEEDER] Shared folder: "
                    + sharedFolder
                );

                while (running.get()) {
                    try {
                        Socket client = server.accept();

                        try {
                            transferPool.execute(
                                () -> processShipment(client)
                            );
                        } catch (RuntimeException e) {
                            try {
                                client.close();
                            } catch (IOException ignored) {
                            }

                            if (running.get()) {
                                System.err.println(
                                    "[P2P SEEDER] Could not schedule "
                                    + "transfer: " + e.getMessage()
                                );
                            }
                        }

                    } catch (SocketException e) {
                        if (running.get()) {
                            System.err.println(
                                "[P2P SEEDER] Socket error: "
                                + e.getMessage()
                            );
                        }
                        break;

                    } catch (IOException e) {
                        if (running.get()) {
                            System.err.println(
                                "[P2P SEEDER] Accept failed: "
                                + e.getMessage()
                            );
                        }
                    }
                }

            }

        } catch (IOException e) {
            startupError = e;

            System.err.println(
                "[P2P SEEDER] Could not start server on port "
                + portP2P + ": " + e.getMessage()
            );

        } finally {
            running.set(false);
            serverSocket = null;

            // Libera quem estiver esperando a inicialização,
            // inclusive quando ocorrer uma falha.
            portReady.countDown();

            transferPool.shutdownNow();

            System.out.println(
                "[P2P SEEDER] Server stopped."
            );
        }
    }

    public int awaitPort(
        int timeout,
        TimeUnit unit
    ) throws InterruptedException {

        if (unit == null) {
            throw new IllegalArgumentException(
                "Time unit cannot be null."
            );
        }

        if (timeout < 0) {
            throw new IllegalArgumentException(
                "Timeout cannot be negative."
            );
        }

        boolean ready = portReady.await(timeout, unit);

        if (!ready) {
            return -1;
        }

        if (startupError != null || !running.get()) {
            return -1;
        }

        ServerSocket server = serverSocket;

        if (server == null || server.isClosed()) {
            return -1;
        }

        return actualPort;
    }

    private void processShipment(Socket socket) {
        String clientIP = socket.getInetAddress()
            .getHostAddress();

        try (Socket client = socket) {
            client.setSoTimeout(SOCKET_TIMEOUT_MS);

            try (
                DataInputStream input = new DataInputStream(
                    client.getInputStream()
                );
                OutputStream output = client.getOutputStream()
            ) {
                String fileName = input.readUTF();
                int sliceIndex = input.readInt();

                // Aceita apenas nomes simples de arquivos.
                if (fileName == null || fileName.isBlank()
                        || fileName.equals(".")
                        || fileName.equals("..")
                        || fileName.contains("/")
                        || fileName.contains("\\")
                        || Paths.get(fileName).getNameCount() != 1) {

                    System.err.println(
                        "[P2P UPLOAD] Invalid file name from "
                        + clientIP
                    );
                    return;
                }

                Path filePath = sharedFolder
                    .resolve(fileName)
                    .normalize();

                if (!filePath.startsWith(sharedFolder)) {
                    System.err.println(
                        "[P2P UPLOAD] Invalid path requested."
                    );
                    return;
                }

                File file = filePath.toFile();

                if (!file.isFile() || !file.canRead()) {
                    System.err.println(
                        "[P2P UPLOAD] File unavailable: "
                        + fileName
                    );
                    return;
                }

                long fileSize = file.length();

                long totalSlices =
                    fileSize / SLICE_SIZE
                    + (fileSize % SLICE_SIZE == 0 ? 0 : 1);

                if (sliceIndex < 0
                        || (long) sliceIndex >= totalSlices) {
                    System.err.println(
                        "[P2P UPLOAD] Invalid slice "
                        + sliceIndex + " for " + fileName
                    );
                    return;
                }

                long startPosition =
                    (long) sliceIndex * SLICE_SIZE;

                long bytesToSend = Math.min(
                    SLICE_SIZE,
                    fileSize - startPosition
                );

                System.out.println(
                    "[P2P UPLOAD] Sending slice "
                    + sliceIndex + " of " + fileName
                    + " to " + clientIP
                );

                try (RandomAccessFile raf =
                        new RandomAccessFile(file, "r")) {

                    raf.seek(startPosition);

                    byte[] buffer = new byte[BUFFER_SIZE];
                    long totalBytesSent = 0;

                    while (totalBytesSent < bytesToSend) {
                        int bytesRead = raf.read(
                            buffer,
                            0,
                            (int) Math.min(
                                buffer.length,
                                bytesToSend - totalBytesSent
                            )
                        );

                        if (bytesRead == -1) {
                            throw new EOFException(
                                "Unexpected end of file."
                            );
                        }

                        output.write(buffer, 0, bytesRead);
                        totalBytesSent += bytesRead;
                    }

                    output.flush();

                    System.out.println(
                        "[P2P UPLOAD] Sent "
                        + totalBytesSent + " bytes to "
                        + clientIP
                    );
                }
            }

        } catch (Exception e) {
            System.err.println(
                "[P2P UPLOAD] Transfer to "
                + clientIP + " failed: "
                + e.getMessage()
            );
        }
    }

    public int getPort() {
        return actualPort > 0 ? actualPort : portP2P;
    }

    public String getSharedFolder() {
        return sharedFolder.toString();
    }

    public boolean isRunning() {
        return running.get();
    }

    public IOException getStartupError() {
        return startupError;
    }

    public void stop() {
        close();
    }

    @Override
    public void close() {
        running.set(false);

        ServerSocket server = serverSocket;

        if (server != null) {
            try {
                server.close();
            } catch (IOException ignored) {
            }
        }

        // Evita que awaitPort fique bloqueado após o encerramento.
        portReady.countDown();

        transferPool.shutdownNow();
    }
}
