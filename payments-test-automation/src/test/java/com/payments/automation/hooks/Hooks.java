package com.payments.automation.hooks;

import com.aventstack.extentreports.ExtentTest;
import com.payments.automation.config.ConfigReader;
import com.payments.automation.db.DatabaseFactory;
import com.payments.automation.drivers.DriverManager;
import com.payments.automation.reports.ExtentManager;
import com.payments.automation.reports.ExtentTestManager;
import com.payments.automation.utils.ScreenshotUtils;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

/**
 * Cucumber lifecycle:
 * <ul>
 *   <li>every scenario becomes a node in the Extent report (tags = categories)</li>
 *   <li>{@code @ui} scenarios get a browser; screenshot attached on failure</li>
 *   <li>DB connections are closed after the run</li>
 * </ul>
 */
public class Hooks {

    @Before(order = 0)
    public void startReport(Scenario scenario) {
        ExtentTest test = ExtentTestManager.startTest(scenario.getName(), scenario.getUri().toString());
        scenario.getSourceTagNames().forEach(tag -> test.assignCategory(tag.substring(1)));
    }

    @Before(value = "@ui", order = 1)
    public void startBrowser() {
        DriverManager.initDriver(ConfigReader.get("browser", "chrome"));
    }

    @AfterStep("@ui")
    public void screenshotOnFailedStep(Scenario scenario) {
        if (scenario.isFailed() && DriverManager.hasDriver()) {
            byte[] png = ScreenshotUtils.asBytes(DriverManager.getDriver());
            scenario.attach(png, "image/png", "failed-step");
            ExtentTestManager.screenshot(ScreenshotUtils.asBase64(DriverManager.getDriver()), "Failed step");
        }
    }

    @After(order = 1)
    public void finishReport(Scenario scenario) {
        switch (scenario.getStatus()) {
            case PASSED -> ExtentTestManager.pass("Scenario passed");
            case SKIPPED, PENDING, UNDEFINED -> ExtentTestManager.warning("Scenario " + scenario.getStatus());
            default -> ExtentTestManager.fail("Scenario " + scenario.getStatus());
        }
        ExtentTestManager.removeTest();
    }

    @After(value = "@ui", order = 0)
    public void quitBrowser() {
        DriverManager.quitDriver();
    }

    @AfterAll
    public static void afterAll() {
        DatabaseFactory.closeAll();
        ExtentManager.flush();
    }
}
