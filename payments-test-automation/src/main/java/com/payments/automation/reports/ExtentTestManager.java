package com.payments.automation.reports;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.markuputils.CodeLanguage;
import com.aventstack.extentreports.markuputils.MarkupHelper;

/** Thread-safe access to the current ExtentTest node + logging shortcuts. */
public final class ExtentTestManager {

    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();

    private ExtentTestManager() {
    }

    public static ExtentTest startTest(String name, String description) {
        ExtentTest test = ExtentManager.getInstance().createTest(name, description);
        TEST.set(test);
        return test;
    }

    public static ExtentTest getTest() {
        return TEST.get();
    }

    public static void removeTest() {
        TEST.remove();
    }

    public static void info(String message) {
        if (TEST.get() != null) {
            TEST.get().info(message);
        }
    }

    public static void pass(String message) {
        if (TEST.get() != null) {
            TEST.get().pass(message);
        }
    }

    public static void fail(String message) {
        if (TEST.get() != null) {
            TEST.get().fail(message);
        }
    }

    public static void warning(String message) {
        if (TEST.get() != null) {
            TEST.get().warning(message);
        }
    }

    public static void json(String title, String json) {
        if (TEST.get() != null) {
            TEST.get().info(title);
            TEST.get().info(MarkupHelper.createCodeBlock(json, CodeLanguage.JSON));
        }
    }

    public static void xml(String title, String xml) {
        if (TEST.get() != null) {
            TEST.get().info(title);
            TEST.get().info(MarkupHelper.createCodeBlock(xml, CodeLanguage.XML));
        }
    }

    public static void code(String title, String text) {
        if (TEST.get() != null) {
            TEST.get().info(title);
            TEST.get().info(MarkupHelper.createCodeBlock(text));
        }
    }

    public static void screenshot(String base64, String title) {
        if (TEST.get() != null) {
            TEST.get().info(title, MediaEntityBuilder.createScreenCaptureFromBase64String(base64, title).build());
        }
    }
}
