package com.trillionloans.los.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.ResponseStatus;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.model.request.MerchantBankDetails;
import com.trillionloans.los.model.request.MerchantChangeRequest;
import com.trillionloans.los.model.request.MerchantDetailsRequest;
import com.trillionloans.los.model.response.ResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {MerchantService.class, Gson.class})
@ExtendWith(SpringExtension.class)
class MerchantServiceTest {
  @MockBean private M2PWrapperApi m2PWrapperApi;
  @Autowired private MerchantService merchantService;

  @Test
  void testStampMerchantDetails() {
    MerchantDetailsRequest merchantDetailsRequest = new MerchantDetailsRequest();
    Mono<?> m2pResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.stampMerchantDetails(Mockito.any())).thenReturn(m2pResult);
    merchantService.stampMerchantDetails(merchantDetailsRequest);
    verify(m2PWrapperApi).stampMerchantDetails(Mockito.any());
  }

  @Test
  void testUpdateMerchantAgainstLoanApplication_EmptyResponse() {
    MerchantChangeRequest merchantChangeRequest = new MerchantChangeRequest();
    String loanId = "some-loan-id";
    when(m2PWrapperApi.updateMerchantAgainstLoanApplication(merchantChangeRequest, loanId))
        .thenReturn(Mono.empty());
    StepVerifier.create(
            merchantService.updateMerchantAgainstLoanApplication(merchantChangeRequest, loanId))
        .expectNextMatches(
            response ->
                response instanceof ResponseDTO
                    && ((ResponseDTO) response)
                        .getMessage()
                        .equals("successfully updated loan application")
                    && ((ResponseDTO) response).getStatus() == ResponseStatus.SUCCESS)
        .verifyComplete();
    verify(m2PWrapperApi, times(1))
        .updateMerchantAgainstLoanApplication(merchantChangeRequest, loanId);
  }

  @Test
  void testStampMerchantDetails_2() {
    MerchantDetailsRequest merchantDetailsRequest = new MerchantDetailsRequest();
    Mockito.<Mono<?>>when(m2PWrapperApi.stampMerchantDetails(Mockito.any()))
        .thenThrow(new ClientSideException("error", "error", HttpStatus.BAD_REQUEST));
    assertThrows(
        ClientSideException.class,
        () -> merchantService.stampMerchantDetails(merchantDetailsRequest));
    verify(m2PWrapperApi).stampMerchantDetails(Mockito.any());
  }

  @Test
  void testStampMerchantBankDetails() {
    MerchantBankDetails merchantDetailsRequest = new MerchantBankDetails();
    Mono<?> m2pResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.stampMerchantBankAccountDetails(Mockito.any(), Mockito.any()))
        .thenReturn(m2pResult);
    merchantService.stampMerchantBankAccountDetails(merchantDetailsRequest, "123");
    verify(m2PWrapperApi).stampMerchantBankAccountDetails(Mockito.any(), Mockito.any());
  }

  @Test
  void testStampMerchantBankDetails_2() {
    MerchantBankDetails merchantDetailsRequest = new MerchantBankDetails();
    Mockito.<Mono<?>>when(
            m2PWrapperApi.stampMerchantBankAccountDetails(Mockito.any(), Mockito.anyString()))
        .thenThrow(new ClientSideException("error", "error", HttpStatus.BAD_REQUEST));
    assertThrows(
        ClientSideException.class,
        () -> merchantService.stampMerchantBankAccountDetails(merchantDetailsRequest, "456"));
    verify(m2PWrapperApi).stampMerchantBankAccountDetails(Mockito.any(), Mockito.anyString());
  }

  @Test
  void testMerchantGet() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.getMerchantAgainstLoanApplication(Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualData = merchantService.getMerchantAgainstLoanApplication("492");
    verify(m2PWrapperApi).getMerchantAgainstLoanApplication(Mockito.<String>any());
    assertSame(justResult, actualData);
  }
}
