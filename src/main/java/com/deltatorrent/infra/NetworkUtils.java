package com.deltatorrent.infra;

import java.net.DatagramSocket;
import java.net.InetAddress;

public final class NetworkUtils {

    private NetworkUtils() {}

    public static String getLocalIp(String trackerHost, int trackerPort)
            throws Exception {

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(
                InetAddress.getByName(trackerHost),
                trackerPort
            );

            return socket.getLocalAddress().getHostAddress();
        }
    }
}
