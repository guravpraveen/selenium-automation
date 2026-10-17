package com.payments.automation.swift;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses raw FIN text into an {@link MtMessage}. Handles CRLF / LF line endings and multi-line fields. */
public final class MtParser {

    private static final Pattern BLOCK = Pattern.compile("\\{([1-5]):");
    private static final Pattern SUB_BLOCK = Pattern.compile("\\{([A-Za-z0-9]+):([^}]*)}");
    private static final Pattern FIELD_START = Pattern.compile("^:(\\d{2}[A-Z]?):(.*)$");

    private MtParser() {
    }

    public static MtMessage parse(String fin) {
        if (fin == null || fin.isBlank()) {
            throw new SwiftValidationException("Empty MT message");
        }
        String text = fin.replace("\r\n", "\n").trim();
        MtMessage msg = new MtMessage();
        Matcher m = BLOCK.matcher(text);
        int pos = 0;
        while (m.find(pos)) {
            String id = m.group(1);
            int contentStart = m.end();
            int contentEnd = id.equals("4") ? text.indexOf("\n-}", contentStart) : matchingBrace(text, m.start());
            if (contentEnd < 0) {
                throw new SwiftValidationException("Block " + id + " is not terminated");
            }
            String content = text.substring(contentStart, contentEnd);
            switch (id) {
                case "1" -> msg.setBasicHeader(content);
                case "2" -> msg.setApplicationHeader(content);
                case "3" -> readSubBlocks(content, msg.getUserHeader());
                case "4" -> readFields(content, msg);
                case "5" -> readSubBlocks(content, msg.getTrailer());
                default -> { }
            }
            pos = id.equals("4") ? contentEnd + 3 : contentEnd + 1;
        }
        if (msg.getAllFields().isEmpty()) {
            throw new SwiftValidationException("Block 4 (text block) missing or empty");
        }
        return msg;
    }

    private static int matchingBrace(String text, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static void readSubBlocks(String content, java.util.Map<String, String> target) {
        Matcher m = SUB_BLOCK.matcher(content);
        while (m.find()) {
            target.put(m.group(1), m.group(2));
        }
    }

    private static void readFields(String content, MtMessage msg) {
        String currentTag = null;
        StringBuilder value = new StringBuilder();
        for (String line : content.split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            Matcher fm = FIELD_START.matcher(line);
            if (fm.matches()) {
                if (currentTag != null) {
                    msg.addField(currentTag, value.toString());
                }
                currentTag = fm.group(1);
                value = new StringBuilder(fm.group(2));
            } else if (currentTag != null) {
                value.append('\n').append(line); // continuation line of a multi-line field (50K, 59, 70 ...)
            }
        }
        if (currentTag != null) {
            msg.addField(currentTag, value.toString());
        }
    }
}
