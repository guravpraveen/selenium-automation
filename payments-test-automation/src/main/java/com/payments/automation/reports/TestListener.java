package com.payments.automation.reports;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.payments.automation.drivers.DriverManager;
import com.payments.automation.utils.ScreenshotUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;

/**
 * TestNG listener that feeds ExtentReports: one node per test method,
 * screenshot on failure for UI tests, flush at end of suite.
 * Register it in testng.xml (&lt;listeners&gt;) - TestNG's own HTML report is still produced too.
 */
public class TestListener implements ITestListener, ISuiteListener {

    private static final Logger LOG = LogManager.getLogger(TestListener.class);

    @Override
    public void onStart(ISuite suite) {
        ExtentManager.getInstance();
    }

    @Override
    public void onFinish(ISuite suite) {
        ExtentManager.flush();
        LOG.info("Extent report: {}", ExtentManager.getReportPath());
    }

    @Override
    public void onTestStart(ITestResult result) {
        String name = result.getMethod().getMethodName();
        if (result.getParameters().length > 0) {
            name += " " + Arrays.toString(result.getParameters());
        }
        ExtentTest test = ExtentTestManager.startTest(name, result.getMethod().getDescription());
        test.assignCategory(result.getMethod().getGroups());
        test.assignCategory(result.getTestClass().getRealClass().getSimpleName());
        LOG.info("START {}", name);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTestManager.pass("Test passed");
        LOG.info("PASS  {}", result.getMethod().getMethodName());
        ExtentTestManager.removeTest();
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            test.fail(result.getThrowable());
            if (DriverManager.hasDriver()) {
                try {
                    String base64 = ScreenshotUtils.asBase64(DriverManager.getDriver());
                    test.fail("Screenshot on failure",
                            MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
                } catch (Exception e) {
                    LOG.warn("Could not capture screenshot: {}", e.getMessage());
                }
            }
        }
        LOG.error("FAIL  {}", result.getMethod().getMethodName(), result.getThrowable());
        ExtentTestManager.removeTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentTestManager.getTest();
        if (test != null) {
            test.skip(result.getThrowable() != null ? result.getThrowable().getMessage() : "Skipped");
        }
        ExtentTestManager.removeTest();
    }
}
