package com.trillionloans.los.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.config.RejectionReasonCodeFactory;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.model.dto.ApproveLoanDTO;
import com.trillionloans.los.model.dto.AssociationsDTO;
import com.trillionloans.los.model.dto.LoanApplicationTermsDTO;
import com.trillionloans.los.model.dto.LoanChargesDTO;
import com.trillionloans.los.model.dto.TopUpDataTableDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.partner.m2p.M2PLoanApplicationRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pNachMandateRequestDTO;
import com.trillionloans.los.model.request.AgreementDocumentUploadRequest;
import com.trillionloans.los.model.request.BulkDocumentsUploadRequest;
import com.trillionloans.los.model.request.ConsentRequest;
import com.trillionloans.los.model.request.KycUploadDocumentRequest;
import com.trillionloans.los.model.request.LoanApplication;
import com.trillionloans.los.model.request.LoanReject;
import com.trillionloans.los.model.request.NachMandateRequest;
import com.trillionloans.los.model.request.SaveChargeRequest;
import com.trillionloans.los.model.request.TopupDataRequest;
import com.trillionloans.los.model.request.UpdateLoanApplication;
import com.trillionloans.los.model.request.m2p.M2pConsentRequest;
import com.trillionloans.los.model.response.GetLoanV2ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanCreationResponseDTO;
import com.trillionloans.los.repository.RiskCategorizationFailureRepository;
import com.trillionloans.los.service.ckyc.AadhaarXmlService;
import com.trillionloans.los.service.db.BreStatusService;
import com.trillionloans.los.service.db.PartnerMasterService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import io.netty.buffer.ByteBuf;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@ContextConfiguration(classes = {LoanApplicationService.class, Gson.class})
@ExtendWith(SpringExtension.class)
class LoanApplicationServiceTest {
  @MockBean private Gson gson;
  @MockBean private LeadService leadService;
  @MockBean private Environment environment;
  @MockBean private RiskCategorizationFailureRepository riskCategorizationFailureRepository;
  @MockBean private M2pFacadeService m2pFacadeService;
  @MockBean private AadhaarXmlService aadhaarXmlService;
  @MockBean private RejectionReasonCodeFactory reasonCodeFactory;
  @MockBean private PartnerMasterService partnerMasterService;
  @MockBean private M2PWrapperApi m2PWrapperApi;
  @MockBean private ProductConfigMasterService productConfigMasterService;
  @MockBean private BreStatusService breStatusService;
  @Autowired private LoanApplicationService loanApplicationService;

  /**
   * Method under test: {@link LoanApplicationService#createLoanApplication(LoanApplication, String,
   * String, boolean)}
   */
  @Test
  void testCreateLoanApplication() {
    Mono<M2pLoanCreationResponseDTO> justResult = Mono.just(new M2pLoanCreationResponseDTO());
    when(m2PWrapperApi.createLoan(
            Mockito.<M2PLoanApplicationRequestDTO>any(), Mockito.<String>any()))
        .thenReturn(justResult);
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
    LoanApplication loanData =
        externalIdResult
            .leadApplicationTerms(leadApplicationTerms)
            .loanOfficerId(1)
            .loanPurposeId(1)
            .losProductKey("Los Product Key")
            .sourcingChannelId(1)
            .build();
    loanApplicationService.createLoanApplication(loanData, "42", "Product Code", true);
    verify(m2PWrapperApi)
        .createLoan(Mockito.<M2PLoanApplicationRequestDTO>any(), Mockito.<String>any());
  }

