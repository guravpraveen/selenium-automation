@db
Feature: Payment and liquidity data in the database
  Uses the "practice" application (H2 in-memory). Change the application name to
  "liquidity" (Oracle) or "cbpr" (SAP HANA) to run the same checks on a real database.

  Background:
    Given I am connected to the "practice" application database
    And the practice payments data is loaded

  @smoke
  Scenario: Settled CBPR+ payment is stored correctly
    When I fetch the payment with reference "TRN0001"
    Then the payment record should have:
      | MSG_TYPE     | pacs.008    |
      | SCHEME       | CBPR        |
      | SENDER_BIC   | PAYBGB2LXXX |
      | CCY          | USD         |
      | AMOUNT       | 25000.00    |
      | STATUS       | SETTLED     |

  Scenario: Pending RTGS payment moves to settled
    When the payment "TRN0002" status is updated to "SETTLED"
    Then the payment "TRN0002" should reach status "SETTLED" within 5 seconds

  Scenario: Scheme counts
    Then there should be 2 payments with scheme "CBPR"
    And there should be 1 payments with scheme "RTGS"

  @liquidity
  Scenario: Nostro stays above minimum threshold
    Then the nostro account "NOSTRO-USD-001" balance should stay above threshold after debit of 25000.00
