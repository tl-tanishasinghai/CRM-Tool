package com.trillionloans.los.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.los.model.dto.AssociationsDTO;
import com.trillionloans.los.model.dto.LoanApplicationTermsDTO;
import com.trillionloans.los.model.partner.m2p.M2PLoanApplicationRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pLoanApplicationTermsDTO;
import com.trillionloans.los.model.partner.m2p.M2pNachMandateRequestDTO;
import com.trillionloans.los.model.request.LoanApplication;
import com.trillionloans.los.model.request.NachMandateRequest;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

class LoanDataUtilTest {
  /** Method under test: {@link LoanDataUtil#getM2pNachMandate(NachMandateRequest)} */
  @Test
  void testGetM2pNachMandate() {
    M2pNachMandateRequestDTO actualM2pNachMandate =
        LoanDataUtil.getM2pNachMandate(new NachMandateRequest());

    assertEquals("dd-MM-yyyy", actualM2pNachMandate.getDateFormat());
    assertNull(actualM2pNachMandate.getPeriodUntilCancelled());
    assertNull(actualM2pNachMandate.getAmount());
    assertNull(actualM2pNachMandate.getBankAccountHolderName());
    assertNull(actualM2pNachMandate.getBankAccountNumber());
    assertNull(actualM2pNachMandate.getBankAccountType());
    assertNull(actualM2pNachMandate.getBankName());
    assertNull(actualM2pNachMandate.getBranchName());
    assertNull(actualM2pNachMandate.getDebitFrequencyEnum());
    assertNull(actualM2pNachMandate.getDebitTypeEnum());
    assertNull(actualM2pNachMandate.getExternalReferenceNumber());
    assertNull(actualM2pNachMandate.getIfsc());
    assertNull(actualM2pNachMandate.getMandateRegistrationRequestedDate());
    assertNull(actualM2pNachMandate.getMicr());
    assertNull(actualM2pNachMandate.getMode());
    assertNull(actualM2pNachMandate.getPeriodEndDate());
    assertNull(actualM2pNachMandate.getPeriodStartDate());
    assertNull(actualM2pNachMandate.getStatus());
    assertNull(actualM2pNachMandate.getUmrn());
  }

  /** Method under test: {@link LoanDataUtil#getM2pNachMandate(NachMandateRequest)} */
  @Test
  void testGetM2pNachMandate2() {
    NachMandateRequest nachMandateRequest = mock(NachMandateRequest.class);
    when(nachMandateRequest.getPeriodUntilCancelled()).thenReturn(true);
    when(nachMandateRequest.getAmount()).thenReturn(10.0d);
    when(nachMandateRequest.getBankAccountHolderName()).thenReturn("Dr Jane Doe");
    when(nachMandateRequest.getBankAccountNumber()).thenReturn("42");
    when(nachMandateRequest.getBankAccountType()).thenReturn("3");
    when(nachMandateRequest.getBankName()).thenReturn("Bank Name");
    when(nachMandateRequest.getBranchName()).thenReturn("janedoe/featurebranch");
    when(nachMandateRequest.getDebitFrequencyEnum()).thenReturn("Debit Frequency Enum");
    when(nachMandateRequest.getDebitTypeEnum()).thenReturn("Debit Type Enum");
    when(nachMandateRequest.getExternalReferenceNumber()).thenReturn("42");
    when(nachMandateRequest.getIfsc()).thenReturn("Ifsc");
    when(nachMandateRequest.getMandateRegistrationRequestedDate()).thenReturn("2020-03-01");
    when(nachMandateRequest.getMicr()).thenReturn("Micr");
    when(nachMandateRequest.getMode()).thenReturn("Mode");
    when(nachMandateRequest.getPeriodEndDate()).thenReturn("2020-03-01");
    when(nachMandateRequest.getPeriodStartDate()).thenReturn("2020-03-01");
    when(nachMandateRequest.getStatus()).thenReturn("Status");
    when(nachMandateRequest.getUmrn()).thenReturn("Umrn");

    M2pNachMandateRequestDTO actualM2pNachMandate =
        LoanDataUtil.getM2pNachMandate(nachMandateRequest);

    verify(nachMandateRequest).getAmount();
    verify(nachMandateRequest).getBankAccountHolderName();
    verify(nachMandateRequest).getBankAccountNumber();
    verify(nachMandateRequest).getBankAccountType();
    verify(nachMandateRequest).getBankName();
    verify(nachMandateRequest).getBranchName();
    verify(nachMandateRequest).getDebitFrequencyEnum();
    verify(nachMandateRequest).getDebitTypeEnum();
    verify(nachMandateRequest).getExternalReferenceNumber();
    verify(nachMandateRequest).getIfsc();
    verify(nachMandateRequest).getMandateRegistrationRequestedDate();
    verify(nachMandateRequest).getMicr();
    verify(nachMandateRequest).getMode();
    verify(nachMandateRequest).getPeriodEndDate();
    verify(nachMandateRequest).getPeriodStartDate();
    verify(nachMandateRequest).getPeriodUntilCancelled();
    verify(nachMandateRequest).getStatus();
    verify(nachMandateRequest).getUmrn();
    assertEquals("2020-03-01", actualM2pNachMandate.getMandateRegistrationRequestedDate());
    assertEquals("2020-03-01", actualM2pNachMandate.getPeriodEndDate());
    assertEquals("2020-03-01", actualM2pNachMandate.getPeriodStartDate());
    assertEquals("3", actualM2pNachMandate.getBankAccountType());
    assertEquals("42", actualM2pNachMandate.getBankAccountNumber());
    assertEquals("42", actualM2pNachMandate.getExternalReferenceNumber());
    assertEquals("Bank Name", actualM2pNachMandate.getBankName());
    assertEquals("Debit Frequency Enum", actualM2pNachMandate.getDebitFrequencyEnum());
    assertEquals("Debit Type Enum", actualM2pNachMandate.getDebitTypeEnum());
    assertEquals("Dr Jane Doe", actualM2pNachMandate.getBankAccountHolderName());
    assertEquals("Ifsc", actualM2pNachMandate.getIfsc());
    assertEquals("Micr", actualM2pNachMandate.getMicr());
    assertEquals("Mode", actualM2pNachMandate.getMode());
    assertEquals("Status", actualM2pNachMandate.getStatus());
    assertEquals("Umrn", actualM2pNachMandate.getUmrn());
    assertEquals("dd-MM-yyyy", actualM2pNachMandate.getDateFormat());
    assertEquals("janedoe/featurebranch", actualM2pNachMandate.getBranchName());
    assertEquals(10.0d, actualM2pNachMandate.getAmount().doubleValue());
    assertTrue(actualM2pNachMandate.getPeriodUntilCancelled());
  }

