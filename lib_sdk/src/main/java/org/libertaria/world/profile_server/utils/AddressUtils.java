package org.libertaria.world.profile_server.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Pattern;

/**
 * Created by Víctor Mars (https://github.com/Yayotron) on 9/1/2018.
 */

public final class AddressUtils {

    private static final String IP_ADDRESS_REGEX = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

    public static boolean isValidIP(String ipAddress) {
        if (ipAddress.isEmpty()) return false;
        final Pattern pattern = Pattern.compile(IP_ADDRESS_REGEX);
        if (!pattern.matcher(ipAddress).matches()) {
            return false;
        }
        return true;
    }

    public static boolean isValidPort(String port) {
        if (port.isEmpty()) return false;
        for (char c : port.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }

    public static boolean isAddressAvailable(String ipAddress, Integer port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ipAddress, port), 600);
            return true;
        } catch (IOException e) {
            return false; // Either timeout or unreachable or failed DNS lookup.
        }
    }
}
