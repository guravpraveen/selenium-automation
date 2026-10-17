@swift @mx
Feature: ISO 20022 MX messages for CBPR+ and RTGS

  @smoke
  Scenario: Validate a sample CBPR+ pacs.008
    Given the MX message file "swift/mx/pacs008_valid.xml"
    When I validate the MX message against CBPR+ rules
    Then the message should be valid
    And the MX elements should be:
      | GrpHdr/SttlmInf/SttlmMtd          | INDA            |
      | CdtTrfTxInf/IntrBkSttlmAmt/@Ccy   | HKD             |
      | CdtTrfTxInf/IntrBkSttlmAmt        | 125000.50       |
      | CdtTrfTxInf/ChrgBr                | DEBT            |
      | AppHdr/MsgDefIdr                  | pacs.008.001.08 |

  Scenario: Generate an RTGS pacs.009 with clearing system
    Given a RTGS payment with details:
      | reference       | LQ20261020EU01 |
      | senderBic       | PAYBDEFFXXX    |
      | receiverBic     | CNTRDEFFXXX    |
      | currency        | EUR            |
      | amount          | 15000000.00    |
      | clearingSystem  | TGT            |
      | creditorAccount | DE00TARGET0001 |
    When I generate the ISO 20022 pacs.009 message
    And I validate the MX message against RTGS rules
    Then the message should be valid
    And the MX element "GrpHdr/SttlmInf/SttlmMtd" should be "CLRG"
    And the MX element "GrpHdr/SttlmInf/ClrSys/Cd" should be "TGT"

  @negative
  Scenario Outline: CBPR+ business rules reject bad data
    Given the MX message file "swift/mx/pacs008_valid.xml"
    When I change MX element "<path>" to "<value>"
    And I validate the MX message against CBPR+ rules
    Then the message should be rejected with error "<error>"

    Examples:
      | path                                  | value        | error |
      | CdtTrfTxInf/PmtId/UETR                | 1234         | TX01  |
      | CdtTrfTxInf/IntrBkSttlmAmt            | 10.999       | TX05  |
      | CdtTrfTxInf/ChrgBr                    | SHA          | TX07  |
      | CdtTrfTxInf/InstdAgt/FinInstnId/BICFI | XYZ          | AG01  |
      | GrpHdr/NbOfTxs                        | 2            | GH02  |
      | GrpHdr/SttlmInf/SttlmMtd              | CLRG         | GH03  |
