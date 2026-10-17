package com.payments.automation.pages;

import com.payments.automation.base.BasePage;
import org.openqa.selenium.By;

public class SecureAreaPage extends BasePage {

    private final By header = By.cssSelector("div.example h2");
    private final By flashMessage = By.id("flash");
    private final By logoutButton = By.cssSelector("a[href='/logout']");

    public String getHeader() {
        return getText(header);
    }

    public String getFlashMessage() {
        return getText(flashMessage).replace("×", "").trim();
    }

    public boolean isLogoutDisplayed() {
        return isDisplayed(logoutButton);
    }

    public LoginPage logout() {
        click(logoutButton);
        return new LoginPage();
    }
}
