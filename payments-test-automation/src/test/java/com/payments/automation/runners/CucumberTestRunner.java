package com.payments.automation.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;

/**
 * Runs Gherkin features through TestNG.
 * Pick scenarios with tags:  mvn test -Dsuite=cucumber -Dcucumber.filter.tags="@swift and not @wip"
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.payments.automation.stepdefs", "com.payments.automation.hooks"},
        tags = "not @wip",
        plugin = {
                "pretty",
                "html:target/reports/cucumber/cucumber.html",
                "json:target/reports/cucumber/cucumber.json",
                "junit:target/reports/cucumber/cucumber.xml",
                "rerun:target/reports/cucumber/rerun.txt"
        },
        monochrome = true)
public class CucumberTestRunner extends AbstractTestNGCucumberTests {

    /** parallel = true runs scenarios in parallel (thread count from testng.xml data-provider-thread-count). */
    @Override
    @DataProvider(parallel = false)
    public Object[][] scenarios(ITestContext context) {
        return super.scenarios(context);
    }
}
