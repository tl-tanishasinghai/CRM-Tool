package com.trillionloans.los.api.partner;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.los.constant.AadhaarXMLType;
import com.trillionloans.los.model.dto.ApproveLoanDTO;
import com.trillionloans.los.model.dto.LivelinessScoreDataTableDTO;
import com.trillionloans.los.model.dto.LoanChargesDTO;
import com.trillionloans.los.model.dto.RiskDetailsDataTableDTO;
import com.trillionloans.los.model.dto.TopUpDataTableDTO;
import com.trillionloans.los.model.partner.m2p.M2PLoanApplicationRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadUpdateDTO;
import com.trillionloans.los.model.partner.m2p.M2pLoanApplicationAssociationsDTO;
import com.trillionloans.los.model.partner.m2p.M2pLoanApplicationTermsDTO;
import com.trillionloans.los.model.partner.m2p.M2pNachMandateRequestDTO;
import com.trillionloans.los.model.request.AadhaarXmlRequest;
import com.trillionloans.los.model.request.BulkDocumentsUploadRequest;
import com.trillionloans.los.model.request.LeadBulkDocumentsUploadRequest;
import com.trillionloans.los.model.request.LoanBankAccountDataTableDTO;
import com.trillionloans.los.model.request.LoanReject;
import com.trillionloans.los.model.request.SelfieUpload;
import com.trillionloans.los.model.request.UpdateLoanApplication;
import com.trillionloans.los.model.request.m2p.M2pBankDetailsRequestDTO;
import com.trillionloans.los.model.request.m2p.M2pConsentRequest;
import com.trillionloans.los.model.request.m2p.M2pInitiateDisbursalDTO;
import com.trillionloans.los.model.response.GetLoanV2ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pBreDataResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pClientCreationResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanCreationResponseDTO;
import com.trillionloans.los.model.response.okyc.M2pAadhaarXmlResponseDTO;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {M2PWrapperApi.class, String.class})
@ExtendWith(SpringExtension.class)
@DisabledInAotMode
class M2PWrapperApiTest {
  @MockBean private Environment environment;

  @MockBean private M2PWrapperApi m2PWrapperApi;
  @MockBean private KafkaLoggingService kafkaLoggingService;

