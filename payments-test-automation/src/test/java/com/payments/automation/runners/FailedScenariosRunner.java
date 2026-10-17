package com.payments.automation.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/** Re-runs only the scenarios that failed in the last run (written by the rerun plugin). */
@CucumberOptions(
        features = "@target/reports/cucumber/rerun.txt",
        glue = {"com.payments.automation.stepdefs", "com.payments.automation.hooks"},
        plugin = {"pretty", "html:target/reports/cucumber/rerun.html"},
        monochrome = true)
public class FailedScenariosRunner extends AbstractTestNGCucumberTests {
}
