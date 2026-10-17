@swift @migration
Feature: MT to MX migration (coexistence translation)

  @smoke
  Scenario: MT103 translates to pacs.008 with all key fields mapped
    Given the MT message file "swift/mt/mt103_valid.txt"
    When I translate the MT message to MX
    And I validate the MX message against CBPR+ rules
    Then the message should be valid
    And the MX UETR should equal the MT block 3 UETR
    And the MX elements should be:
      | CdtTrfTxInf/PmtId/InstrId       | TRN0001                |
      | CdtTrfTxInf/IntrBkSttlmDt       | 2026-10-17             |
      | CdtTrfTxInf/IntrBkSttlmAmt      | 25000.00               |
      | CdtTrfTxInf/ChrgBr              | SHAR                   |
      | CdtTrfTxInf/Dbtr/Nm             | ACME TRADING LTD       |
      | CdtTrfTxInf/DbtrAcct/Id/IBAN    | GB29NWBK60161331926819 |
      | CdtTrfTxInf/Cdtr/Nm             | LION EXPORTS PTE LTD   |

  Scenario: MT202 translates to pacs.009
    Given the MT message file "swift/mt/mt202_valid.txt"
    When I translate the MT message to MX
    Then the MX element "AppHdr/MsgDefIdr" should be "pacs.009.001.08"
    And the MX element "CdtTrfTxInf/IntrBkSttlmAmt/@Ccy" should be "INR"
    And the MX element "CdtTrfTxInf/CdtrAcct/Id/Othr/Id" should be "IN00RTGS0001"

  Scenario: pacs.008 translates back to a valid MT103
    Given the MX message file "swift/mx/pacs008_valid.xml"
    When I translate the MX message to MT
    And I validate the MT message
    Then the message should be valid
    And the MT field "32A" should be "261018HKD125000,5"
    And the MT field "71A" should be "OUR"
