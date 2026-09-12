package com.ThoughtsAndOpinions.IdentityService.utils;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;

public class CursorUtils {

    /**
     * Converts an OffsetDateTime into an opaque Base64 cursor string.
     */
    public static String encodeCursor(OffsetDateTime timestamp) {
        if (timestamp == null) {
            return "";
        }
        // Convert timestamp to ISO-8601 String string (e.g., "2026-09-12T21:15:00.123Z")
        String timestampStr = timestamp.toString();

        // Encode to Base64 bytes and wrap as a string
        return Base64.getEncoder().encodeToString(timestampStr.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a Base64 cursor string back into a concrete OffsetDateTime object.
     */
    public static OffsetDateTime decodeCursor(String cursorToken) {
        if (cursorToken == null || cursorToken.isBlank()) {
            // If the user is on page 1, default to the current time so they get the newest rows
            return OffsetDateTime.now();
        }

        // 1. Decode from Base64 back to raw bytes
        byte[] decodedBytes = Base64.getDecoder().decode(cursorToken);
        String timestampStr = new String(decodedBytes, StandardCharsets.UTF_8);

        // 2. Parse the string directly back into an OffsetDateTime object
        return OffsetDateTime.parse(timestampStr);
    }
}
