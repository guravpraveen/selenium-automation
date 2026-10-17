@swift @mt
Feature: SWIFT MT message generation and validation

  @smoke
  Scenario: Parse and validate a sample MT103
    Given the MT message file "swift/mt/mt103_valid.txt"
    When I validate the MT message
    Then the message should be valid
    And the MT message type should be "103"
    And the MT field "32A" should be "261017USD25000,"

  Scenario: Generate an MT103 for a CBPR+ customer payment
    Given a CBPR+ payment with details:
      | reference       | TRN103GEN01            |
      | senderBic       | PAYBGB2LXXX            |
      | receiverBic     | PAYBSGSGXXX            |
      | currency        | USD                    |
      | amount          | 1250.50                |
      | valueDate       | 2026-10-20             |
      | debtorName      | ACME TRADING LTD       |
      | debtorAccount   | GB29NWBK60161331926819 |
      | debtorAddress   | 1 CANADA SQUARE\|LONDON GB |
      | creditorName    | LION EXPORTS PTE LTD   |
      | creditorAccount | SG1234567890           |
      | remittance      | INVOICE 2026-0101      |
      | charges         | OUR                    |
    When I generate the MT103 message
    And I validate the MT message
    Then the message should be valid
    And the MT field "32A" should be "261020USD1250,5"
    And the MT field "71A" should be "OUR"

  Scenario: Generate an MT202 for RTGS liquidity funding
    Given a RTGS payment with details:
      | reference        | LQ20261020IN01 |
      | relatedReference | NONREF         |
      | senderBic        | PAYBINBBXXX    |
      | receiverBic      | CNTRINBBXXX    |
      | currency         | INR            |
      | amount           | 50000000.00    |
      | creditorAccount  | IN00RTGS0001   |
    When I generate the MT202 message
    And I validate the MT message
    Then the message should be valid

  @negative
  Scenario: Invalid MT103 is rejected with SWIFT error codes
    Given the MT message file "swift/mt/mt103_invalid.txt"
    When I validate the MT message
    Then the message should be rejected with error "T26"
    And the message should be rejected with error "T36"
    And the message should be rejected with error "E13"
    And the message should be rejected with error "G01"
