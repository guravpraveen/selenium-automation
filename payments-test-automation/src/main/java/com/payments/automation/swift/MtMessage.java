package com.payments.automation.swift;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parsed SWIFT FIN (MT) message.
 *
 * <pre>
 * {1:F01BANKGB2LAXXX0000000000}            Basic header  - sender LT address
 * {2:I103BANKUS33XXXXN}                    Application   - I/O, message type, receiver
 * {3:{108:MUR123}{121:uetr-uuid}}          User header   - MUR, UETR (gpi), service id
 * {4:
 * :20:REF123
 * :32A:261017USD1000,00
 * -}                                       Text block    - the business fields
 * {5:{CHK:ABC}}                            Trailer
 * </pre>
 */
public class MtMessage {

    private String basicHeader = "";
    private String applicationHeader = "";
    private final Map<String, String> userHeader = new LinkedHashMap<>();
    /** Ordered list because tags may repeat (e.g. :71F: charges, :13C: time indication). */
    private final List<MtField> fields = new ArrayList<>();
    private final Map<String, String> trailer = new LinkedHashMap<>();

    public record MtField(String tag, String value) {
        @Override
        public String toString() {
            return ":" + tag + ":" + value;
        }
    }

    // ------------------------------------------------------------------ header derived values
    /** "103", "202", "940" ... from block 2. */
    public String getMessageType() {
        return applicationHeader.length() >= 4 ? applicationHeader.substring(1, 4) : "";
    }

    public boolean isInput() {
        return applicationHeader.startsWith("I");
    }

    /** Sender BIC (8 or 11) from the LT address in block 1: F01 + 12-char LT address. */
    public String getSenderBic() {
        if (basicHeader.length() < 15) {
            return "";
        }
        String lt = basicHeader.substring(3, 15);
        return bicFromLt(lt);
    }

    /** Receiver BIC from block 2 (input message: I + type + 12-char LT address). */
    public String getReceiverBic() {
        if (isInput() && applicationHeader.length() >= 16) {
            return bicFromLt(applicationHeader.substring(4, 16));
        }
        // Output: O + type + HHMM + MIR(28) ... receiver is in block 1 for output messages
        return getSenderBic();
    }

    private static String bicFromLt(String lt) {
        // LT address = BIC8 + terminal code (1) + branch (3)
        String bic8 = lt.substring(0, 8);
        String branch = lt.substring(9, 12);
        return "XXX".equals(branch) ? bic8 : bic8 + branch;
    }

    public String getUetr() {
        return userHeader.getOrDefault("121", "");
    }

    // ------------------------------------------------------------------ field access
    public Optional<String> getField(String tag) {
        return fields.stream().filter(f -> f.tag().equals(tag)).map(MtField::value).findFirst();
    }

    public String getFieldOrEmpty(String tag) {
        return getField(tag).orElse("");
    }

    public List<String> getFields(String tag) {
        return fields.stream().filter(f -> f.tag().equals(tag)).map(MtField::value).toList();
    }

    /** Matches any option letter: getFieldAnyOption("50") finds 50A, 50F or 50K. */
    public Optional<MtField> getFieldAnyOption(String tagNumber) {
        return fields.stream().filter(f -> f.tag().matches(tagNumber + "[A-Z]?")).findFirst();
    }

    public boolean hasField(String tag) {
        return getField(tag).isPresent();
    }

    public List<MtField> getAllFields() {
        return List.copyOf(fields);
    }

    public MtMessage addField(String tag, String value) {
        fields.add(new MtField(tag, value));
        return this;
    }

    public MtMessage setField(String tag, String value) {
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).tag().equals(tag)) {
                fields.set(i, new MtField(tag, value));
                return this;
            }
        }
        return addField(tag, value);
    }

    public MtMessage removeField(String tag) {
        fields.removeIf(f -> f.tag().equals(tag));
        return this;
    }

    // ------------------------------------------------------------------ getters / setters
    public String getBasicHeader() {
        return basicHeader;
    }

    public MtMessage setBasicHeader(String basicHeader) {
        this.basicHeader = basicHeader;
        return this;
    }

    public String getApplicationHeader() {
        return applicationHeader;
    }

    public MtMessage setApplicationHeader(String applicationHeader) {
        this.applicationHeader = applicationHeader;
        return this;
    }

    public Map<String, String> getUserHeader() {
        return userHeader;
    }

    public Map<String, String> getTrailer() {
        return trailer;
    }

    /** Serialises back to FIN format. */
    public String toFin() {
        StringBuilder sb = new StringBuilder();
        sb.append("{1:").append(basicHeader).append('}');
        sb.append("{2:").append(applicationHeader).append('}');
        if (!userHeader.isEmpty()) {
            sb.append("{3:");
            userHeader.forEach((k, v) -> sb.append('{').append(k).append(':').append(v).append('}'));
            sb.append('}');
        }
        sb.append("{4:\r\n");
        for (MtField f : fields) {
            sb.append(f).append("\r\n");
        }
        sb.append("-}");
        if (!trailer.isEmpty()) {
            sb.append("{5:");
            trailer.forEach((k, v) -> sb.append('{').append(k).append(':').append(v).append('}'));
            sb.append('}');
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return toFin();
    }
}
