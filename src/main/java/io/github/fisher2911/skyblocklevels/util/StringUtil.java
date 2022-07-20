package io.github.fisher2911.skyblocklevels.util;

public class StringUtil {

    public static String formatId(String id) {
        if (id.isBlank()) return id;
        return String.join(" ", capitalize(id.toLowerCase().split("[- ]")));
    }

    private static String[] capitalize(String[] parts) {
        for (int i = 0; i < parts.length; i++) {
            parts[i] = capitalize(parts[i]);
        }
        return parts;
    }

    public static String capitalize(String string) {
        if (string.isBlank()) return string;
        final char first = string.charAt(0);
        final char[] chars = string.toCharArray();
        chars[0] = Character.toUpperCase(first);
        return new String(chars);
    }

}
