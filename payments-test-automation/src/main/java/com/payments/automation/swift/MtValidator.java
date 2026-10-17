package com.payments.automation.swift;

import java.util.List;
import java.util.Set;

/**
 * Subset of SWIFT FIN standards / network-validated rules for MT103 and MT202.
 * Error codes mimic the SWIFT style (T = text, C = conditional, H = header, G = gpi).
 * Extend with your bank's own rules (cut-off times, country specific :59: formats ...).
 */
public final class MtValidator {

    private static final Set<String> BANK_OP_CODES = Set.of("CRED", "CRTS", "SPAY", "SPRI", "SSTD");
    private static final Set<String> CHARGES = Set.of("OUR", "BEN", "SHA");

    private MtValidator() {
    }

    public static ValidationResult validate(MtMessage m) {
        return switch (m.getMessageType()) {
            case "103" -> validateMt103(m);
            case "202" -> validateMt202(m);
            default -> {
                ValidationResult r = new ValidationResult();
                validateCommon(m, r);
                yield r;
            }
        };
    }

    public static ValidationResult validateMt103(MtMessage m) {
        ValidationResult r = new ValidationResult();
        validateCommon(m, r);
        for (String tag : List.of("20", "23B", "32A", "71A")) {
            r.check(m.hasField(tag), "T13", "Mandatory field :" + tag + ": missing");
        }
        r.check(m.getFieldAnyOption("50").isPresent(), "T13", "Mandatory field :50a: (ordering customer) missing");
        r.check(m.getFieldAnyOption("59").isPresent(), "T13", "Mandatory field :59a: (beneficiary) missing");

        m.getField("23B").ifPresent(v -> r.check(BANK_OP_CODES.contains(v), "T36", "Invalid :23B: code " + v));
        String charges = m.getFieldOrEmpty("71A");
        if (!charges.isEmpty()) {
            r.check(CHARGES.contains(charges), "T08", "Invalid :71A: " + charges);
            boolean has71F = m.hasField("71F");
            boolean has71G = m.hasField("71G");
            // Network rule C14 (D50/E13): charges fields must match 71A
            if ("OUR".equals(charges)) {
                r.check(!has71F, "E13", ":71F: not allowed when :71A: is OUR");
            }
            if ("SHA".equals(charges)) {
                r.check(!has71G, "D50", ":71G: not allowed when :71A: is SHA");
            }
            if ("BEN".equals(charges)) {
                r.check(has71F, "E15", "At least one :71F: required when :71A: is BEN");
                r.check(!has71G, "E15", ":71G: not allowed when :71A: is BEN");
            }
        }
        // Rule C1 (D75): if 33B currency differs from 32A currency, 36 exchange rate is mandatory
        m.getField("33B").ifPresent(v33b -> {
            String ccy32 = m.getFieldOrEmpty("32A").length() >= 9 ? m.getFieldOrEmpty("32A").substring(6, 9) : "";
            if (!v33b.startsWith(ccy32)) {
                r.check(m.hasField("36"), "D75", ":36: exchange rate required when :33B: currency differs from :32A:");
            }
        });
        // gpi / SRP: UETR is mandatory for MT103
        r.check(SwiftValidators.isValidUetr(m.getUetr()), "G01", "Block 3 {121:} UETR missing or not a UUID v4");
        return r;
    }

    public static ValidationResult validateMt202(MtMessage m) {
        ValidationResult r = new ValidationResult();
        validateCommon(m, r);
        for (String tag : List.of("20", "21", "32A")) {
            r.check(m.hasField(tag), "T13", "Mandatory field :" + tag + ": missing");
        }
        r.check(m.getFieldAnyOption("58").isPresent(), "T13", "Mandatory field :58a: (beneficiary institution) missing");
        m.getField("21").ifPresent(v -> checkReference(v, "21", r));
        if ("COV".equals(m.getUserHeader().get("119"))) {
            r.check(m.getFieldAnyOption("50").isPresent(), "C1", "MT202 COV requires underlying :50a:");
            r.check(m.getFieldAnyOption("59").isPresent(), "C1", "MT202 COV requires underlying :59a:");
        }
        r.check(SwiftValidators.isValidUetr(m.getUetr()), "G01", "Block 3 {121:} UETR missing or not a UUID v4");
        return r;
    }

    private static void validateCommon(MtMessage m, ValidationResult r) {
        r.check(m.getBasicHeader().startsWith("F01"), "H10", "Block 1 must start with F01");
        r.check(SwiftValidators.isValidBic(m.getSenderBic()), "H20", "Invalid sender BIC " + m.getSenderBic());
        r.check(SwiftValidators.isValidBic(m.getReceiverBic()), "H50", "Invalid receiver BIC " + m.getReceiverBic());
        m.getField("20").ifPresent(v -> checkReference(v, "20", r));
        m.getField("32A").ifPresent(v -> check32A(v, r));
        for (MtMessage.MtField f : m.getAllFields()) {
            r.check(SwiftValidators.isSwiftXCharset(f.value()), "T26", "Field :" + f.tag() + ": has invalid characters");
            if (f.tag().endsWith("A") && f.tag().matches("5[2-8]A")) {
                String bic = f.value().contains("\n") ? f.value().substring(f.value().lastIndexOf('\n') + 1) : f.value();
                r.check(SwiftValidators.isValidBic(bic), "T27", "Invalid BIC in :" + f.tag() + ": " + bic);
            }
        }
    }

    /** :20: / :21: - max 16x, must not start or end with '/' nor contain '//' (rule T26). */
    private static void checkReference(String v, String tag, ValidationResult r) {
        r.check(!v.isEmpty() && v.length() <= 16, "T50", ":" + tag + ": must be 1-16 characters");
        r.check(!v.startsWith("/") && !v.endsWith("/") && !v.contains("//"), "T26",
                ":" + tag + ": must not start/end with '/' or contain '//'");
    }

    /** :32A: = 6!n date + 3!a currency + 15d amount. */
    private static void check32A(String v, ValidationResult r) {
        if (v.length() < 11) {
            r.error("T40", ":32A: too short: " + v);
            return;
        }
        String date = v.substring(0, 6);
        String ccy = v.substring(6, 9);
        String amount = v.substring(9);
        r.check(SwiftValidators.isValidYyMmDd(date), "T50", ":32A: invalid date " + date);
        r.check(SwiftValidators.isValidCurrency(ccy), "T52", ":32A: invalid currency " + ccy);
        r.check(SwiftValidators.isValidMtAmount(amount, ccy), "C03",
                ":32A: invalid amount " + amount + " (decimal comma, max " + SwiftValidators.currencyDecimals(ccy)
                        + " decimals for " + ccy + ")");
    }
}
