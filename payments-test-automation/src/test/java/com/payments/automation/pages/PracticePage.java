package com.payments.automation.pages;

import com.payments.automation.base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;

import java.util.List;
import java.util.Map;

/**
 * One page object per "the-internet" exercise, to practise every BasePage method:
 * dropdowns, checkboxes, JS alerts, nested frames, windows, hovers, tables and keyboard.
 */
public class PracticePage extends BasePage {

    // dropdown
    private final By dropdown = By.id("dropdown");
    // checkboxes
    private final By checkbox1 = By.cssSelector("#checkboxes input:nth-of-type(1)");
    private final By checkbox2 = By.cssSelector("#checkboxes input:nth-of-type(2)");
    // alerts
    private final By jsAlert = By.xpath("//button[text()='Click for JS Alert']");
    private final By jsConfirm = By.xpath("//button[text()='Click for JS Confirm']");
    private final By jsPrompt = By.xpath("//button[text()='Click for JS Prompt']");
    private final By alertResult = By.id("result");
    // frames
    private final By body = By.tagName("body");
    private final By middleContent = By.id("content");
    // windows
    private final By clickHere = By.linkText("Click Here");
    private final By h3 = By.tagName("h3");
    // hovers
    private final By firstAvatar = By.cssSelector(".figure:nth-of-type(1) img");
    private final By firstCaption = By.cssSelector(".figure:nth-of-type(1) .figcaption h5");
    // table
    private final By table1 = By.id("table1");
    // key presses
    private final By keyTarget = By.id("target");
    private final By keyResult = By.id("result");

    // ---------- dropdown
    public void selectOption(String option) {
        openPath("/dropdown");
        selectByVisibleText(dropdown, option);
    }

    public String selectedOption() {
        return getSelectedOption(dropdown);
    }

    public List<String> dropdownOptions() {
        return getDropdownOptions(dropdown);
    }

    // ---------- checkboxes
    public void openCheckboxes() {
        openPath("/checkboxes");
    }

    public void tickBoth() {
        check(checkbox1);
        check(checkbox2);
    }

    public void untickBoth() {
        uncheck(checkbox1);
        uncheck(checkbox2);
    }

    public boolean isChecked(int index) {
        return isSelected(index == 1 ? checkbox1 : checkbox2);
    }

    // ---------- alerts
    public String acceptJsAlert() {
        openPath("/javascript_alerts");
        click(jsAlert);
        acceptAlert();
        return getText(alertResult);
    }

    public String dismissJsConfirm() {
        openPath("/javascript_alerts");
        click(jsConfirm);
        dismissAlert();
        return getText(alertResult);
    }

    public String answerJsPrompt(String answer) {
        openPath("/javascript_alerts");
        click(jsPrompt);
        typeInAlert(answer);
        return getText(alertResult);
    }

    // ---------- nested frames
    public String readMiddleFrame() {
        openPath("/nested_frames");
        switchToFrame("frame-top");
        switchToFrame("frame-middle");
        String text = getText(middleContent);
        switchToDefaultContent();
        return text;
    }

    public String readBottomFrame() {
        openPath("/nested_frames");
        switchToFrame("frame-bottom");
        String text = getText(body);
        switchToDefaultContent();
        return text;
    }

    // ---------- windows
    public String openNewWindowAndReadHeading() {
        openPath("/windows");
        String parent = getWindowHandle();
        click(clickHere);
        wait.numberOfWindows(2);
        switchToNewWindow();
        String heading = getText(h3);
        closeCurrentAndSwitchTo(parent);
        return heading;
    }

    // ---------- hovers
    public String hoverFirstAvatar() {
        openPath("/hovers");
        hover(firstAvatar);
        return getText(firstCaption);
    }

    // ---------- tables
    public List<Map<String, String>> readExampleTable() {
        openPath("/tables");
        return readTable(table1);
    }

    // ---------- keyboard
    public String pressKeyInInput(Keys key) {
        openPath("/key_presses");
        click(keyTarget);
        find(keyTarget).sendKeys(key);
        return getText(keyResult);
    }
}
