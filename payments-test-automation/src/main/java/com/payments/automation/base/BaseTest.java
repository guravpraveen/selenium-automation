package com.payments.automation.base;

import com.payments.automation.config.ConfigReader;
import com.payments.automation.drivers.DriverManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

/**
 * Parent for TestNG UI tests. Browser comes from testng.xml parameter,
 * then -Dbrowser, then config.properties - so cross-browser runs are a suite change only.
 */
public abstract class BaseTest {

    @Parameters("browser")
    @BeforeMethod(alwaysRun = true)
    public void setUp(@Optional String browser) {
        String target = System.getProperty("browser") != null ? System.getProperty("browser")
                : browser != null ? browser : ConfigReader.get("browser", "chrome");
        DriverManager.initDriver(target);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverManager.quitDriver();
    }
}
