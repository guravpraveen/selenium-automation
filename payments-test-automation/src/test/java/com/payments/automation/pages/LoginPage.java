package com.payments.automation.pages;

import com.payments.automation.base.BasePage;
import org.openqa.selenium.By;

/** Practice login page: https://the-internet.herokuapp.com/login (tomsmith / SuperSecretPassword!). */
public class LoginPage extends BasePage {

    private final By username = By.id("username");
    private final By password = By.id("password");
    private final By loginButton = By.cssSelector("button[type='submit']");
    private final By flashMessage = By.id("flash");

    public LoginPage openPage() {
        openPath("/login");
        return this;
    }

    public LoginPage enterUsername(String user) {
        type(username, user);
        return this;
    }

    public LoginPage enterPassword(String pwd) {
        typeSecret(password, pwd);
        return this;
    }

    public void clickLogin() {
        click(loginButton);
    }

    public SecureAreaPage loginAs(String user, String pwd) {
        enterUsername(user).enterPassword(pwd).clickLogin();
        return new SecureAreaPage();
    }

    public String getFlashMessage() {
        // flash text ends with a close "x" character
        return getText(flashMessage).replace("×", "").trim();
    }

    public boolean isLoginButtonDisplayed() {
        return isDisplayed(loginButton);
    }
}
