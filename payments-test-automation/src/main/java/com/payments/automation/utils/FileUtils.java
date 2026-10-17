package com.payments.automation.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

public final class FileUtils {

    private FileUtils() {
    }

    /** Reads a text resource from src/test/resources (or any classpath location). */
    public static String readResource(String classpathResource) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + classpathResource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Path write(Path file, String content) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            return Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Polls a folder until a file with the given suffix appears (e.g. a downloaded statement). */
    public static Path waitForDownload(Path folder, String suffix, Duration timeout) {
        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try (var files = Files.list(folder)) {
                var match = files.filter(p -> p.toString().endsWith(suffix)).findFirst();
                if (match.isPresent()) {
                    return match.get();
                }
            } catch (IOException ignored) {
                // folder may not exist yet
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new IllegalStateException("No *" + suffix + " file in " + folder + " after " + timeout);
    }
}
