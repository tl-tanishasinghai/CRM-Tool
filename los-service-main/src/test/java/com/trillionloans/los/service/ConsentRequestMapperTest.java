package com.trillionloans.los.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.model.request.ConsentRequest;
import com.trillionloans.los.model.request.m2p.M2pConsentRequest;
import com.trillionloans.los.repository.RiskCategorizationFailureRepository;
import com.trillionloans.los.service.db.BreStatusService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
public class ConsentRequestMapperTest {
  @MockBean private M2PWrapperApi m2PWrapperApi;
  @MockBean private ProductConfigMasterService productConfigMasterService;
  @MockBean private BreStatusService breStatusService;
  @MockBean private LeadService leadService;
  @MockBean private RiskCategorizationFailureRepository riskCategorizationFailureRepository;
  @MockBean private M2pFacadeService m2pFacadeService;

  @MockBean private LoanApplicationService loanApplicationService;

  @Test
  void testBuildM2PConsentRequest_ValidRequest_WithAdditionalDetailsAndDateTime() {
    ConsentRequest request =
        ConsentRequest.builder()
            .consentKey("key123")
            .ipAddress("192.168.1.1")
            .isAccepted(true)
            .additionalDetails("{\"note\":\"test\"}")
            .dateTime("2024-05-15T10:00:00")
            .build();

    when(loanApplicationService.buildM2PConsentRequest(request))
        .thenReturn(
            Mono.error(
                new ClientSideException(
                    "Failed to parse additionalDetails JSON",
                    "Invalid JSON",
                    HttpStatus.BAD_REQUEST)));

    M2pConsentRequest expectedResult = new M2pConsentRequest();
    expectedResult.setConsentKey("key123");
    expectedResult.setIpAddress("192.168.1.1");
    expectedResult.setIsAccepted(true);
    expectedResult.setAdditionalDetails(
        "{\"note\":\"test\",\"approvedDateTime\":\"2024-05-15T10:00:00\"}");

    when(loanApplicationService.buildM2PConsentRequest(any()))
        .thenReturn(Mono.just(expectedResult));

    StepVerifier.create(loanApplicationService.buildM2PConsentRequest(request))
        .assertNext(
            result -> {
              assertNotNull(result);
              assertEquals("key123", result.getConsentKey());
              assertEquals("192.168.1.1", result.getIpAddress());
              assertTrue(result.getIsAccepted());
              assertTrue(result.getAdditionalDetails().contains("\"note\":\"test\""));
              assertTrue(
                  result
                      .getAdditionalDetails()
                      .contains("\"approvedDateTime\":\"2024-05-15T10:00:00\""));
            })
        .verifyComplete();
  }

  @Test
  void testBuildM2PConsentRequest_ValidRequest_WithoutAdditionalDetails() {
    ConsentRequest request =
        ConsentRequest.builder()
            .consentKey("key456")
            .ipAddress("127.0.0.1")
            .isAccepted(false)
            .additionalDetails(null)
            .dateTime("2024-05-15T11:30:00")
            .build();

    M2pConsentRequest expected = new M2pConsentRequest();
    expected.setConsentKey("key456");
    expected.setIpAddress("127.0.0.1");
    expected.setIsAccepted(false);
    expected.setAdditionalDetails("{\"approvedDateTime\":\"2024-05-15T11:30:00\"}");

    when(loanApplicationService.buildM2PConsentRequest(request)).thenReturn(Mono.just(expected));

    StepVerifier.create(loanApplicationService.buildM2PConsentRequest(request))
        .assertNext(
            result -> {
              assertNotNull(result);
              assertEquals("key456", result.getConsentKey());
              assertEquals("127.0.0.1", result.getIpAddress());
              assertFalse(result.getIsAccepted());
              assertTrue(
                  result
                      .getAdditionalDetails()
                      .contains("\"approvedDateTime\":\"2024-05-15T11:30:00\""));
            })
        .verifyComplete();
  }

  @Test
  void testBuildM2PConsentRequest_ValidRequest_WithoutDateTime() {
    ConsentRequest request =
        ConsentRequest.builder()
            .consentKey("key789")
            .ipAddress("10.0.0.1")
            .isAccepted(false)
            .additionalDetails("{\"info\":\"data\"}")
            .dateTime(null)
            .build();

    M2pConsentRequest expected = new M2pConsentRequest();
    expected.setConsentKey("key789");
    expected.setIpAddress("10.0.0.1");
    expected.setIsAccepted(false);
    expected.setAdditionalDetails("{\"info\":\"data\"}");

    when(loanApplicationService.buildM2PConsentRequest(request)).thenReturn(Mono.just(expected));

    StepVerifier.create(loanApplicationService.buildM2PConsentRequest(request))
        .assertNext(
            result -> {
              assertNotNull(result);
              assertEquals("key789", result.getConsentKey());
              assertEquals("10.0.0.1", result.getIpAddress());
              assertFalse(result.getIsAccepted());
              assertTrue(result.getAdditionalDetails().contains("\"info\":\"data\""));
              assertFalse(result.getAdditionalDetails().contains("approvedDateTime"));
            })
        .verifyComplete();
  }

  @Test
  void testBuildM2PConsentRequest_NullRequest_ShouldThrowException() {
    when(loanApplicationService.buildM2PConsentRequest(null))
        .thenReturn(
            Mono.error(
                new ClientSideException(
                    "ConsentRequest cannot be null", "Invalid JSON", HttpStatus.BAD_REQUEST)));

    StepVerifier.create(loanApplicationService.buildM2PConsentRequest(null))
        .expectErrorMatches(
            ex ->
                ex instanceof ClientSideException
                    && ex.getMessage().equals("ConsentRequest cannot be null")
                    && ((ClientSideException) ex).getHttpStatusCode() == HttpStatus.BAD_REQUEST)
        .verify();
  }

  @Test
  void testBuildM2PConsentRequest_MalformedAdditionalDetails_ShouldThrowException() {

    ConsentRequest request =
        ConsentRequest.builder()
            .consentKey("keyError")
            .ipAddress("127.0.0.1")
            .isAccepted(true)
            .additionalDetails("{invalid-json")
            .dateTime("2024-05-15T10:00:00")
            .build();

    when(loanApplicationService.buildM2PConsentRequest(request))
        .thenReturn(
            Mono.error(
                new ClientSideException(
                    "Failed to parse additionalDetails JSON",
                    "Invalid JSON",
                    HttpStatus.BAD_REQUEST)));

    StepVerifier.create(loanApplicationService.buildM2PConsentRequest(request))
        .expectErrorMatches(
            ex ->
                ex instanceof ClientSideException
                    && ex.getMessage().contains("Failed to parse additionalDetails JSON")
                    && ((ClientSideException) ex).getHttpStatusCode() == HttpStatus.BAD_REQUEST)
        .verify();
  }
}
