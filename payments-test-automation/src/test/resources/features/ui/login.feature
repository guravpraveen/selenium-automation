@ui
Feature: Login to the practice web application
  As a payments operations user
  I want to log in securely
  So that I can access the secure area

  Background:
    Given the user is on the login page

  @smoke
  Scenario: Successful login with valid credentials
    When the user logs in with username "tomsmith" and password "SuperSecretPassword!"
    Then the secure area page is displayed

  @regression
  Scenario Outline: Login is rejected for invalid credentials
    When the user logs in with username "<username>" and password "<password>"
    Then the message "<message>" is shown

    Examples:
      | username    | password             | message                   |
      | tomsmith    | wrong                | Your password is invalid! |
      | unknownUser | SuperSecretPassword! | Your username is invalid! |
