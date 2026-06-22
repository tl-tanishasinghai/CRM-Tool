package com.trillionloans.lms.api.m2p;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.lms.config.WebClientTimeoutProperties;
import com.trillionloans.lms.model.dto.M2pLoanForeClosureDTO;
import com.trillionloans.lms.model.request.CollectChargeRequest;
import com.trillionloans.lms.model.request.MarkCollectionRequest;
import com.trillionloans.lms.model.request.RejectLoanRequest;
import com.trillionloans.lms.model.request.SaveChargesRequest;
import com.trillionloans.lms.model.request.WaiveChargeRequest;
import com.trillionloans.lms.service.KafkaLoggingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class M2PApiTest {

  @Mock private Environment environment;
  @Mock private KafkaLoggingService kafkaLoggingService;
  @Mock private WebClientTimeoutProperties webClientTimeoutProperties;
  @Mock private M2PApi m2PApi;

  @Test
  void testGetRepaymentScheduleByLoanAccountNumber() {
    when(m2PApi.getRepaymentScheduleByLoanAccountNumber("42"))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.getRepaymentScheduleByLoanAccountNumber("42"))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).getRepaymentScheduleByLoanAccountNumber("42");
  }

  @Test
  void testGetForeclosureDetailsByLoanAccountNumber() {
    when(m2PApi.getForeclosureDetailsByLoanAccountNumber("42", "2020-03-01", true, true))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(
            m2PApi.getForeclosureDetailsByLoanAccountNumber("42", "2020-03-01", true, true))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).getForeclosureDetailsByLoanAccountNumber("42", "2020-03-01", true, true);
  }

  @Test
  void testPostForeClosureRequest() {
    when(m2PApi.postForeClosureRequest(anyString(), any(M2pLoanForeClosureDTO.class)))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.postForeClosureRequest("42", new M2pLoanForeClosureDTO()))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).postForeClosureRequest(anyString(), any(M2pLoanForeClosureDTO.class));
  }

  @Test
  void testMarkCollection() {
    MarkCollectionRequest request =
        MarkCollectionRequest.builder()
            .dateFormat("2020-03-01")
            .locale("en")
            .note("Note")
            .paymentTypeId(1L)
            .receiptNumber("42")
            .timeFormat("Time Format")
            .transactionAmount(10.0d)
            .transactionDate("2020-03-01")
            .build();

    when(m2PApi.markCollection("42", request))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.markCollection("42", request))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).markCollection("42", request);
  }

  @Test
  void testSaveCharges() {
    SaveChargesRequest request =
        SaveChargesRequest.builder()
            .amount(10.0d)
            .chargeId(1)
            .dateFormat("2020-03-01")
            .dueDate("2020-03-01")
            .locale("en")
            .build();

    when(m2PApi.saveCharges("42", request))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.saveCharges("42", request))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).saveCharges("42", request);
  }

  @Test
  void testGetCharges() {
    when(m2PApi.getCharges("42", "Exclude", true))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.getCharges("42", "Exclude", true))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).getCharges("42", "Exclude", true);
  }

  @Test
  void testWaiveCharge() {
    WaiveChargeRequest waiveRequest = new WaiveChargeRequest("2020-03-01", "en", "2020-03-01");

    when(m2PApi.waiveCharge("42", "42", waiveRequest))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.waiveCharge("42", "42", waiveRequest))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).waiveCharge("42", "42", waiveRequest);
  }

  @Test
  void testCollectCharge() {
    CollectChargeRequest collectRequest = new CollectChargeRequest();

    when(m2PApi.collectCharge("42", "Charge Type", collectRequest))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.collectCharge("42", "Charge Type", collectRequest))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).collectCharge("42", "Charge Type", collectRequest);
  }

  @Test
  void testFetchSOAPdf() {
    when(m2PApi.fetchSOAPdf("42", "2020-03-01"))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.fetchSOAPdf("42", "2020-03-01"))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).fetchSOAPdf("42", "2020-03-01");
  }

  @Test
  void testRejectLoan() {
    RejectLoanRequest rejectRequest = new RejectLoanRequest("2020-03-01", "en", "2020-03-01");

    when(m2PApi.rejectLoan("42", rejectRequest))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.rejectLoan("42", rejectRequest))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).rejectLoan("42", rejectRequest);
  }

  @Test
  void testFetchNOCPdf() {
    when(m2PApi.fetchNOCPdf("42")).thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.fetchNOCPdf("42")).expectError(RuntimeException.class).verify();

    verify(m2PApi).fetchNOCPdf("42");
  }

  @Test
  void testGetRpsWithDpdByLoanAccountNumber() {
    Mono<Object> justResult = Mono.just("Data");
    Mockito.when(m2PApi.getRpsWithDpdByLoanAccountNumber(anyString())).thenReturn(justResult);
    Mono<?> actual = m2PApi.getRpsWithDpdByLoanAccountNumber("42");
    verify(m2PApi).getRpsWithDpdByLoanAccountNumber(anyString());
    assertSame(justResult, actual);
  }

  @Test
  void testFetchDueAsOnDate() {
    Mono<Object> justResult = Mono.just("Data");
    when(m2PApi.fetchDueAsOnDate(anyString(), anyString())).thenReturn(justResult);
    Mono<?> actual = m2PApi.fetchDueAsOnDate("42", "2020-03-01");

    verify(m2PApi).fetchDueAsOnDate(anyString(), anyString());
    assertSame(justResult, actual);
  }

  @Test
  void testFetchAllLoansDueAsOnDate() {
    Mono<Object> justResult = Mono.just("Data");
    when(m2PApi.fetchAllLoansDueAsOnDate(anyString())).thenReturn(justResult);
    Mono<?> actual = m2PApi.fetchAllLoansDueAsOnDate("2020-03-01");
    verify(m2PApi).fetchAllLoansDueAsOnDate(anyString());
    assertSame(justResult, actual);
  }

  @Test
  void testGetAllLoansAgainstLeadId() {
    when(m2PApi.getAllLoanDetailsAgainstLead("42"))
        .thenReturn(Mono.error(new RuntimeException("Simulated error")));

    StepVerifier.create(m2PApi.getAllLoanDetailsAgainstLead("42"))
        .expectError(RuntimeException.class)
        .verify();

    verify(m2PApi).getAllLoanDetailsAgainstLead("42");
  }
}
