package com.payments.automation.reports;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.payments.automation.config.ConfigReader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Single shared ExtentReports instance writing a Spark HTML report to target/reports. */
public final class ExtentManager {

    private static ExtentReports extent;
    private static String reportPath;

    private ExtentManager() {
    }

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            reportPath = "target/reports/ExtentReport_" + stamp + ".html";
            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setTheme(Theme.STANDARD);
            spark.config().setDocumentTitle("Payments Test Automation");
            spark.config().setReportName("Payments Regression - " + ConfigReader.get("env", "qa").toUpperCase());
            spark.config().setTimeStampFormat("dd-MMM-yyyy HH:mm:ss");

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("Environment", ConfigReader.get("env", "qa"));
            extent.setSystemInfo("Browser", ConfigReader.get("browser", "chrome"));
            extent.setSystemInfo("Application", ConfigReader.get("app.name", "payments"));
            extent.setSystemInfo("Database", ConfigReader.get("db.type", "h2"));
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java", System.getProperty("java.version"));
            extent.setSystemInfo("User", System.getProperty("user.name"));
        }
        return extent;
    }

    public static synchronized void flush() {
        if (extent != null) {
            extent.flush();
        }
    }

    public static String getReportPath() {
        return reportPath;
    }
}
