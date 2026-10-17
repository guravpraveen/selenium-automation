package com.payments.automation.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotUtils {

    private static final Path DIR = Paths.get("target", "reports", "screenshots");

    private ScreenshotUtils() {
    }

    public static String asBase64(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
    }

    public static byte[] asBytes(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    /** Saves a full-page screenshot and returns the file path. */
    public static Path save(WebDriver driver, String name) {
        return write(asBytes(driver), name);
    }

    /** Saves a screenshot of one element only. */
    public static Path save(WebElement element, String name) {
        return write(element.getScreenshotAs(OutputType.BYTES), name);
    }

    private static Path write(byte[] bytes, String name) {
        try {
            Files.createDirectories(DIR);
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            Path file = DIR.resolve(name.replaceAll("[^a-zA-Z0-9-_]", "_") + "_" + stamp + ".png");
            Files.write(file, bytes);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
