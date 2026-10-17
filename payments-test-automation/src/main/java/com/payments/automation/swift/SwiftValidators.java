package com.payments.automation.swift;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Pattern;

/** Field-level rules shared by MT and MX validation. */
public final class SwiftValidators {

    /** 4 bank + 2 country + 2 location + optional 3 branch. */
    private static final Pattern BIC = Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$");
    private static final Pattern UETR = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    private static final Pattern IBAN = Pattern.compile("^[A-Z]{2}[0-9]{2}[A-Z0-9]{11,30}$");
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");
    /** SWIFT X character set used in MT text fields. */
    private static final Pattern X_CHARSET = Pattern.compile("^[A-Za-z0-9/\\-?:().,'+ \\n]*$");

    /** ISO 4217 minor units for currencies common in CBPR+ / RTGS testing. Default 2. */
    private static final Map<String, Integer> DECIMALS = Map.ofEntries(
            Map.entry("JPY", 0), Map.entry("KRW", 0), Map.entry("VND", 0), Map.entry("IDR", 2),
            Map.entry("KWD", 3), Map.entry("BHD", 3), Map.entry("OMR", 3), Map.entry("JOD", 3),
            Map.entry("TND", 3), Map.entry("CLP", 0), Map.entry("ISK", 0), Map.entry("UGX", 0));

    private SwiftValidators() {
    }

    public static boolean isValidBic(String bic) {
        return bic != null && BIC.matcher(bic).matches();
    }

    public static boolean isValidUetr(String uetr) {
        return uetr != null && UETR.matcher(uetr).matches();
    }

    public static boolean isValidCurrency(String ccy) {
        return ccy != null && CURRENCY.matcher(ccy).matches();
    }

    public static boolean isSwiftXCharset(String text) {
        return text != null && X_CHARSET.matcher(text).matches();
    }

    /** IBAN check digits (ISO 13616 mod-97). */
    public static boolean isValidIban(String iban) {
        if (iban == null) {
            return false;
        }
        String s = iban.replace(" ", "").toUpperCase();
        if (!IBAN.matcher(s).matches()) {
            return false;
        }
        String rearranged = s.substring(4) + s.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            numeric.append(Character.isLetter(c) ? String.valueOf(c - 'A' + 10) : String.valueOf(c));
        }
        return new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }

    public static int currencyDecimals(String ccy) {
        return DECIMALS.getOrDefault(ccy, 2);
    }

    /** MT amount: digits with mandatory decimal comma, max 15 chars, e.g. "1000," or "1250,50". */
    public static boolean isValidMtAmount(String amount, String ccy) {
        if (amount == null || amount.length() > 15 || !amount.matches("^\\d+,\\d*$")) {
            return false;
        }
        String decimals = amount.substring(amount.indexOf(',') + 1);
        return decimals.length() <= currencyDecimals(ccy);
    }

    /** MX amount: dot decimal, max 18 digits total, decimals per currency. */
    public static boolean isValidMxAmount(String amount, String ccy) {
        if (amount == null || !amount.matches("^\\d{1,18}(\\.\\d+)?$")) {
            return false;
        }
        return new BigDecimal(amount).scale() <= currencyDecimals(ccy);
    }

    public static boolean isValidYyMmDd(String date) {
        try {
            LocalDate.parse(date, DateTimeFormatter.ofPattern("yyMMdd"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ format conversions
    public static BigDecimal mtAmountToDecimal(String mtAmount) {
        return new BigDecimal(mtAmount.replace(',', '.').replaceAll("\\.$", ""));
    }

    public static String decimalToMtAmount(BigDecimal amount) {
        String plain = amount.stripTrailingZeros().toPlainString();
        if (amount.signum() == 0) {
            plain = "0";
        }
        return plain.contains(".") ? plain.replace('.', ',') : plain + ",";
    }

    public static String decimalToMxAmount(BigDecimal amount, String ccy) {
        return amount.setScale(currencyDecimals(ccy), java.math.RoundingMode.UNNECESSARY).toPlainString();
    }

    /** "261017" -> 2026-10-17 */
    public static LocalDate mtDateToIso(String yyMMdd) {
        return LocalDate.parse(yyMMdd, DateTimeFormatter.ofPattern("yyMMdd"));
    }

    public static String isoToMtDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyMMdd"));
    }
}
