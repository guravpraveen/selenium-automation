package com.payments.automation.drivers;

import com.payments.automation.config.ConfigReader;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates WebDriver instances. Selenium 4.6+ ships Selenium Manager, so no
 * WebDriverManager / manual driver binaries are required for local browsers.
 *
 * <p>Set {@code grid.url} to run on Selenium Grid / cloud, and {@code appium.url}
 * for mobile browsers.</p>
 */
public final class DriverFactory {

    private static final Logger LOG = LogManager.getLogger(DriverFactory.class);

    private DriverFactory() {
    }

    public static WebDriver createDriver(BrowserType type) {
        boolean headless = ConfigReader.getBoolean("headless", false);
        String gridUrl = ConfigReader.get("grid.url", "");
        LOG.info("Starting {} (headless={}, grid={})", type, headless, gridUrl.isEmpty() ? "local" : gridUrl);

        WebDriver driver = switch (type) {
            case CHROME -> start(chromeOptions(headless), gridUrl, ChromeDriver::new);
            case FIREFOX -> start(firefoxOptions(headless), gridUrl, FirefoxDriver::new);
            case EDGE -> start(edgeOptions(headless), gridUrl, EdgeDriver::new);
            case SAFARI -> new SafariDriver();
            case CHROME_MOBILE_EMULATION -> start(mobileEmulationOptions(headless), gridUrl, ChromeDriver::new);
            case ANDROID_CHROME -> new AndroidDriver(appiumUrl(), androidChromeOptions());
            case IOS_SAFARI -> new IOSDriver(appiumUrl(), iosSafariOptions());
        };

        configureTimeouts(driver);
        if (!type.isAppium() && type != BrowserType.CHROME_MOBILE_EMULATION
                && ConfigReader.getBoolean("browser.maximize", true)) {
            driver.manage().window().maximize();
        }
        return driver;
    }

    @FunctionalInterface
    private interface LocalDriver<O extends Capabilities> {
        WebDriver create(O options);
    }

    private static <O extends Capabilities> WebDriver start(O options, String gridUrl, LocalDriver<O> local) {
        if (gridUrl.isEmpty()) {
            return local.create(options);
        }
        return new RemoteWebDriver(toUrl(gridUrl), options);
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
        options.addArguments("--disable-notifications", "--remote-allow-origins=*", "--no-sandbox",
                "--disable-dev-shm-usage", "--window-size=1920,1080");
        if (headless) {
            options.addArguments("--headless=new");
        }
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("download.default_directory", System.getProperty("user.dir") + "/target/downloads");
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);
        String binary = ConfigReader.get("chrome.binary", "");
        if (!binary.isEmpty()) {
            options.setBinary(binary);
        }
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless");
        }
        options.addPreference("dom.webnotifications.enabled", false);
        options.addPreference("browser.download.dir", System.getProperty("user.dir") + "/target/downloads");
        options.addPreference("browser.download.folderList", 2);
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--disable-notifications", "--window-size=1920,1080");
        if (headless) {
            options.addArguments("--headless=new");
        }
        return options;
    }

    private static ChromeOptions mobileEmulationOptions(boolean headless) {
        ChromeOptions options = chromeOptions(headless);
        Map<String, Object> mobileEmulation = new HashMap<>();
        mobileEmulation.put("deviceName", ConfigReader.get("mobile.emulation.device", "Pixel 7"));
        options.setExperimentalOption("mobileEmulation", mobileEmulation);
        return options;
    }

    private static UiAutomator2Options androidChromeOptions() {
        return new UiAutomator2Options()
                .setPlatformName("Android")
                .setDeviceName(ConfigReader.get("appium.android.deviceName", "Android Emulator"))
                .setPlatformVersion(ConfigReader.get("appium.android.platformVersion", "14"))
                .withBrowserName("Chrome")
                .setNewCommandTimeout(Duration.ofSeconds(120));
    }

    private static XCUITestOptions iosSafariOptions() {
        return new XCUITestOptions()
                .setPlatformName("iOS")
                .setDeviceName(ConfigReader.get("appium.ios.deviceName", "iPhone 15"))
                .setPlatformVersion(ConfigReader.get("appium.ios.platformVersion", "17.5"))
                .withBrowserName("Safari")
                .setNewCommandTimeout(Duration.ofSeconds(120));
    }

    private static URL appiumUrl() {
        return toUrl(ConfigReader.get("appium.url", "http://127.0.0.1:4723"));
    }

    private static URL toUrl(String url) {
        try {
            return URI.create(url).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    private static void configureTimeouts(WebDriver driver) {
        // Implicit wait is kept at 0 on purpose: the framework uses explicit waits everywhere.
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(ConfigReader.getInt("implicit.wait", 0)));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigReader.getInt("page.load.timeout", 60)));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(ConfigReader.getInt("script.timeout", 30)));
    }
}