  /** Method under test: {@link LoanDataUtil#getM2pLoanApplicationRequestDTO(LoanApplication)} */
  @Test
  void testGetM2pLoanApplicationRequestDTO() {
    LoanApplication.LoanApplicationBuilder amountResult = LoanApplication.builder().amount(10.0d);
    AssociationsDTO associations =
        AssociationsDTO.builder()
            .anchor("Anchor")
            .merchant("Merchant")
            .self("Self")
            .thirdParty("Third Party")
            .build();
    LoanApplication.LoanApplicationBuilder associationsResult =
        amountResult.associations(associations);
    LoanApplication.LoanApplicationBuilder externalIdResult =
        associationsResult.charges(new ArrayList<>()).externalId("42");
    LoanApplicationTermsDTO leadApplicationTerms =
        LoanApplicationTermsDTO.builder()
            .amountForUpfrontCollection(10.0d)
            .graceOnInterestCharged(1)
            .graceOnPrincipalPayment(1)
            .interestRatePerPeriod(10.0d)
            .maxEligibleAmount(10.0d)
            .numberOfRepayments(10)
            .repayEvery(1)
            .repaymentPeriodFrequencyEnum(10)
            .termFrequency(1)
            .termPeriodFrequencyEnum(10)
            .build();
    LoanApplication loanApplication =
        externalIdResult
            .leadApplicationTerms(leadApplicationTerms)
            .loanOfficerId(1)
            .loanPurposeId(1)
            .losProductKey("Los Product Key")
            .sourcingChannelId(1)
            .build();

    M2PLoanApplicationRequestDTO actualM2pLoanApplicationRequestDTO =
        LoanDataUtil.getM2pLoanApplicationRequestDTO(loanApplication);

    assertEquals("42", actualM2pLoanApplicationRequestDTO.getExternalIdOne());
    assertEquals("Los Product Key", actualM2pLoanApplicationRequestDTO.getLosProductKey());
    M2pLoanApplicationTermsDTO leadApplicationTerms2 =
        actualM2pLoanApplicationRequestDTO.getLeadApplicationTerms();
    assertEquals("dd-MM-yyyy", leadApplicationTerms2.getDateFormat());
    assertEquals(1, actualM2pLoanApplicationRequestDTO.getLoanOfficerId().intValue());
    assertEquals(1, actualM2pLoanApplicationRequestDTO.getSourcingChannelId().intValue());
    assertEquals(1, leadApplicationTerms2.getGraceOnInterestCharged().intValue());
    assertEquals(1, leadApplicationTerms2.getGraceOnPrincipalPayment().intValue());
    assertEquals(1, leadApplicationTerms2.getRepayEvery().intValue());
    assertEquals(1, leadApplicationTerms2.getTermFrequency().intValue());
    assertEquals(10, leadApplicationTerms2.getNumberOfRepayments().intValue());
    assertEquals(10, leadApplicationTerms2.getRepaymentPeriodFrequencyEnum().intValue());
    assertEquals(10, leadApplicationTerms2.getTermPeriodFrequencyEnum().intValue());
    assertEquals(10.0d, actualM2pLoanApplicationRequestDTO.getAmount().doubleValue());
    assertEquals(10.0d, leadApplicationTerms2.getAmountForUpfrontCollection().doubleValue());
    assertEquals(10.0d, leadApplicationTerms2.getInterestRatePerPeriod().doubleValue());
    assertEquals(10.0d, leadApplicationTerms2.getMaxEligibleAmount().doubleValue());
    assertTrue(actualM2pLoanApplicationRequestDTO.getCharges().isEmpty());
  }

