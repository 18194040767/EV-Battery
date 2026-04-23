package com.evbattery.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class HashUtil {
    private HashUtil() {
    }

    public static String sha256(String content) {
        return sha256(content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] content) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(content == null ? new byte[0] : content);
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate SHA-256 hash", ex);
        }
    }
}
