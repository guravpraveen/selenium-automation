package com.payments.automation.base;

import com.payments.automation.config.ConfigReader;
import com.payments.automation.drivers.DriverManager;
import com.payments.automation.utils.ScreenshotUtils;
import com.payments.automation.utils.WaitUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.locators.RelativeLocator;
import org.openqa.selenium.support.ui.Select;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parent of every Page Object. Wraps the Selenium WebDriver API in reusable,
 * wait-safe methods so page classes only describe locators and business actions.
 *
 * <p>Method groups: navigation, element actions, reads, dropdowns, checkboxes,
 * mouse/keyboard (Actions), JavaScript, alerts, frames, windows/tabs, cookies,
 * tables, file upload, scrolling, Selenium 4 relative locators and screenshots.</p>
 */
public abstract class BasePage {

    protected final Logger log = LogManager.getLogger(getClass());
    protected final WebDriver driver;
    protected final WaitUtils wait;
    protected final Actions actions;

    protected BasePage() {
        this.driver = DriverManager.getDriver();
        this.wait = new WaitUtils(driver);
        this.actions = new Actions(driver);
        PageFactory.initElements(driver, this); // supports @FindBy if you prefer PageFactory style
    }

    // ------------------------------------------------------------------ navigation
    public void open(String url) {
        log.info("Navigate to {}", url);
        driver.get(url);
        wait.pageLoaded();
    }

    /** Opens a path relative to {@code base.url}. */
    public void openPath(String path) {
        open(ConfigReader.getRequired("base.url") + path);
    }

    public void refresh() {
        driver.navigate().refresh();
    }

    public void back() {
        driver.navigate().back();
    }

