package com.payments.automation.utils;

import net.datafaker.Faker;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Random but realistic test data (names, addresses) plus payments-specific references. */
public final class TestDataGenerator {

    private static final Faker FAKER = new Faker(Locale.ENGLISH);

    private TestDataGenerator() {
    }

    public static String firstName() {
        return FAKER.name().firstName();
    }

    public static String lastName() {
        return FAKER.name().lastName();
    }

    public static String fullName() {
        return FAKER.name().fullName();
    }

    public static String companyName() {
        return FAKER.company().name();
    }

    public static String email() {
        return FAKER.internet().emailAddress();
    }

    public static String phone() {
        return FAKER.phoneNumber().cellPhone();
    }

    public static String streetAddress() {
        return FAKER.address().streetAddress();
    }

    public static String city() {
        return FAKER.address().city();
    }

    /** Alphanumeric, 16 chars max - valid for MT field :20: (sender's reference). */
    public static String transactionReference() {
        return "TRN" + System.currentTimeMillis() % 1_000_000_000_000L;
    }

    /** UETR - mandatory UUID v4 for gpi / CBPR+ (MT block 3 tag 121, MX UETR element). */
    public static String uetr() {
        return UUID.randomUUID().toString();
    }

    /** Amount with the given number of decimals (ISO 4217: USD=2, JPY=0, KWD=3). */
    public static BigDecimal amount(double min, double max, int decimals) {
        double v = ThreadLocalRandom.current().nextDouble(min, max);
        return BigDecimal.valueOf(v).setScale(decimals, RoundingMode.HALF_UP);
    }

    public static String valueDateYYMMDD(int plusDays) {
        return LocalDate.now().plusDays(plusDays).format(DateTimeFormatter.ofPattern("yyMMdd"));
    }

    public static String isoDate(int plusDays) {
        return LocalDate.now().plusDays(plusDays).toString();
    }

    public static String numeric(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }
}
