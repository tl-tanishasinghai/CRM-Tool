package com.trillionloans.los.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.trillionloans.los.api.partner.KarzaApi;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.NexusApi;
import com.trillionloans.los.api.partner.PartnerApi;
import com.trillionloans.los.config.RejectionReasonCodeFactory;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.request.m2p.M2pCkycrCallbackRequest;
import com.trillionloans.los.model.request.m2p.M2pDisbursementCallBackRequest;
import com.trillionloans.los.model.request.m2p.M2pKycCallBackWithAmlRequest;
import com.trillionloans.los.repository.RiskCategorizationFailureRepository;
import com.trillionloans.los.service.db.BreStatusService;
import com.trillionloans.los.service.db.CallbackStoreService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@ContextConfiguration(classes = {M2pFacadeService.class, Gson.class})
@ExtendWith(SpringExtension.class)
class M2pFacadeServiceTest {

  @MockBean private M2PWrapperApi m2PWrapperApi;
  @MockBean private PartnerApi partnerApi;
  @MockBean private KarzaApi karzaApi;
  @MockBean private CallbackStoreService callbackStoreService;
  @MockBean private ProductConfigMasterService productConfigMasterService;
  @MockBean private BreStatusService breStatusService;
  @MockBean private NexusApi nexusApi;
  @MockBean private LeadService leadService;
  @MockBean private RiskCategorizationFailureRepository riskCategorizationFailureRepository;
  @MockBean private RejectionReasonCodeFactory reasonCodeFactory;

  @Autowired private M2pFacadeService m2pFacadeService;

  /**
   * Method under test: {@link
   * M2pFacadeService#registerDisbursementStatus(M2pDisbursementCallBackRequest)}
   */
  @Test
  void testRegisterDisbursementStatus() {
    Mono<Tuple2<String, ProductControl>> justResult =
        Mono.<Tuple2<String, ProductControl>>just(mock(Tuple2.class));
    when(productConfigMasterService.getProductConfigMasterData(Mockito.<String>any()))
        .thenReturn(justResult);
    m2pFacadeService.registerDisbursementStatus(new M2pDisbursementCallBackRequest());
    verify(productConfigMasterService).getProductConfigMasterData(Mockito.<String>any());
  }

  /** Method under test: */
  @Test
  void testRegisterESignStatus() {
    Mono<Tuple2<String, ProductControl>> justResult =
        Mono.<Tuple2<String, ProductControl>>just(mock(Tuple2.class));
    when(productConfigMasterService.getProductConfigMasterData(Mockito.<String>any()))
        .thenReturn(justResult);
    M2pKycCallBackWithAmlRequest requestBody = new M2pKycCallBackWithAmlRequest();
    m2pFacadeService.registerESignStatus(requestBody);
    verify(productConfigMasterService).getProductConfigMasterData(Mockito.<String>any());
  }

  /** Method under test: */
  @Test
  void testRegisterESignStatusWithNumericInput() {
    Mono<Tuple2<String, ProductControl>> justResult =
        Mono.<Tuple2<String, ProductControl>>just(mock(Tuple2.class));
    when(productConfigMasterService.getProductConfigMasterData(Mockito.<String>any()))
        .thenReturn(justResult);
    M2pKycCallBackWithAmlRequest requestBody =
        M2pKycCallBackWithAmlRequest.builder().loanApplicationId("42").build();
    m2pFacadeService.registerESignStatus(requestBody);
    verify(productConfigMasterService).getProductConfigMasterData(Mockito.<String>any());
  }

  /** Method under test: */
  @Test
  void testRegisterESignStatusWhenBaseExceptionOccurs() {
    when(productConfigMasterService.getProductConfigMasterData(Mockito.<String>any()))
        .thenThrow(new BaseException("An error occurred", "Client Response", null));
    M2pKycCallBackWithAmlRequest requestBody =
        M2pKycCallBackWithAmlRequest.builder().loanApplicationId("42").build();
    assertThrows(BaseException.class, () -> m2pFacadeService.registerESignStatus(requestBody));
    verify(productConfigMasterService).getProductConfigMasterData(Mockito.<String>any());
  }

  /**
   * Method under test: {@link
   * M2pFacadeService#registerDisbursementStatusToPartnerWithoutM2pCta(M2pDisbursementCallBackRequest,
   * String, String, ProductControl.Flow, Boolean)}
   */
  @Test
  void testRegisterDisbursementStatusToPartnerWithoutM2pCta() {
    Mono<Object> justResult = Mono.just("Data");
    when(partnerApi.registerDisbursementCallback(
            Mockito.<Object>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<Integer>any()))
        .thenReturn(justResult);
    M2pDisbursementCallBackRequest requestBody =
        M2pDisbursementCallBackRequest.builder()
            .approvedAmount(1)
            .disbursementDate("2020-03-01")
            .lanID(1)
            .loanApplicationId(1)
            .netDisbursement(1)
            .productCode("Product Code")
            .receiptNumber("42")
            .status("Status")
            .build();
    m2pFacadeService.registerDisbursementStatusToPartnerWithoutM2pCta(
        requestBody,
        "Product Code",
        "Partner Code",
        new ProductControl.Flow(
            "42",
            "Function Name",
            "Partner Uri",
            "Call Method",
            3,
            "Logger Header",
            false,
            false,
            0.5,
            0.5,
            true,
            false,
            false,
            false,
            "CTA Name",
            false,
            false,
            new HashMap<>(),
            new ArrayList<>()),
        false);
    verify(partnerApi)
        .registerDisbursementCallback(
            Mockito.<Object>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<Integer>any());
  }

