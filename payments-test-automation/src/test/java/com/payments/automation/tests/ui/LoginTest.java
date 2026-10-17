package com.payments.automation.tests.ui;

import com.payments.automation.base.BaseTest;
import com.payments.automation.data.DataProviders;
import com.payments.automation.pages.LoginPage;
import com.payments.automation.pages.SecureAreaPage;
import com.payments.automation.reports.ExtentTestManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/** Excel data-driven login tests - rows come from the "Login" sheet. */
public class LoginTest extends BaseTest {

    @Test(groups = {"ui", "smoke"}, description = "Valid user lands on Secure Area and can log out")
    public void validLoginAndLogout() {
        SecureAreaPage secure = new LoginPage().openPage().loginAs("tomsmith", "SuperSecretPassword!");
        Assert.assertEquals(secure.getHeader(), "Secure Area");
        Assert.assertTrue(secure.isLogoutDisplayed(), "Logout button should be visible");

        LoginPage login = secure.logout();
        Assert.assertTrue(login.getFlashMessage().contains("You logged out of the secure area!"));
    }

    @Test(groups = {"ui", "regression"}, dataProvider = "loginData", dataProviderClass = DataProviders.class,
            description = "Login scenarios from Excel")
    public void loginFromExcel(Map<String, String> row) {
        ExtentTestManager.info("Test case: " + row.get("TestCaseId"));
        LoginPage login = new LoginPage().openPage();
        login.enterUsername(row.get("Username")).enterPassword(row.get("Password")).clickLogin();

        if ("SUCCESS".equals(row.get("ExpectedResult"))) {
            Assert.assertTrue(new SecureAreaPage().getFlashMessage().contains(row.get("ExpectedMessage")));
        } else {
            Assert.assertTrue(login.getFlashMessage().contains(row.get("ExpectedMessage")),
                    "Unexpected message: " + login.getFlashMessage());
        }
    }
}