  /**
   * Method under test: {@link LoanApplicationService#createLoanApplication(LoanApplication, String,
   * String, boolean)}
   */
  @Test
  void testCreateLoanApplication2() {
    when(m2PWrapperApi.createLoan(
            Mockito.<M2PLoanApplicationRequestDTO>any(), Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
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
    LoanApplication loanData =
        externalIdResult
            .leadApplicationTerms(leadApplicationTerms)
            .loanOfficerId(1)
            .loanPurposeId(1)
            .losProductKey("Los Product Key")
            .sourcingChannelId(1)
            .build();
    assertThrows(
        ClientSideException.class,
        () -> loanApplicationService.createLoanApplication(loanData, "42", "Product Code", true));
    verify(m2PWrapperApi)
        .createLoan(Mockito.<M2PLoanApplicationRequestDTO>any(), Mockito.<String>any());
  }

  /**
   * Method under test: {@link LoanApplicationService#updateLoan(UpdateLoanApplication, String,
   * String)}
   */
  @Test
  void testUpdateLoan() {
    Mono<Object> justResult = Mono.just("Data");
    when(m2PWrapperApi.updateLoanApplication(
            Mockito.<String>any(),
            Mockito.<UpdateLoanApplication>any(),
            Mockito.<Class<Object>>any()))
        .thenReturn(justResult);

    String partnerCode = "partner-001";
    ProductControl mockProductControl = new ProductControl(new ArrayList<>());
    Tuple2<String, ProductControl> tuple = Tuples.of(partnerCode, mockProductControl);
    when(productConfigMasterService.getProductConfigMasterData(anyString()))
        .thenReturn(Mono.just(tuple));

    loanApplicationService.updateLoan(
        new UpdateLoanApplication(
            "10", "Tenure", "Rate Of Interest", "2020-03-01", "2020-03-01", ""),
        "42",
        "");
  }

  /**
   * Method under test: {@link LoanApplicationService#updateLoan(UpdateLoanApplication, String,
   * String)}
   */
  @Test
  void testUpdateLoan2() {
    when(m2PWrapperApi.updateLoanApplication(
            Mockito.<String>any(),
            Mockito.<UpdateLoanApplication>any(),
            Mockito.<Class<Object>>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));

    assertThrows(
        NullPointerException.class,
        () ->
            loanApplicationService.updateLoan(
                new UpdateLoanApplication(
                    "10", "Tenure", "Rate Of Interest", "2020-03-01", "2020-03-01", ""),
                "42",
                "ELO"));
  }

  /**
   * Method under test: {@link LoanApplicationService#updateLoanApplication(UpdateLoanApplication,
   * String)}
   */
  @Test
  void testUpdateLoanApplication() {
    Mono<Object> justResult = Mono.just("Data");
    when(m2PWrapperApi.updateLoanApplication(
            Mockito.<String>any(),
            Mockito.<UpdateLoanApplication>any(),
            Mockito.<Class<Object>>any()))
        .thenReturn(justResult);
    loanApplicationService.updateLoanApplication(
        new UpdateLoanApplication(
            "10", "Tenure", "Rate Of Interest", "2020-03-01", "2020-03-01", ""),
        "42");
    verify(m2PWrapperApi)
        .updateLoanApplication(
            Mockito.<String>any(),
            Mockito.<UpdateLoanApplication>any(),
            Mockito.<Class<Object>>any());
  }

  /**
   * Method under test: {@link LoanApplicationService#updateLoanApplication(UpdateLoanApplication,
   * String)}
   */
  @Test
  void testUpdateLoanApplication2() {
    when(m2PWrapperApi.updateLoanApplication(
            Mockito.<String>any(),
            Mockito.<UpdateLoanApplication>any(),
            Mockito.<Class<Object>>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class,
        () ->
            loanApplicationService.updateLoanApplication(
                new UpdateLoanApplication(
                    "10", "Tenure", "Rate Of Interest", "2020-03-01", "2020-03-01", ""),
                "42"));
    verify(m2PWrapperApi)
        .updateLoanApplication(
            Mockito.<String>any(),
            Mockito.<UpdateLoanApplication>any(),
            Mockito.<Class<Object>>any());
  }

  /** Method under test: {@link LoanApplicationService#getLoanApplications(String)} */
  @Test
  void testGetLoanApplications() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.getLoanApplications(Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualLoanApplications = loanApplicationService.getLoanApplications("42");
    verify(m2PWrapperApi).getLoanApplications(Mockito.<String>any());
    assertSame(justResult, actualLoanApplications);
  }

  /** Method under test: {@link LoanApplicationService#getLoanApplications(String)} */
  @Test
  void testGetLoanApplications2() {
    Mockito.<Mono<?>>when(m2PWrapperApi.getLoanApplications(Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(ClientSideException.class, () -> loanApplicationService.getLoanApplications("42"));
    verify(m2PWrapperApi).getLoanApplications(Mockito.<String>any());
  }

  /** Method under test: {@link LoanApplicationService#getLoanApplicationByLoanId(String)} */
  @Test
  void testGetLoanApplicationByLoanId() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.getLoanApplicationByLoanId(Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualLoanApplicationByLoanId = loanApplicationService.getLoanApplicationByLoanId("42");
    verify(m2PWrapperApi).getLoanApplicationByLoanId(Mockito.<String>any());
    assertSame(justResult, actualLoanApplicationByLoanId);
  }

  /** Method under test: {@link LoanApplicationService#getLoanApplicationByLoanId(String)} */
  @Test
  void testGetLoanApplicationByLoanId2() {
    Mockito.<Mono<?>>when(m2PWrapperApi.getLoanApplicationByLoanId(Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class, () -> loanApplicationService.getLoanApplicationByLoanId("42"));
    verify(m2PWrapperApi).getLoanApplicationByLoanId(Mockito.<String>any());
  }

  /** Method under test: {@link LoanApplicationService#getLoanApplicationByLoanIdV2(String)} */
  @Test
  void testGetLoanApplicationByLoanIdV2() {
    GetLoanV2ResponseDTO buildResult =
        GetLoanV2ResponseDTO.builder()
            .approvedAmount(10.0d)
            .clientId(1)
            .disbursedDate("2020-03-01")
            .loanApplicationId(1)
            .loanApplicationStatus("Loan Application Status")
            .loanId(1)
            .netDisburseAmount(10.0d)
            .utrNumber("42")
            .build();
    Mono<GetLoanV2ResponseDTO> justResult = Mono.just(buildResult);
    when(m2PWrapperApi.getLoanApplicationByLoanIdV2(Mockito.<String>any(), null))
        .thenReturn(justResult);
    Mono<GetLoanV2ResponseDTO> actualLoanApplicationByLoanIdV2 =
        loanApplicationService.getLoanApplicationByLoanIdV2("42");
    verify(m2PWrapperApi).getLoanApplicationByLoanIdV2(Mockito.<String>any(), null);
    assertSame(justResult, actualLoanApplicationByLoanIdV2);
  }

  /** Method under test: {@link LoanApplicationService#getLoanApplicationByLoanIdV2(String)} */
  @Test
  void testGetLoanApplicationByLoanIdV22() {
    when(m2PWrapperApi.getLoanApplicationByLoanIdV2(Mockito.<String>any(), null))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class, () -> loanApplicationService.getLoanApplicationByLoanIdV2("42"));
    verify(m2PWrapperApi).getLoanApplicationByLoanIdV2(Mockito.<String>any(), null);
  }

  /** Method under test: {@link LoanApplicationService#getNachMandateRequest(String)} */
  @Test
  void testGetNachMandateRequest() {
    Flux<?> justResult = Flux.just("Data");
    Mockito.<Flux<?>>when(m2PWrapperApi.getNachMandateRequest(Mockito.<String>any()))
        .thenReturn(justResult);
    Flux<?> actualNachMandateRequest = loanApplicationService.getNachMandateRequest("42");
    verify(m2PWrapperApi).getNachMandateRequest(Mockito.<String>any());
    assertSame(justResult, actualNachMandateRequest);
  }

  /** Method under test: {@link LoanApplicationService#getNachMandateRequest(String)} */
  @Test
  void testGetNachMandateRequest2() {
    Mockito.<Flux<?>>when(m2PWrapperApi.getNachMandateRequest(Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class, () -> loanApplicationService.getNachMandateRequest("42"));
    verify(m2PWrapperApi).getNachMandateRequest(Mockito.<String>any());
  }

  /**
   * Method under test: {@link
   * LoanApplicationService#uploadAgreementDocumentAgainstLoan(AgreementDocumentUploadRequest,
   * String)}
   */
  @Test
  void testUploadAgreementDocumentAgainstLoan() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.uploadAgreementDocumentAgainstLoan(
                Mockito.<AgreementDocumentUploadRequest>any(), Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualUploadAgreementDocumentAgainstLoanResult =
        loanApplicationService.uploadAgreementDocumentAgainstLoan(
            new AgreementDocumentUploadRequest(), "42");
    verify(m2PWrapperApi)
        .uploadAgreementDocumentAgainstLoan(
            Mockito.<AgreementDocumentUploadRequest>any(), Mockito.<String>any());
    assertSame(justResult, actualUploadAgreementDocumentAgainstLoanResult);
  }

  /**
   * Method under test: {@link
   * LoanApplicationService#uploadAgreementDocumentAgainstLoan(AgreementDocumentUploadRequest,
   * String)}
   */
  @Test
  void testUploadAgreementDocumentAgainstLoan2() {
    Mockito.<Mono<?>>when(
            m2PWrapperApi.uploadAgreementDocumentAgainstLoan(
                Mockito.<AgreementDocumentUploadRequest>any(), Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class,
        () ->
            loanApplicationService.uploadAgreementDocumentAgainstLoan(
                new AgreementDocumentUploadRequest(), "42"));
    verify(m2PWrapperApi)
        .uploadAgreementDocumentAgainstLoan(
            Mockito.<AgreementDocumentUploadRequest>any(), Mockito.<String>any());
  }

  /**
   * Method under test: {@link
   * LoanApplicationService#uploadKycDocumentAgainstLeadId(KycUploadDocumentRequest, String,
   * String)}
   */
  @Test
  void testUploadKycDocumentAgainstLeadId() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.uploadKycDocumentAgainstLeadId(
                Mockito.<KycUploadDocumentRequest>any(),
                Mockito.<String>any(),
                Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualUploadKycDocumentAgainstLeadIdResult =
        loanApplicationService.uploadKycDocumentAgainstLeadId(
            new KycUploadDocumentRequest(), "42", "42");
    verify(m2PWrapperApi)
        .uploadKycDocumentAgainstLeadId(
            Mockito.<KycUploadDocumentRequest>any(), Mockito.<String>any(), Mockito.<String>any());
    assertSame(justResult, actualUploadKycDocumentAgainstLeadIdResult);
  }

  /**
   * Method under test: {@link
   * LoanApplicationService#uploadKycDocumentAgainstLeadId(KycUploadDocumentRequest, String,
   * String)}
   */
  @Test
  void testUploadKycDocumentAgainstLeadId2() {
    Mockito.<Mono<?>>when(
            m2PWrapperApi.uploadKycDocumentAgainstLeadId(
                Mockito.<KycUploadDocumentRequest>any(),
                Mockito.<String>any(),
                Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class,
        () ->
            loanApplicationService.uploadKycDocumentAgainstLeadId(
                new KycUploadDocumentRequest(), "42", "42"));
    verify(m2PWrapperApi)
        .uploadKycDocumentAgainstLeadId(
            Mockito.<KycUploadDocumentRequest>any(), Mockito.<String>any(), Mockito.<String>any());
  }

  /**
   * Method under test: {@link LoanApplicationService#uploadNachMandateRequest(String,
   * NachMandateRequest, String)}
   */
  @Test
  void testUploadNachMandateRequest() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.uploadNachMandateRequest(
                Mockito.<String>any(), Mockito.<M2pNachMandateRequestDTO>any()))
        .thenReturn(justResult);
    Mockito.when(productConfigMasterService.getProductConfigMasterData(Mockito.<String>any()))
        .thenReturn(Mono.empty());
    Mono<?> actualUploadNachMandateRequestResult =
        loanApplicationService.uploadNachMandateRequest(
            "42", new NachMandateRequest(), "TEST_PRODUCT");
    verify(m2PWrapperApi)
        .uploadNachMandateRequest(Mockito.<String>any(), Mockito.<M2pNachMandateRequestDTO>any());
  }

  /**
   * Method under test: {@link LoanApplicationService#uploadNachMandateRequest(String,
   * NachMandateRequest, String)}
   */
  @Test
  void testUploadNachMandateRequest2() {
    Mockito.<Mono<?>>when(
            m2PWrapperApi.uploadNachMandateRequest(
                Mockito.<String>any(), Mockito.<M2pNachMandateRequestDTO>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class,
        () ->
            loanApplicationService.uploadNachMandateRequest(
                "42", new NachMandateRequest(), "TEST_PRODUCT"));
    verify(m2PWrapperApi)
        .uploadNachMandateRequest(Mockito.<String>any(), Mockito.<M2pNachMandateRequestDTO>any());
  }

  /**
   * Method under test: {@link LoanApplicationService#createConsent(ConsentRequest, String, String)}
   */
  @Test
  void testCreateConsent() {
    Mono<String> justResult = Mono.just("Data");

    Mockito.<Mono<?>>when(
            m2PWrapperApi.createConsent(
                Mockito.<M2pConsentRequest>any(), Mockito.<String>any(), Mockito.<String>any()))
        .thenReturn(justResult);

    Mono<?> actualCreateConsentResult =
        loanApplicationService.createConsent(new ConsentRequest(), "42", "42");

    StepVerifier.create((Mono<String>) actualCreateConsentResult)
        .expectNext("Data")
        .verifyComplete();
  }

  /**
   * Method under test: {@link LoanApplicationService#createConsent(ConsentRequest, String, String)}
   */
  @Test
  void testCreateConsent2() {
    // Mock to throw exception when called
    when(m2PWrapperApi.createConsent(any(M2pConsentRequest.class), anyString(), anyString()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));

    Mono<?> result = loanApplicationService.createConsent(new ConsentRequest(), "42", "42");

    StepVerifier.create(result).expectError(ClientSideException.class).verify();

    verify(m2PWrapperApi).createConsent(any(M2pConsentRequest.class), anyString(), anyString());
  }

  /** Method under test: {@link LoanApplicationService#rejectLoanApplication(LoanReject, String)} */
  @Test
  void testRejectLoanApplication() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.rejectLoanApplication(Mockito.<LoanReject>any(), Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualRejectLoanApplicationResult =
        loanApplicationService.rejectLoanApplication(new LoanReject(), "42");
    verify(m2PWrapperApi).rejectLoanApplication(Mockito.<LoanReject>any(), Mockito.<String>any());
    assertSame(justResult, actualRejectLoanApplicationResult);
  }

  /** Method under test: {@link LoanApplicationService#rejectLoanApplication(LoanReject, String)} */
  @Test
  void testRejectLoanApplication2() {
    Mockito.<Mono<?>>when(
            m2PWrapperApi.rejectLoanApplication(Mockito.<LoanReject>any(), Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class,
        () -> loanApplicationService.rejectLoanApplication(new LoanReject(), "42"));
    verify(m2PWrapperApi).rejectLoanApplication(Mockito.<LoanReject>any(), Mockito.<String>any());
  }

  /** Method under test: {@link LoanApplicationService#getRepaymentScheduleByLoanId(String)} */
  @Test
  void testGetRepaymentScheduleByLoanId() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.getRepaymentScheduleByLoanId(Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualRepaymentScheduleByLoanId =
        loanApplicationService.getRepaymentScheduleByLoanId("42");
    verify(m2PWrapperApi).getRepaymentScheduleByLoanId(Mockito.<String>any());
    assertSame(justResult, actualRepaymentScheduleByLoanId);
  }

  /** Method under test: {@link LoanApplicationService#getRepaymentScheduleByLoanId(String)} */
  @Test
  void testGetRepaymentScheduleByLoanId2() {
    Mockito.<Mono<?>>when(m2PWrapperApi.getRepaymentScheduleByLoanId(Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class, () -> loanApplicationService.getRepaymentScheduleByLoanId("42"));
    verify(m2PWrapperApi).getRepaymentScheduleByLoanId(Mockito.<String>any());
  }

  /**
   * Method under test: {@link
   * LoanApplicationService#uploadDocumentsAgainstLoan(BulkDocumentsUploadRequest, String, String)}
   */
  @Test
  void testUploadDocumentsAgainstLoan() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.uploadDocumentsAgainstLoan(
                Mockito.<String>any(), Mockito.<BulkDocumentsUploadRequest>any()))
        .thenReturn(justResult);
    loanApplicationService.uploadDocumentsAgainstLoan(
        new BulkDocumentsUploadRequest(), "42", "Product Code");
    verify(m2PWrapperApi)
        .uploadDocumentsAgainstLoan(
            Mockito.<String>any(), Mockito.<BulkDocumentsUploadRequest>any());
  }

  /**
   * Method under test: {@link
   * LoanApplicationService#uploadDocumentsAgainstLoan(BulkDocumentsUploadRequest, String, String)}
   */
  @Test
  void testUploadDocumentsAgainstLoan_WithValidParameters() {
    ByteBufMono byteBufMono = mock(ByteBufMono.class);
    Mono<Object> justResult = Mono.just("Data");
    when(byteBufMono.flatMap(Mockito.<Function<ByteBuf, Mono<Object>>>any()))
        .thenReturn(justResult);
    Mockito.<Mono<?>>when(
            m2PWrapperApi.uploadDocumentsAgainstLoan(
                Mockito.<String>any(), Mockito.<BulkDocumentsUploadRequest>any()))
        .thenReturn(byteBufMono);
    Mono<?> actualUploadDocumentsAgainstLoanResult =
        loanApplicationService.uploadDocumentsAgainstLoan(
            new BulkDocumentsUploadRequest(), "42", "Product Code");
    verify(m2PWrapperApi)
        .uploadDocumentsAgainstLoan(
            Mockito.<String>any(), Mockito.<BulkDocumentsUploadRequest>any());
    verify(byteBufMono).flatMap(Mockito.<Function<ByteBuf, Mono<Object>>>any());
    assertSame(justResult, actualUploadDocumentsAgainstLoanResult);
  }

  /**
   * Method under test: {@link LoanApplicationService#getDocumentByLoanIdAndDocumentId(String,
   * String)}
   */
  @Test
  void testGetDocumentByLoanIdAndDocumentId() throws UnsupportedEncodingException {
    Mono<byte[]> justResult = Mono.just("AXAXAXAX".getBytes("UTF-8"));
    when(m2PWrapperApi.getDocumentByLoanIdAndDocumentId(
            Mockito.<String>any(), Mockito.<String>any()))
        .thenReturn(justResult);
    loanApplicationService.getDocumentByLoanIdAndDocumentId("42", "42");
    verify(m2PWrapperApi)
        .getDocumentByLoanIdAndDocumentId(Mockito.<String>any(), Mockito.<String>any());
  }

  /**
   * Method under test: {@link LoanApplicationService#getDocumentByLoanIdAndDocumentId(String,
   * String)}
   */
  @Test
  void testGetDocumentThrowsClientSideException() {
    when(m2PWrapperApi.getDocumentByLoanIdAndDocumentId(
            Mockito.<String>any(), Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class,
        () -> loanApplicationService.getDocumentByLoanIdAndDocumentId("42", "42"));
    verify(m2PWrapperApi)
        .getDocumentByLoanIdAndDocumentId(Mockito.<String>any(), Mockito.<String>any());
  }

  /** Method under test: {@link LoanApplicationService#undoApproveLoan(String)} */
  @Test
  void testUndoApproveLoan() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.undoApproveLoan(Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualUndoApproveLoanResult = loanApplicationService.undoApproveLoan("42");
    verify(m2PWrapperApi).undoApproveLoan(Mockito.<String>any());
    assertSame(justResult, actualUndoApproveLoanResult);
  }

  /** Method under test: {@link LoanApplicationService#undoApproveLoan(String)} */
  @Test
  void testUndoApproveLoanThrowsClientSideException() {
    Mockito.<Mono<?>>when(m2PWrapperApi.undoApproveLoan(Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(ClientSideException.class, () -> loanApplicationService.undoApproveLoan("42"));
    verify(m2PWrapperApi).undoApproveLoan(Mockito.<String>any());
  }

  /** Method under test: {@link LoanApplicationService#addCharges(SaveChargeRequest, String)} */
  @Test
  void testAddCharges() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.addCharges(Mockito.<String>any(), Mockito.<LoanChargesDTO>any()))
        .thenReturn(justResult);
    Mono<?> actualAddChargesResult =
        loanApplicationService.addCharges(new SaveChargeRequest(), "42");
    verify(m2PWrapperApi).addCharges(Mockito.<String>any(), Mockito.<LoanChargesDTO>any());
    assertSame(justResult, actualAddChargesResult);
  }

  /** Method under test: {@link LoanApplicationService#addCharges(SaveChargeRequest, String)} */
  @Test
  void testAddChargesWithValidParameters() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.addCharges(Mockito.<String>any(), Mockito.<LoanChargesDTO>any()))
        .thenReturn(justResult);
    Mono<?> actualAddChargesResult =
        loanApplicationService.addCharges(new SaveChargeRequest(1, 10.0d, true, true), "42");
    verify(m2PWrapperApi).addCharges(Mockito.<String>any(), Mockito.<LoanChargesDTO>any());
    assertSame(justResult, actualAddChargesResult);
  }

  /** Method under test: {@link LoanApplicationService#addCharges(SaveChargeRequest, String)} */
  @Test
  void testAddChargesThrowsClientSideException() {
    Mockito.<Mono<?>>when(
            m2PWrapperApi.addCharges(Mockito.<String>any(), Mockito.<LoanChargesDTO>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class,
        () -> loanApplicationService.addCharges(new SaveChargeRequest(), "42"));
    verify(m2PWrapperApi).addCharges(Mockito.<String>any(), Mockito.<LoanChargesDTO>any());
  }

  /** Method under test: {@link LoanApplicationService#addCharges(SaveChargeRequest, String)} */
  @Test
  void testAddChargesWithSpecificChargeRequest() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.addCharges(Mockito.<String>any(), Mockito.<LoanChargesDTO>any()))
        .thenReturn(justResult);
    Mono<?> actualAddChargesResult =
        loanApplicationService.addCharges(new SaveChargeRequest(1, 10.0d, false, true), "42");
    verify(m2PWrapperApi).addCharges(Mockito.<String>any(), Mockito.<LoanChargesDTO>any());
    assertSame(justResult, actualAddChargesResult);
  }

  /** Method under test: {@link LoanApplicationService#addCharges(SaveChargeRequest, String)} */
  @Test
  void testAddChargesWithSaveChargeRequestAndIdentifier() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.addCharges(Mockito.<String>any(), Mockito.<LoanChargesDTO>any()))
        .thenReturn(justResult);
    Mono<?> actualAddChargesResult =
        loanApplicationService.addCharges(new SaveChargeRequest(1, 10.0d, true, false), "42");
    verify(m2PWrapperApi).addCharges(Mockito.<String>any(), Mockito.<LoanChargesDTO>any());
    assertSame(justResult, actualAddChargesResult);
  }

  /**
   * Method under test: {@link LoanApplicationService#addTopUpDataTable(TopupDataRequest, String)}
   */
  @Test
  void testAddTopUpDataTable() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.addTopUpDataTable(
                Mockito.<TopUpDataTableDTO>any(), Mockito.<String>any()))
        .thenReturn(justResult);
    TopupDataRequest topupDataRequest =
        TopupDataRequest.builder()
            .outstandingAmount(10.0d)
            .sourcingChannel("Sourcing Channel")
            .topupId("42")
            .build();
    Mono<?> actualAddTopUpDataTableResult =
        loanApplicationService.addTopUpDataTable(topupDataRequest, "42");
    verify(m2PWrapperApi)
        .addTopUpDataTable(Mockito.<TopUpDataTableDTO>any(), Mockito.<String>any());
    assertSame(justResult, actualAddTopUpDataTableResult);
  }

  /**
   * Method under test: {@link LoanApplicationService#addTopUpDataTable(TopupDataRequest, String)}
   */
  @Test
  void testAddTopUpDataTableThrowsClientSideException() {
    Mockito.<Mono<?>>when(
            m2PWrapperApi.addTopUpDataTable(
                Mockito.<TopUpDataTableDTO>any(), Mockito.<String>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    TopupDataRequest topupDataRequest =
        TopupDataRequest.builder()
            .outstandingAmount(10.0d)
            .sourcingChannel("Sourcing Channel")
            .topupId("42")
            .build();
    assertThrows(
        ClientSideException.class,
        () -> loanApplicationService.addTopUpDataTable(topupDataRequest, "42"));
    verify(m2PWrapperApi)
        .addTopUpDataTable(Mockito.<TopUpDataTableDTO>any(), Mockito.<String>any());
  }

  /** Method under test: {@link LoanApplicationService#approveLoan(String, String)} */
  @Test
  void testApproveLoan() {
    Mono<Object> justResult = Mono.just("Data");
    when(m2PWrapperApi.approveLoan(Mockito.<String>any(), Mockito.<ApproveLoanDTO>any()))
        .thenReturn(justResult);
    Mono<Object> actualApproveLoanResult = loanApplicationService.approveLoan("42", "2020-03-01");
    verify(m2PWrapperApi).approveLoan(Mockito.<String>any(), Mockito.<ApproveLoanDTO>any());
    assertSame(justResult, actualApproveLoanResult);
  }

  /** Method under test: {@link LoanApplicationService#approveLoan(String, String)} */
  @Test
  void testApproveLoanThrowsClientSideException() {
    when(m2PWrapperApi.approveLoan(Mockito.<String>any(), Mockito.<ApproveLoanDTO>any()))
        .thenThrow(new ClientSideException("An error occurred", "Client Response", null));
    assertThrows(
        ClientSideException.class, () -> loanApplicationService.approveLoan("42", "2020-03-01"));
    verify(m2PWrapperApi).approveLoan(Mockito.<String>any(), Mockito.<ApproveLoanDTO>any());
  }

  @Test
  void testGetDocumentList() {
    Mono<?> justResult = Mono.just("Document Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.getDocumentList(any())).thenReturn(justResult);
    Mono<?> actualDocumentList = loanApplicationService.getDocumentList("42");
    verify(m2PWrapperApi).getDocumentList(any());
    assertSame(justResult, actualDocumentList);
  }
}
