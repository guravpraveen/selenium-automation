package com.payments.automation.drivers;

import org.openqa.selenium.WebDriver;

/** Holds one WebDriver per thread so tests can run in parallel safely. */
public final class DriverManager {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver not initialised for this thread. Call DriverManager.initDriver() first.");
        }
        return driver;
    }

    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    public static void initDriver(String browser) {
        if (DRIVER.get() == null) {
            DRIVER.set(DriverFactory.createDriver(BrowserType.from(browser)));
        }
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
            }
        }
    }
}