    public void forward() {
        driver.navigate().forward();
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageSource() {
        return driver.getPageSource();
    }

    // ------------------------------------------------------------------ element actions
    public WebElement find(By locator) {
        return wait.visible(locator);
    }

    public List<WebElement> findAll(By locator) {
        return driver.findElements(locator);
    }

    public void click(By locator) {
        log.debug("Click {}", locator);
        try {
            wait.clickable(locator).click();
        } catch (org.openqa.selenium.ElementClickInterceptedException e) {
            log.warn("Normal click intercepted on {}, falling back to JS click", locator);
            jsClick(locator);
        }
    }

    public void type(By locator, String text) {
        log.debug("Type '{}' into {}", text, locator);
        WebElement element = wait.visible(locator);
        element.clear();
        element.sendKeys(text);
    }

    /** Types without logging the value (passwords, PINs, tokens). */
    public void typeSecret(By locator, String secret) {
        log.debug("Type ****** into {}", locator);
        WebElement element = wait.visible(locator);
        element.clear();
        element.sendKeys(secret);
    }

    public void typeAndEnter(By locator, String text) {
        type(locator, text);
        driver.findElement(locator).sendKeys(Keys.ENTER);
    }

    /** Clears fields that ignore {@code clear()} (some React/Angular inputs). */
    public void clearWithKeys(By locator) {
        WebElement element = wait.visible(locator);
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
    }

    public void submit(By locator) {
        wait.visible(locator).submit();
    }

    // ------------------------------------------------------------------ reads / state
    public String getText(By locator) {
        return wait.visible(locator).getText().trim();
    }

    public List<String> getTexts(By locator) {
        List<String> texts = new ArrayList<>();
        for (WebElement e : wait.allVisible(locator)) {
            texts.add(e.getText().trim());
        }
        return texts;
    }

    public String getAttribute(By locator, String attribute) {
        return wait.present(locator).getDomAttribute(attribute);
    }

    public String getProperty(By locator, String property) {
        return wait.present(locator).getDomProperty(property);
    }

    public String getValue(By locator) {
        return getProperty(locator, "value");
    }

    public String getCssValue(By locator, String cssProperty) {
        return wait.visible(locator).getCssValue(cssProperty);
    }

    public boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public boolean isDisplayed(By locator, Duration timeout) {
        try {
            new WaitUtils(driver, timeout).visible(locator);
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean isEnabled(By locator) {
        return wait.present(locator).isEnabled();
    }

    public boolean isSelected(By locator) {
        return wait.present(locator).isSelected();
    }

    public int count(By locator) {
        return driver.findElements(locator).size();
    }

    public Point getLocation(By locator) {
        return wait.visible(locator).getLocation();
    }

    public Dimension getSize(By locator) {
        return wait.visible(locator).getSize();
    }

    // ------------------------------------------------------------------ dropdowns
    public void selectByVisibleText(By locator, String text) {
        new Select(wait.visible(locator)).selectByVisibleText(text);
    }

    public void selectByValue(By locator, String value) {
        new Select(wait.visible(locator)).selectByValue(value);
    }

    public void selectByIndex(By locator, int index) {
        new Select(wait.visible(locator)).selectByIndex(index);
    }

    public void selectByContainsText(By locator, String partialText) {
        new Select(wait.visible(locator)).selectByContainsVisibleText(partialText);
    }

    public void deselectAll(By locator) {
        new Select(wait.visible(locator)).deselectAll();
    }

    public String getSelectedOption(By locator) {
        return new Select(wait.visible(locator)).getFirstSelectedOption().getText().trim();
    }

    public List<String> getDropdownOptions(By locator) {
        List<String> options = new ArrayList<>();
        for (WebElement option : new Select(wait.visible(locator)).getOptions()) {
            options.add(option.getText().trim());
        }
        return options;
    }

    /** For custom (non-&lt;select&gt;) dropdowns: open it, then click the option with the given text. */
    public void selectCustomDropdown(By dropdown, By options, String text) {
        click(dropdown);
        for (WebElement option : wait.allVisible(options)) {
            if (option.getText().trim().equalsIgnoreCase(text)) {
                option.click();
                return;
            }
        }
        throw new NoSuchElementException("Option '" + text + "' not found in " + options);
    }

    // ------------------------------------------------------------------ checkbox / radio
    public void check(By locator) {
        if (!isSelected(locator)) {
            click(locator);
        }
    }

    public void uncheck(By locator) {
        if (isSelected(locator)) {
            click(locator);
        }
    }

    // ------------------------------------------------------------------ mouse & keyboard (Actions API)
    public void hover(By locator) {
        actions.moveToElement(wait.visible(locator)).perform();
    }

    public void doubleClick(By locator) {
        actions.doubleClick(wait.visible(locator)).perform();
    }

    public void rightClick(By locator) {
        actions.contextClick(wait.visible(locator)).perform();
    }

    public void dragAndDrop(By source, By target) {
        actions.dragAndDrop(wait.visible(source), wait.visible(target)).perform();
    }

    public void dragAndDropBy(By source, int xOffset, int yOffset) {
        actions.dragAndDropBy(wait.visible(source), xOffset, yOffset).perform();
    }

    public void clickAndHold(By locator, Duration hold) {
        actions.clickAndHold(wait.visible(locator)).pause(hold).release().perform();
    }

    public void pressKey(Keys key) {
        actions.sendKeys(key).perform();
    }

    public void pressKeys(CharSequence... keys) {
        actions.sendKeys(Keys.chord(keys)).perform();
    }

    public void scrollToElementWithActions(By locator) {
        actions.scrollToElement(driver.findElement(locator)).perform();
    }

    // ------------------------------------------------------------------ JavaScript
    public Object executeJs(String script, Object... args) {
        return ((JavascriptExecutor) driver).executeScript(script, args);
    }

    public void jsClick(By locator) {
        executeJs("arguments[0].click();", wait.present(locator));
    }

    public void jsType(By locator, String text) {
        executeJs("arguments[0].value=arguments[1];"
                + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));", wait.present(locator), text);
    }

    public void scrollIntoView(By locator) {
        executeJs("arguments[0].scrollIntoView({block:'center'});", wait.present(locator));
    }

    public void scrollToTop() {
        executeJs("window.scrollTo(0, 0);");
    }

    public void scrollToBottom() {
        executeJs("window.scrollTo(0, document.body.scrollHeight);");
    }

    public void scrollBy(int x, int y) {
        executeJs("window.scrollBy(arguments[0], arguments[1]);", x, y);
    }

    public void highlight(By locator) {
        executeJs("arguments[0].style.border='3px solid red';", wait.present(locator));
    }

    public void removeAttribute(By locator, String attribute) {
        executeJs("arguments[0].removeAttribute(arguments[1]);", wait.present(locator), attribute);
    }

    // ------------------------------------------------------------------ alerts
    public String getAlertText() {
        return wait.alert().getText();
    }

    public void acceptAlert() {
        wait.alert().accept();
    }

    public void dismissAlert() {
        wait.alert().dismiss();
    }

    public void typeInAlert(String text) {
        Alert alert = wait.alert();
        alert.sendKeys(text);
        alert.accept();
    }

    // ------------------------------------------------------------------ frames
    public void switchToFrame(By locator) {
        wait.frame(locator);
    }

    public void switchToFrame(int index) {
        driver.switchTo().frame(index);
    }

    public void switchToFrame(String nameOrId) {
        driver.switchTo().frame(nameOrId);
    }

    public void switchToParentFrame() {
        driver.switchTo().parentFrame();
    }

    public void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    // ------------------------------------------------------------------ windows & tabs
    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    public Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    /** Switches to the first window that is not the current one. */
    public String switchToNewWindow() {
        String current = driver.getWindowHandle();
        for (String handle : driver.getWindowHandles()) {
            if (!handle.equals(current)) {
                driver.switchTo().window(handle);
                return handle;
            }
        }
        throw new NoSuchElementException("No new window found");
    }

    public void switchToWindowByTitle(String title) {
        for (String handle : driver.getWindowHandles()) {
            driver.switchTo().window(handle);
            if (driver.getTitle().contains(title)) {
                return;
            }
        }
        throw new NoSuchElementException("No window with title containing: " + title);
    }

    public void switchToWindow(String handle) {
        driver.switchTo().window(handle);
    }

    public void openNewTab(String url) {
        driver.switchTo().newWindow(WindowType.TAB);
        driver.get(url);
    }

    public void openNewWindow(String url) {
        driver.switchTo().newWindow(WindowType.WINDOW);
        driver.get(url);
    }

    public void closeCurrentAndSwitchTo(String handle) {
        driver.close();
        driver.switchTo().window(handle);
    }

    public void setWindowSize(int width, int height) {
        driver.manage().window().setSize(new Dimension(width, height));
    }

    // ------------------------------------------------------------------ cookies
    public void addCookie(String name, String value) {
        driver.manage().addCookie(new Cookie(name, value));
    }

    public Cookie getCookie(String name) {
        return driver.manage().getCookieNamed(name);
    }

    public Set<Cookie> getAllCookies() {
        return driver.manage().getCookies();
    }

    public void deleteCookie(String name) {
        driver.manage().deleteCookieNamed(name);
    }

    public void deleteAllCookies() {
        driver.manage().deleteAllCookies();
    }

    // ------------------------------------------------------------------ web tables
    /** Reads an HTML table into a list of rows keyed by header text. */
    public List<Map<String, String>> readTable(By table) {
        WebElement tableElement = wait.visible(table);
        List<String> headers = new ArrayList<>();
        for (WebElement th : tableElement.findElements(By.cssSelector("thead th, tr:first-child th"))) {
            headers.add(th.getText().trim());
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (WebElement tr : tableElement.findElements(By.cssSelector("tbody tr"))) {
            List<WebElement> cells = tr.findElements(By.tagName("td"));
            if (cells.isEmpty()) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < cells.size(); i++) {
                String key = i < headers.size() ? headers.get(i) : "col" + i;
                row.put(key, cells.get(i).getText().trim());
            }
            rows.add(row);
        }
        return rows;
    }

    public String getCellText(By table, int row, int col) {
        return wait.visible(table)
                .findElement(By.xpath(".//tbody/tr[" + row + "]/td[" + col + "]"))
                .getText().trim();
    }

    // ------------------------------------------------------------------ files
    public void uploadFile(By fileInput, Path file) {
        driver.findElement(fileInput).sendKeys(file.toAbsolutePath().toString());
    }

    // ------------------------------------------------------------------ Selenium 4 relative locators
    public WebElement findBelow(By target, By anchor) {
        return driver.findElement(RelativeLocator.with(target).below(anchor));
    }

    public WebElement findAbove(By target, By anchor) {
        return driver.findElement(RelativeLocator.with(target).above(anchor));
    }

    public WebElement findToRightOf(By target, By anchor) {
        return driver.findElement(RelativeLocator.with(target).toRightOf(anchor));
    }

    public WebElement findToLeftOf(By target, By anchor) {
        return driver.findElement(RelativeLocator.with(target).toLeftOf(anchor));
    }

    public WebElement findNear(By target, By anchor) {
        return driver.findElement(RelativeLocator.with(target).near(anchor));
    }

    // ------------------------------------------------------------------ screenshots
    public Path takeScreenshot(String name) {
        return ScreenshotUtils.save(driver, name);
    }

    public Path takeElementScreenshot(By locator, String name) {
        return ScreenshotUtils.save(wait.visible(locator), name);
    }
}
