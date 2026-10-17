package com.payments.automation.utils;

import com.payments.automation.config.ConfigReader;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/** Explicit / fluent wait helpers built on {@link WebDriverWait}. */
public class WaitUtils {

    private final WebDriver driver;
    private final Duration timeout;

    public WaitUtils(WebDriver driver) {
        this(driver, Duration.ofSeconds(ConfigReader.getInt("explicit.wait", 20)));
    }

    public WaitUtils(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.timeout = timeout;
    }

    private WebDriverWait await() {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    public WebElement visible(By locator) {
        return await().until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public WebElement visible(WebElement element) {
        return await().until(ExpectedConditions.visibilityOf(element));
    }

    public List<WebElement> allVisible(By locator) {
        return await().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public WebElement present(By locator) {
        return await().until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public WebElement clickable(By locator) {
        return await().until(ExpectedConditions.elementToBeClickable(locator));
    }

    public WebElement clickable(WebElement element) {
        return await().until(ExpectedConditions.elementToBeClickable(element));
    }

    public boolean invisible(By locator) {
        return await().until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public boolean textPresent(By locator, String text) {
        return await().until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    public boolean attributeContains(By locator, String attribute, String value) {
        return await().until(ExpectedConditions.attributeContains(locator, attribute, value));
    }

    public boolean titleContains(String title) {
        return await().until(ExpectedConditions.titleContains(title));
    }

    public boolean urlContains(String fragment) {
        return await().until(ExpectedConditions.urlContains(fragment));
    }

    public Alert alert() {
        return await().until(ExpectedConditions.alertIsPresent());
    }

    public WebDriver frame(By locator) {
        return await().until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(locator));
    }

    public boolean numberOfWindows(int expected) {
        return await().until(ExpectedConditions.numberOfWindowsToBe(expected));
    }

    public void pageLoaded() {
        await().until(d -> "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState")));
    }

    /** Waits for jQuery AJAX calls to finish (no-op when the page does not use jQuery). */
    public void ajaxComplete() {
        await().until(d -> (Boolean) ((JavascriptExecutor) d)
                .executeScript("return (window.jQuery == null) || (jQuery.active === 0)"));
    }

    /** Fluent wait with custom polling - useful for slow back-office screens (e.g. payment status refresh). */
    public <T> T fluent(Function<WebDriver, T> condition, Duration maxWait, Duration polling) {
        return new FluentWait<>(driver)
                .withTimeout(maxWait)
                .pollingEvery(polling)
                .ignoring(NoSuchElementException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(condition);
    }
}