  /**
   * Method under test: {@link
   * M2pFacadeService#registerDisbursementStatusToPartnerWithoutM2pCta(M2pDisbursementCallBackRequest,
   * String, String, ProductControl.Flow, Boolean)}
   */
  @Test
  void testRegisterDisbursementStatusToPartnerWithoutM2pCta2() {
    Mono<Object> justResult = Mono.just("Data");
    when(partnerApi.registerDisbursementCallback(
            Mockito.<Object>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<Integer>any()))
        .thenReturn(justResult);
    M2pDisbursementCallBackRequest requestBody =
        M2pDisbursementCallBackRequest.builder()
            .approvedAmount(1)
            .disbursementDate("2020-03-01")
            .lanID(1)
            .loanApplicationId(1)
            .netDisbursement(1)
            .productCode("Product Code")
            .receiptNumber("42")
            .status("Status")
            .build();
    m2pFacadeService.registerDisbursementStatusToPartnerWithoutM2pCta(
        requestBody,
        "Product Code",
        "Partner Code",
        new ProductControl.Flow(
            "42",
            "Function Name",
            "Partner Uri",
            "Call Method",
            3,
            "Logger Header",
            false,
            false,
            0.5,
            0.5,
            true,
            false,
            false,
            false,
            "Cta Name",
            false,
            false,
            new HashMap<>(),
            new ArrayList<>()),
        false);
    verify(partnerApi)
        .registerDisbursementCallback(
            Mockito.<Object>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<Integer>any());
  }

  /**
   * Method under test: {@link
   * M2pFacadeService#registerDisbursementStatusToPartnerWithoutM2pCta(M2pDisbursementCallBackRequest,
   * String, String, ProductControl.Flow, Boolean)}
   */
  @Test
  void testRegisterDisbursementStatusToPartnerWithoutM2pCta3() {
    Mono<Object> justResult = Mono.just("Data");
    when(partnerApi.registerDisbursementCallback(
            Mockito.<Object>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<Integer>any()))
        .thenReturn(justResult);
    M2pDisbursementCallBackRequest requestBody =
        M2pDisbursementCallBackRequest.builder()
            .approvedAmount(1)
            .disbursementDate("2020-03-01")
            .lanID(1)
            .loanApplicationId(1)
            .netDisbursement(1)
            .productCode("Product Code")
            .receiptNumber("42")
            .status("Status")
            .build();

    m2pFacadeService.registerDisbursementStatusToPartnerWithoutM2pCta(
        requestBody,
        "Product Code",
        "Partner Code",
        new ProductControl.Flow(
            "42",
            "Function Name",
            "Partner Uri",
            "Call Method",
            3,
            "Logger Header",
            false,
            false,
            0.5,
            0.5,
            true,
            false,
            false,
            false,
            "Cta Name",
            false,
            false,
            new HashMap<>(),
            new ArrayList<>()),
        false);
    verify(partnerApi)
        .registerDisbursementCallback(
            Mockito.<Object>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<Integer>any());
  }

  /**
   * Method under test: {@link
   * M2pFacadeService#registerDisbursementStatusToPartnerWithoutM2pCta(M2pDisbursementCallBackRequest,
   * String, String, ProductControl.Flow, Boolean)}
   */
  @Test
  void testRegisterDisbursementStatusToPartnerWithoutM2pCta4() {
    when(partnerApi.registerDisbursementCallback(
            Mockito.<Object>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<Integer>any()))
        .thenThrow(new BaseException("An error occurred", "Client Response", null));
    M2pDisbursementCallBackRequest requestBody =
        M2pDisbursementCallBackRequest.builder()
            .approvedAmount(1)
            .disbursementDate("2020-03-01")
            .lanID(1)
            .loanApplicationId(1)
            .netDisbursement(1)
            .productCode("Product Code")
            .receiptNumber("42")
            .status("Status")
            .build();
    assertThrows(
        BaseException.class,
        () ->
            m2pFacadeService.registerDisbursementStatusToPartnerWithoutM2pCta(
                requestBody,
                "Product Code",
                "Partner Code",
                new ProductControl.Flow(
                    "42",
                    "Function Name",
                    "Partner Uri",
                    "Call Method",
                    3,
                    "Logger Header",
                    false,
                    false,
                    0.5,
                    0.5,
                    true,
                    false,
                    false,
                    false,
                    "Cta Name",
                    false,
                    false,
                    new HashMap<>(),
                    new ArrayList<>()),
                false));
    verify(partnerApi)
        .registerDisbursementCallback(
            Mockito.<Object>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<Integer>any());
  }

  /** Method under test: {@link M2pFacadeService#registerCkycrStatus(M2pCkycrCallbackRequest)} */
  @Test
  void testRegisterCkycrStatus2() {
    Mockito.<Mono<?>>when(m2PWrapperApi.registerCta(Mockito.<String>any(), Mockito.<String>any()))
        .thenThrow(new BaseException("An error occurred", "Client Response", null));
    M2pCkycrCallbackRequest requestBody =
        M2pCkycrCallbackRequest.builder().loanId(1).productCode("Product Code").build();
    assertThrows(BaseException.class, () -> m2pFacadeService.registerCkycrStatus(requestBody));
    verify(m2PWrapperApi).registerCta(Mockito.<String>any(), Mockito.<String>any());
  }
}