  /** Method under test: {@link LoanDataUtil#getM2pLoanApplicationRequestDTO(LoanApplication)} */
  @Test
  void testGetM2pLoanApplicationRequestDTO2() {
    LoanApplication loanApplication = mock(LoanApplication.class);
    when(loanApplication.getExternalId()).thenReturn("42");
    when(loanApplication.getCharges()).thenReturn(new ArrayList<>());
    LoanApplicationTermsDTO buildResult =
        LoanApplicationTermsDTO.builder()
            .amountForUpfrontCollection(10.0d)
            .graceOnInterestCharged(1)
            .graceOnPrincipalPayment(1)
            .interestRatePerPeriod(10.0d)
            .maxEligibleAmount(10.0d)
            .numberOfRepayments(10)
            .repayEvery(1)
            .repaymentPeriodFrequencyEnum(10)
            .termFrequency(1)
            .termPeriodFrequencyEnum(10)
            .build();
    when(loanApplication.getLeadApplicationTerms()).thenReturn(buildResult);
    when(loanApplication.getAmount()).thenReturn(10.0d);
    when(loanApplication.getLoanPurposeId()).thenReturn(1);
    when(loanApplication.getSourcingChannelId()).thenReturn(1);
    when(loanApplication.getLosProductKey()).thenReturn("Los Product Key");

    M2PLoanApplicationRequestDTO actualM2pLoanApplicationRequestDTO =
        LoanDataUtil.getM2pLoanApplicationRequestDTO(loanApplication);

    verify(loanApplication).getAmount();
    verify(loanApplication).getCharges();
    verify(loanApplication).getExternalId();
    verify(loanApplication, atLeast(1)).getLeadApplicationTerms();
    verify(loanApplication).getLoanPurposeId();
    verify(loanApplication).getLosProductKey();
    verify(loanApplication).getSourcingChannelId();
    assertEquals("42", actualM2pLoanApplicationRequestDTO.getExternalIdOne());
    assertEquals("Los Product Key", actualM2pLoanApplicationRequestDTO.getLosProductKey());
    M2pLoanApplicationTermsDTO leadApplicationTerms =
        actualM2pLoanApplicationRequestDTO.getLeadApplicationTerms();
    assertEquals("dd-MM-yyyy", leadApplicationTerms.getDateFormat());
    assertEquals(1, actualM2pLoanApplicationRequestDTO.getLoanOfficerId().intValue());
    assertEquals(1, actualM2pLoanApplicationRequestDTO.getSourcingChannelId().intValue());
    assertEquals(1, leadApplicationTerms.getGraceOnInterestCharged().intValue());
    assertEquals(1, leadApplicationTerms.getGraceOnPrincipalPayment().intValue());
    assertEquals(1, leadApplicationTerms.getRepayEvery().intValue());
    assertEquals(1, leadApplicationTerms.getTermFrequency().intValue());
    assertEquals(10, leadApplicationTerms.getNumberOfRepayments().intValue());
    assertEquals(10, leadApplicationTerms.getRepaymentPeriodFrequencyEnum().intValue());
    assertEquals(10, leadApplicationTerms.getTermPeriodFrequencyEnum().intValue());
    assertEquals(10.0d, actualM2pLoanApplicationRequestDTO.getAmount().doubleValue());
    assertEquals(10.0d, leadApplicationTerms.getAmountForUpfrontCollection().doubleValue());
    assertEquals(10.0d, leadApplicationTerms.getInterestRatePerPeriod().doubleValue());
    assertEquals(10.0d, leadApplicationTerms.getMaxEligibleAmount().doubleValue());
    assertTrue(actualM2pLoanApplicationRequestDTO.getCharges().isEmpty());
  }
}
