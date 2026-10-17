package com.payments.automation.stepdefs;

import com.payments.automation.pages.LoginPage;
import com.payments.automation.pages.SecureAreaPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;

public class LoginSteps {

    private LoginPage loginPage;

    @Given("the user is on the login page")
    public void openLoginPage() {
        loginPage = new LoginPage().openPage();
    }

    @When("the user logs in with username {string} and password {string}")
    public void login(String username, String password) {
        loginPage.enterUsername(username).enterPassword(password).clickLogin();
    }

    @Then("the secure area page is displayed")
    public void secureAreaDisplayed() {
        Assertions.assertThat(new SecureAreaPage().getHeader()).isEqualTo("Secure Area");
    }

    @Then("the message {string} is shown")
    public void messageShown(String message) {
        Assertions.assertThat(loginPage.getFlashMessage()).contains(message);
    }
}
