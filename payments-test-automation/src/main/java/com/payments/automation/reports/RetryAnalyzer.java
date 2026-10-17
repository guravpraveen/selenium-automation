package com.payments.automation.reports;

import com.payments.automation.config.ConfigReader;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/** Re-runs a failed test up to {@code retry.count} times (default 0 = off). */
public class RetryAnalyzer implements IRetryAnalyzer {

    private int attempt = 0;

    @Override
    public boolean retry(ITestResult result) {
        int max = ConfigReader.getInt("retry.count", 0);
        if (attempt < max) {
            attempt++;
            return true;
        }
        return false;
    }
}
