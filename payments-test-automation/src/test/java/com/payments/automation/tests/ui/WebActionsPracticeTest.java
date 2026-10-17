package com.payments.automation.tests.ui;

import com.payments.automation.base.BaseTest;
import com.payments.automation.pages.PracticePage;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.Keys;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/** Exercises the BasePage wrappers against the-internet.herokuapp.com - good daily Selenium practice. */
public class WebActionsPracticeTest extends BaseTest {

    @Test(groups = "ui")
    public void dropdownSelection() {
        PracticePage page = new PracticePage();
        page.selectOption("Option 2");
        Assertions.assertThat(page.selectedOption()).isEqualTo("Option 2");
        Assertions.assertThat(page.dropdownOptions()).contains("Option 1", "Option 2");
    }

    @Test(groups = "ui")
    public void checkboxes() {
        PracticePage page = new PracticePage();
        page.openCheckboxes();
        page.tickBoth();
        Assertions.assertThat(page.isChecked(1) && page.isChecked(2)).isTrue();
        page.untickBoth();
        Assertions.assertThat(page.isChecked(1) || page.isChecked(2)).isFalse();
    }

    @Test(groups = "ui")
    public void javascriptAlerts() {
        PracticePage page = new PracticePage();
        Assertions.assertThat(page.acceptJsAlert()).isEqualTo("You successfully clicked an alert");
        Assertions.assertThat(page.dismissJsConfirm()).isEqualTo("You clicked: Cancel");
        Assertions.assertThat(page.answerJsPrompt("MT103")).isEqualTo("You entered: MT103");
    }

    @Test(groups = "ui")
    public void nestedFrames() {
        PracticePage page = new PracticePage();
        Assertions.assertThat(page.readMiddleFrame()).isEqualTo("MIDDLE");
        Assertions.assertThat(page.readBottomFrame()).isEqualTo("BOTTOM");
    }

    @Test(groups = "ui")
    public void multipleWindows() {
        Assertions.assertThat(new PracticePage().openNewWindowAndReadHeading()).isEqualTo("New Window");
    }

    @Test(groups = "ui")
    public void mouseHover() {
        Assertions.assertThat(new PracticePage().hoverFirstAvatar()).isEqualTo("name: user1");
    }

    @Test(groups = "ui")
    public void webTable() {
        List<Map<String, String>> rows = new PracticePage().readExampleTable();
        Assertions.assertThat(rows).hasSize(4);
        Assertions.assertThat(rows.get(0)).containsEntry("Last Name", "Smith").containsEntry("First Name", "John");
    }

    @Test(groups = "ui")
    public void keyboardKeys() {
        Assertions.assertThat(new PracticePage().pressKeyInInput(Keys.TAB)).isEqualTo("You entered: TAB");
    }
}
