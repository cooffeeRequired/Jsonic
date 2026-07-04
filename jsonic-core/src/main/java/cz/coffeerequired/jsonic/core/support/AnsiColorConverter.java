package cz.coffeerequired.jsonic.core.support;

import java.util.AbstractMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AnsiColorConverter {

    public static final String RESET = "\u001B[0m";

    private static final Map<Character, String> ANSI_CODES = Map.ofEntries(
            new AbstractMap.SimpleEntry<>('0', "\u001B[30m"),
            new AbstractMap.SimpleEntry<>('1', "\u001B[34m"),
            new AbstractMap.SimpleEntry<>('2', "\u001B[32m"),
            new AbstractMap.SimpleEntry<>('3', "\u001B[36m"),
            new AbstractMap.SimpleEntry<>('4', "\u001B[31m"),
            new AbstractMap.SimpleEntry<>('5', "\u001B[35m"),
            new AbstractMap.SimpleEntry<>('6', "\u001B[33m"),
            new AbstractMap.SimpleEntry<>('7', "\u001B[37m"),
            new AbstractMap.SimpleEntry<>('8', "\u001B[90m"),
            new AbstractMap.SimpleEntry<>('9', "\u001B[94m"),
            new AbstractMap.SimpleEntry<>('a', "\u001B[92m"),
            new AbstractMap.SimpleEntry<>('b', "\u001B[96m"),
            new AbstractMap.SimpleEntry<>('c', "\u001B[91m"),
            new AbstractMap.SimpleEntry<>('d', "\u001B[95m"),
            new AbstractMap.SimpleEntry<>('e', "\u001B[93m"),
            new AbstractMap.SimpleEntry<>('f', "\u001B[97m"),
            new AbstractMap.SimpleEntry<>('r', RESET),
            new AbstractMap.SimpleEntry<>('l', "\u001B[1m"),
            new AbstractMap.SimpleEntry<>('o', "\u001B[3m"),
            new AbstractMap.SimpleEntry<>('n', "\u001B[4m"),
            new AbstractMap.SimpleEntry<>('m', "\u001B[9m"),
            new AbstractMap.SimpleEntry<>('k', "\u001B[8m")
    );

    private static final Pattern COLOR_PATTERN = Pattern.compile("&([0-9a-fl-r])|&#([0-9a-fA-F]{6})");

    private AnsiColorConverter() {
    }

    public static String convertToAnsi(String text) {
        Matcher matcher = COLOR_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String replacement;
            if (matcher.group(1) != null) {
                char colorCode = matcher.group(1).charAt(0);
                replacement = ANSI_CODES.getOrDefault(colorCode, "");
            } else if (matcher.group(2) != null) {
                replacement = hexToAnsi(matcher.group(2));
            } else {
                replacement = "";
            }
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        return result + RESET;
    }

    public static String hexToAnsi(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return String.format("\u001B[38;2;%d;%d;%dm", r, g, b);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    public static String colorize(String text, String color) {
        return color + text + RESET;
    }

    public static String colorizeHttpMethod(String method) {
        return switch (method.trim().toUpperCase()) {
            case "GET" -> colorize("GET", "\u001B[32m");
            case "POST" -> colorize("POST", "\u001B[34m");
            case "PUT" -> colorize("PUT", "\u001B[90m");
            case "DELETE" -> colorize("DELETE", "\u001B[31m");
            case "PATCH" -> colorize("PATCH", "\u001B[33m");
            case "HEAD" -> colorize("HEAD", "\u001B[36m");
            default -> method;
        };
    }
}
