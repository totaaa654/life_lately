package com.lifelately.util;

public final class ValidationUtils {
    private ValidationUtils() { }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static String requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }
}