  /** Method under test: {@link M2PWrapperApi#getLeadData(String)} */
  @Test
  void testGetLeadData() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .getLeadData("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#getLoanApplicationByLoanIdV2(String, String)} */
  @Test
  void testGetLoanApplicationByLoanIdV2() throws AssertionError {

    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<GetLoanV2ResponseDTO> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .getLoanApplicationByLoanIdV2("42", null));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#getLoanApplications(String)} */
  @Test
  void testGetLoanApplications() throws AssertionError {

    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .getLoanApplications("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#getLoanApplicationByLoanId(String)} */
  @Test
  void testGetLoanApplicationByLoanId() throws AssertionError {

    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .getLoanApplicationByLoanId("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#createLead(M2pLeadRequestDTO)} */
  @Test
  void testCreateLead() throws AssertionError {

    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<M2pClientCreationResponseDTO> createResult =
        StepVerifier.create(m2pWrapperApi.createLead(new M2pLeadRequestDTO()));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#processM2pDedupeRequest(String)} */
  @Test
  void testProcessM2pDedupeRequest() throws AssertionError {

    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<M2pClientCreationResponseDTO> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .processM2pDedupeRequest("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#createLoan(M2PLoanApplicationRequestDTO, String)} */
  @Test
  void testCreateLoan() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);
    M2pLoanApplicationTermsDTO leadApplicationTerms =
        M2pLoanApplicationTermsDTO.builder()
            .amountForUpfrontCollection(10.0d)
            .dateFormat("2020-03-01")
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

    StepVerifier.FirstStep<M2pLoanCreationResponseDTO> createResult =
        StepVerifier.create(
            m2pWrapperApi.createLoan(
                new M2PLoanApplicationRequestDTO(
                    1,
                    10.0d,
                    "Los Product Key",
                    1,
                    leadApplicationTerms,
                    new ArrayList<>(),
                    "External Id One",
                    M2pLoanApplicationAssociationsDTO.builder().build()),
                "42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /**
   * Method under test: {@link M2PWrapperApi#updateLoanApplication(String, UpdateLoanApplication,
   * Class)}
   */
  @Test
  void testUpdateLoanApplication() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);
    UpdateLoanApplication loanData =
        new UpdateLoanApplication(
            "10", "Tenure", "Rate Of Interest", "2020-03-01", "2020-03-01", "");

    Class<Object> responseType = Object.class;

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(m2pWrapperApi.updateLoanApplication("42", loanData, responseType));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /**
   * Method under test: {@link M2PWrapperApi#uploadAadhaarXml(AadhaarXmlRequest, String,
   * AadhaarXMLType, String)}
   */
  @Test
  void testUploadAadhaarXml() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<M2pAadhaarXmlResponseDTO> createResult =
        StepVerifier.create(
            m2pWrapperApi.uploadAadhaarXml(
                new AadhaarXmlRequest("Request String"), "42", AadhaarXMLType.DIGI_LOCKER, null));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /**
   * Method under test: {@link M2PWrapperApi#uploadAadhaarXml(AadhaarXmlRequest, String,
   * AadhaarXMLType, String)}
   */
  @Test
  void testUploadAadhaarXml2() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi = new M2PWrapperApi("", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<M2pAadhaarXmlResponseDTO> createResult =
        StepVerifier.create(
            m2pWrapperApi.uploadAadhaarXml(
                new AadhaarXmlRequest("Request String"), "42", AadhaarXMLType.DIGI_LOCKER, null));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /**
   * Method under test: {@link M2PWrapperApi#uploadSelfieAgainstLead(SelfieUpload, String, String)}
   */
  @Test
  void testUploadSelfieAgainstLead() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.uploadSelfieAgainstLead(new SelfieUpload(), "42", null));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /**
   * Method under test: {@link M2PWrapperApi#uploadNachMandateRequest(String,
   * M2pNachMandateRequestDTO)}
   */
  @Test
  void testUploadNachMandateRequest() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            m2pWrapperApi.uploadNachMandateRequest("42", new M2pNachMandateRequestDTO()));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#updateLead(M2pLeadUpdateDTO, String)} */
  @Test
  void testUpdateLead() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.updateLead(new M2pLeadUpdateDTO(), "42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#getKycIdentifiersAgainstLead(String)} */
  @Test
  void testGetKycIdentifiersAgainstLead() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .getKycIdentifiersAgainstLead("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#getNachMandateRequest(String)} */
  @Test
  void testGetNachMandateRequest() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .getNachMandateRequest("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#createConsent(M2pConsentRequest, String, String)} */
  @Test
  void testCreateConsent() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.createConsent(new M2pConsentRequest(), "42", "42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#rejectLoanApplication(LoanReject, String)} */
  @Test
  void testRejectLoanApplication() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.rejectLoanApplication(new LoanReject(), "42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#getRepaymentScheduleByLoanId(String)} */
  @Test
  void testGetRepaymentScheduleByLoanId() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .getRepaymentScheduleByLoanId("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#triggerDisbursement(String)} */
  @Test
  void testTriggerDisbursement() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .triggerDisbursement("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /**
   * Method under test: {@link M2PWrapperApi#uploadDocumentsAgainstLoan(String,
   * BulkDocumentsUploadRequest)}
   */
  @Test
  void testUploadDocumentsAgainstLoan() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            m2pWrapperApi.uploadDocumentsAgainstLoan("42", new BulkDocumentsUploadRequest()));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#getDocumentByLoanIdAndDocumentId(String, String)} */
  @Test
  void testGetDocumentByLoanIdAndDocumentId() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<byte[]> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .getDocumentByLoanIdAndDocumentId("42", "42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#postBreData(Object, String)} */
  @Test
  void testPostBreData() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<M2pBreDataResponseDTO> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .postBreData("Request Body", "42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#postBreData(Object, String)} */
  @Test
  void testPostBreData2() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<M2pBreDataResponseDTO> createResult =
        StepVerifier.create(
            (new M2PWrapperApi("https://example.org/example", "42", "42", env, kafkaLoggingService))
                .postBreData("Request Body", "42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#undoApproveLoan(String)} */
  @Test
  void testUndoApproveLoan() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .undoApproveLoan("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#addCharges(String, LoanChargesDTO)} */
  @Test
  void testAddCharges() {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    m2pWrapperApi.addCharges("42", new LoanChargesDTO());

    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#addCharges(String, LoanChargesDTO)} */
  @Test
  void testAddChargesExpectError() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("Base Url", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.addCharges("42", new LoanChargesDTO()));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#addTopUpDataTable(TopUpDataTableDTO, String)} */
  @Test
  void testAddTopUpDataTable() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            m2pWrapperApi.addTopUpDataTable(
                new TopUpDataTableDTO("42", "10", "en", "2020-03-01", "Sourcing Channel"), "42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#approveLoan(String, ApproveLoanDTO)} */
  @Test
  void testApproveLoan() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            m2pWrapperApi.approveLoan("42", new ApproveLoanDTO("2020-03-01", "2020-03-01")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#registerCta(String, String)} */
  @Test
  void testRegisterCta() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new M2PWrapperApi(
                    "https://example.org/example", "ABC123", "42", env, kafkaLoggingService))
                .registerCta("42", "Cta Name"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /**
   * Method under test: {@link M2PWrapperApi#uploadDocumentsAgainstLead(String,
   * LeadBulkDocumentsUploadRequest)}
   */
  @Test
  void testUploadDocumentsAgainstLead() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            m2pWrapperApi.uploadDocumentsAgainstLead("42", new LeadBulkDocumentsUploadRequest()));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#fetchBankAccountDetails(String)} */
  @Test
  void testFetchBankAccountDetails() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.fetchBankAccountDetails("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /**
   * Method under test: {@link M2PWrapperApi#addBankAccountDetails(String,
   * M2pBankDetailsRequestDTO)}
   */
  @Test
  void testAddBankAccountDetails() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            m2pWrapperApi.addBankAccountDetails("42", new M2pBankDetailsRequestDTO()));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /**
   * Method under test: {@link M2PWrapperApi#addBankDetailsDataTable(LoanBankAccountDataTableDTO,
   * String)}
   */
  @Test
  void testAddBankDetailsDataTable() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            m2pWrapperApi.addBankDetailsDataTable(new LoanBankAccountDataTableDTO(), "42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /** Method under test: {@link M2PWrapperApi#getBankDetailsDataTable(String)} */
  @Test
  void testGetBankDetailsDataTable() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.getBankDetailsDataTable("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  /**
   * Method under test: {@link M2PWrapperApi#initiateLoanDisbursement(String,
   * M2pInitiateDisbursalDTO)}
   */
  @Test
  void testInitiateLoanDisbursement() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            m2pWrapperApi.initiateLoanDisbursement("42", new M2pInitiateDisbursalDTO()));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  @Test
  void testLivelinessScoreUpload() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            m2pWrapperApi.uploadScoreAgainstLead(
                new LivelinessScoreDataTableDTO("61", "33", "14", "144", "123.45", "43677431"),
                "55"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  @Test
  void testGetRiskAgainstLoanId() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.getRiskDetailsAgainstLoanId("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  @Test
  void testUploadRiskAgainstLead() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            m2pWrapperApi.uploadRiskAgainstLead(
                RiskDetailsDataTableDTO.builder()
                    .leadId("43")
                    .risk("LOW_RISK")
                    .timestamp("123")
                    .build(),
                "lead"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  @Test
  void testGetLeadInfo() {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("42");

    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult = StepVerifier.create(m2pWrapperApi.getLeadInfo("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  @Test
  void testGetDocumentList() {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("42");

    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.getDocumentList("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }

  @Test
  void testLoanByExternalId() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.getLoanByExternalId("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testDisbursementStatusV2() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.<String>any())).thenReturn("42");
    M2PWrapperApi m2pWrapperApi =
        new M2PWrapperApi("https://example.org/example", "ABC123", "42", env, kafkaLoggingService);

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(m2pWrapperApi.getLoanDisbursementStatusV2("42"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.<String>any());
  }
}
