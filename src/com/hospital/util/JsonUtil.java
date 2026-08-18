package com.hospital.util;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonUtil {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof java.util.Date) {
            return "\"" + DATE_FORMAT.format((java.util.Date) obj) + "\"";
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(escapeJson(String.valueOf(entry.getKey()))).append("\":");
                sb.append(toJson(entry.getValue()));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (obj instanceof Collection) {
            Collection<?> col = (Collection<?>) obj;
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : col) {
                if (!first) sb.append(",");
                sb.append(toJson(item));
                first = false;
            }
            sb.append("]");
            return sb.toString();
        }
        if (obj.getClass().isArray()) {
            Object[] arr = (Object[]) obj;
            return toJson(Arrays.asList(arr));
        }
        // Fallback for custom DTO objects - reflect fields or convert
        return dtoToJson(obj);
    }

    private static String dtoToJson(Object obj) {
        StringBuilder sb = new StringBuilder("{");
        java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
        boolean first = true;
        for (java.lang.reflect.Field f : fields) {
            f.setAccessible(true);
            try {
                Object val = f.get(obj);
                if (!first) sb.append(",");
                sb.append("\"").append(f.getName()).append("\":");
                sb.append(toJson(val));
                first = false;
            } catch (Exception ignored) {}
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseJsonObject(String json) {
        if (json == null || json.trim().isEmpty()) return new HashMap<>();
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return new HashMap<>();

        Map<String, Object> map = new LinkedHashMap<>();
        String content = json.substring(1, json.length() - 1).trim();
        if (content.isEmpty()) return map;

        List<String> tokens = splitTopLevel(content, ',');
        for (String token : tokens) {
            int colonIdx = findColonIndex(token);
            if (colonIdx != -1) {
                String key = token.substring(0, colonIdx).trim();
                String valStr = token.substring(colonIdx + 1).trim();

                if (key.startsWith("\"") && key.endsWith("\"")) {
                    key = key.substring(1, key.length() - 1);
                }

                Object value = parseJsonValue(valStr);
                map.put(key, value);
            }
        }
        return map;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        json = json.trim();
        if (!json.startsWith("[") || !json.endsWith("]")) return new ArrayList<>();

        List<Object> list = new ArrayList<>();
        String content = json.substring(1, json.length() - 1).trim();
        if (content.isEmpty()) return list;

        List<String> tokens = splitTopLevel(content, ',');
        for (String token : tokens) {
            list.add(parseJsonValue(token.trim()));
        }
        return list;
    }

    private static Object parseJsonValue(String val) {
        if (val == null || val.isEmpty() || val.equals("null")) return null;
        if (val.startsWith("\"") && val.endsWith("\"")) {
            return unescapeJson(val.substring(1, val.length() - 1));
        }
        if (val.equalsIgnoreCase("true")) return Boolean.TRUE;
        if (val.equalsIgnoreCase("false")) return Boolean.FALSE;
        if (val.startsWith("{")) return parseJsonObject(val);
        if (val.startsWith("[")) return parseJsonArray(val);
        try {
            if (val.contains(".")) {
                return Double.parseDouble(val);
            } else {
                return Long.parseLong(val);
            }
        } catch (NumberFormatException e) {
            return val;
        }
    }

    private static int findColonIndex(String s) {
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == ':' && !inQuotes) {
                return i;
            }
        }
        return -1;
    }

    private static List<String> splitTopLevel(String s, char delimiter) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        int depthBraces = 0;
        int depthBrackets = 0;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && (i == 0 || s.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (!inQuotes) {
                if (c == '{') depthBraces++;
                else if (c == '}') depthBraces--;
                else if (c == '[') depthBrackets++;
                else if (c == ']') depthBrackets--;
            }

            if (c == delimiter && !inQuotes && depthBraces == 0 && depthBrackets == 0) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private static String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\b", "\b")
                .replace("\\f", "\f")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }
}
