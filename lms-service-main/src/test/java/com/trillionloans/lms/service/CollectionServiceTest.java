package com.trillionloans.lms.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.lms.api.m2p.M2PApi;
import com.trillionloans.lms.model.dto.LoanForeClosureDTO;
import com.trillionloans.lms.model.dto.M2pLoanForeClosureDTO;
import com.trillionloans.lms.model.request.CollectChargeRequest;
import com.trillionloans.lms.model.request.MarkCollectionRequest;
import com.trillionloans.lms.model.request.RejectLoanRequest;
import com.trillionloans.lms.model.request.SaveChargesRequest;
import com.trillionloans.lms.model.request.WaiveChargeRequest;
import com.trillionloans.lms.model.response.ForeclosureDetailsResponseDto;
import com.trillionloans.lms.model.response.TransactionDetailResponse;
import com.trillionloans.lms.service.db.PartnerMasterService;
import java.io.UnsupportedEncodingException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {CollectionService.class})
@ExtendWith(SpringExtension.class)
@DisabledInAotMode
class CollectionServiceTest {
  @Autowired private CollectionService collectionService;

  @MockBean private M2PApi m2PApi;
  @MockBean private PartnerMasterService partnerMasterService;

  /**
   * Method under test: {@link CollectionService#getRepaymentScheduleByLoanAccountNumber(String)}
   */
  @Test
  void testGetRepaymentScheduleByLoanAccountNumber() throws AssertionError {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PApi.getRepaymentScheduleByLoanAccountNumber(Mockito.<String>any()))
        .thenReturn(justResult);

    Mono<?> actualPublisher = collectionService.getRepaymentScheduleByLoanAccountNumber("42");

    verify(m2PApi).getRepaymentScheduleByLoanAccountNumber(eq("42"));
    assertSame(justResult, actualPublisher);
    StepVerifier.FirstStep<?> createResult = StepVerifier.create(actualPublisher);
    createResult
        .assertNext(
            o -> {
              assertEquals("Data", o);
              return;
            })
        .expectComplete()
        .verify();
  }

  /** Method under test: {@link CollectionService#getLoanTransactionDetails(String)} */
  @Test
  void testGetTransactionDetails() throws AssertionError {
    TransactionDetailResponse transactionDetailResponse =
        TransactionDetailResponse.builder()
            .clientId("12")
            .transactions(
                List.of(
                    TransactionDetailResponse.Transaction.builder().externalId("bp-123").build()))
            .build();
    Mono<TransactionDetailResponse> justResult = Mono.just(transactionDetailResponse);
    Mockito.when(m2PApi.getLoanTransactionDetails(Mockito.<String>any())).thenReturn(justResult);

    Mono<?> actualPublisher = collectionService.getLoanTransactionDetails("42");

    verify(m2PApi).getLoanTransactionDetails(eq("42"));
    assertSame(justResult, actualPublisher);
    StepVerifier.FirstStep<?> createResult = StepVerifier.create(actualPublisher);
    createResult
        .assertNext(o -> assertEquals(transactionDetailResponse, o))
        .expectComplete()
        .verify();
  }

  /**
   * Method under test: {@link CollectionService#getForeclosureDetailsByLoanAccountNumber(String,
   * String, Boolean, Boolean)}
   */
  @Test
  void testGetForeclosureDetailsByLoanAccountNumber() throws AssertionError {
    ForeclosureDetailsResponseDto justResult =
        ForeclosureDetailsResponseDto.builder().amount(100.00).netForeclosureAmount(111.00).build();
    Mockito.<Mono<?>>when(
            m2PApi.getForeclosureDetailsByLoanAccountNumber(
                Mockito.<String>any(),
                Mockito.<String>any(),
                Mockito.<Boolean>any(),
                Mockito.<Boolean>any()))
        .thenReturn(Mono.just(justResult));

    Mono<ForeclosureDetailsResponseDto> actualPublisher =
        collectionService.getForeclosureDetailsByLoanAccountNumber("42", "2020-03-01", true, true);

    verify(m2PApi)
        .getForeclosureDetailsByLoanAccountNumber(eq("42"), eq("2020-03-01"), eq(true), eq(true));
    StepVerifier.create(actualPublisher)
        .assertNext(
            response -> {
              // Validate that netForeclosureAmount is set to the amount value if it was null
              assertEquals(
                  justResult.getNetForeclosureAmount(), response.getNetForeclosureAmount());
            })
        .expectComplete()
        .verify();
  }

  @Test
  void testGetForeclosureDetailsByLoanAccountNumberWhenNetForeclosureAmountIsNull() {
    // Arrange: Prepare a mock response with an amount set
    ForeclosureDetailsResponseDto justResult =
        ForeclosureDetailsResponseDto.builder().amount(100.00).build();
    Mockito.when(
            m2PApi.getForeclosureDetailsByLoanAccountNumber(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyBoolean(),
                Mockito.anyBoolean()))
        .thenReturn(Mono.just(justResult));

    Mono<ForeclosureDetailsResponseDto> actualPublisher =
        collectionService.getForeclosureDetailsByLoanAccountNumber("42", "2020-03-01", true, true);

    // Assert: Verify the behavior and the results
    verify(m2PApi)
        .getForeclosureDetailsByLoanAccountNumber(eq("42"), eq("2020-03-01"), eq(true), eq(true));

    // Use StepVerifier to check the result
    StepVerifier.create(actualPublisher)
        .assertNext(
            response -> {
              // Validate that netForeclosureAmount is set to the amount value if it was null
              assertEquals(justResult.getAmount(), response.getNetForeclosureAmount());
            })
        .expectComplete()
        .verify();
  }

  /**
   * Method under test: {@link CollectionService#postForeClosureRequest(String, LoanForeClosureDTO)}
   */
  @Test
  void testPostForeClosureRequest() throws AssertionError {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PApi.postForeClosureRequest(
                Mockito.<String>any(), Mockito.<M2pLoanForeClosureDTO>any()))
        .thenReturn(justResult);

    Mono<?> actualPublisher =
        collectionService.postForeClosureRequest("42", new LoanForeClosureDTO());

    verify(m2PApi).postForeClosureRequest(eq("42"), isA(M2pLoanForeClosureDTO.class));
    assertSame(justResult, actualPublisher);
    StepVerifier.FirstStep<?> createResult = StepVerifier.create(actualPublisher);
    createResult
        .assertNext(
            o -> {
              assertEquals("Data", o);
              return;
            })
        .expectComplete()
        .verify();
  }

  @Test
  void testMarkCollection_basicFieldUpdate() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PApi.markCollection(Mockito.any(), Mockito.any()))
        .thenReturn(justResult);
    MarkCollectionRequest markCollectionRequest =
        MarkCollectionRequest.builder().uniqueIdentifier("123").build();
    Mono<?> result = collectionService.markCollection("42", markCollectionRequest, "1001");
    Assertions.assertEquals("dd-MM-yyyy", markCollectionRequest.getDateFormat());
    assertEquals("dd-MM-yyyy'T'HH:mm:ssZ", markCollectionRequest.getTimeFormat());
    assertEquals("en", markCollectionRequest.getLocale());
    assertNull(markCollectionRequest.getExternalId());
    StepVerifier.FirstStep<?> createResult = StepVerifier.create(result);
    createResult
        .assertNext(
            o -> {
              assertEquals("Data", o);
            })
        .expectComplete()
        .verify();
  }

  @Test
  void testMarkCollection_withReceiptNumber_setsExternalId() {
    Mono<?> justResult = Mono.just("Success");
    Mockito.<Mono<?>>when(m2PApi.markCollection(Mockito.any(), Mockito.any()))
        .thenReturn(justResult);

    MarkCollectionRequest markCollectionRequest =
        MarkCollectionRequest.builder().uniqueIdentifier("456").receiptNumber("RCP123").build();

    Mono<?> result = collectionService.markCollection("LN42", markCollectionRequest, "2001");

    assertEquals("dd-MM-yyyy", markCollectionRequest.getDateFormat());
    assertEquals("dd-MM-yyyy'T'HH:mm:ssZ", markCollectionRequest.getTimeFormat());
    assertEquals("en", markCollectionRequest.getLocale());
    assertEquals("RCP123_LN42", markCollectionRequest.getExternalId());

    StepVerifier.create(result)
        .assertNext(o -> assertEquals("Success", o))
        .expectComplete()
        .verify();
  }

  @Test
  void testMarkCollection_allFieldsNull_requestStillWorks() {
    Mono<?> justResult = Mono.just("Fallback");

    Mockito.<Mono<?>>when(m2PApi.markCollection(Mockito.any(), Mockito.any()))
        .thenReturn(justResult);

    MarkCollectionRequest markCollectionRequest = new MarkCollectionRequest();

    Mono<?> result = collectionService.markCollection("ACC001", markCollectionRequest, "3001");

    assertEquals("dd-MM-yyyy", markCollectionRequest.getDateFormat());
    assertEquals("dd-MM-yyyy'T'HH:mm:ssZ", markCollectionRequest.getTimeFormat());
    assertEquals("en", markCollectionRequest.getLocale());
    assertNull(markCollectionRequest.getExternalId());

    StepVerifier.create(result)
        .assertNext(o -> assertEquals("Fallback", o))
        .expectComplete()
        .verify();
  }

  @Test
  void testMarkCollection_nullLoanAccountNumber_stillFormatsCorrectly() {
    Mono<?> justResult = Mono.just("OK");

    Mockito.<Mono<?>>when(m2PApi.markCollection(Mockito.any(), Mockito.any()))
        .thenReturn(justResult);

    MarkCollectionRequest markCollectionRequest =
        MarkCollectionRequest.builder().receiptNumber("RCPT789").build();

    Mono<?> result = collectionService.markCollection(null, markCollectionRequest, "9999");

    assertEquals(
        "RCPT789_null", markCollectionRequest.getExternalId()); // will concatenate with null
    StepVerifier.create(result).assertNext(o -> assertEquals("OK", o)).expectComplete().verify();
  }

  @Test
  void testMarkCollection_emptyReceiptNumber_setsExternalId() {
    Mono<?> justResult = Mono.just("Done");
    Mockito.<Mono<?>>when(m2PApi.markCollection(Mockito.any(), Mockito.any()))
        .thenReturn(justResult);
    MarkCollectionRequest markCollectionRequest =
        MarkCollectionRequest.builder().receiptNumber(null).build();
    Mono<?> result = collectionService.markCollection("LACC999", markCollectionRequest, "X");
    assertNull(markCollectionRequest.getExternalId());
    StepVerifier.create(result).assertNext(o -> assertEquals("Done", o)).expectComplete().verify();
  }

  /** Method under test: {@link CollectionService#saveCharges(String, SaveChargesRequest)} */
  @Test
  void testSaveCharges() throws AssertionError {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PApi.saveCharges(Mockito.<String>any(), Mockito.<SaveChargesRequest>any()))
        .thenReturn(justResult);
    SaveChargesRequest saveChargesRequest = new SaveChargesRequest();

    Mono<?> actualPublisher = collectionService.saveCharges("42", saveChargesRequest);

    verify(m2PApi).saveCharges(eq("42"), isA(SaveChargesRequest.class));
    assertEquals("dd-MM-yyyy", saveChargesRequest.getDateFormat());
    assertEquals("en", saveChargesRequest.getLocale());
    assertSame(justResult, actualPublisher);
    StepVerifier.FirstStep<?> createResult = StepVerifier.create(actualPublisher);
    createResult
        .assertNext(
            o -> {
              assertEquals("Data", o);
            })
        .expectComplete()
        .verify();
  }

  /** Method under test: {@link CollectionService#getCharges(String, String, Boolean)} */
  @Test
  void testGetCharges() throws AssertionError {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PApi.getCharges(Mockito.<String>any(), Mockito.<String>any(), Mockito.<Boolean>any()))
        .thenReturn(justResult);

    Mono<?> actualPublisher = collectionService.getCharges("42", "Exclude", true);

    verify(m2PApi).getCharges(eq("42"), eq("Exclude"), eq(true));
    assertSame(justResult, actualPublisher);
    StepVerifier.FirstStep<?> createResult = StepVerifier.create(actualPublisher);
    createResult
        .assertNext(
            o -> {
              assertEquals("Data", o);
              return;
            })
        .expectComplete()
        .verify();
  }

  /**
   * Method under test: {@link CollectionService#waiveCharge(String, String, WaiveChargeRequest)}
   */
  @Test
  void testWaiveCharge() throws AssertionError {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PApi.waiveCharge(
                Mockito.<String>any(), Mockito.<String>any(), Mockito.<WaiveChargeRequest>any()))
        .thenReturn(justResult);
    WaiveChargeRequest waiveChargeRequest =
        new WaiveChargeRequest("2020-03-01", "en", "2020-03-01");

    Mono<?> actualPublisher = collectionService.waiveCharge("42", "42", waiveChargeRequest);

    verify(m2PApi).waiveCharge(eq("42"), eq("42"), isA(WaiveChargeRequest.class));
    assertEquals("dd-MM-yyyy", waiveChargeRequest.getDateFormat());
    assertEquals("en", waiveChargeRequest.getLocale());
    assertSame(justResult, actualPublisher);
    StepVerifier.FirstStep<?> createResult = StepVerifier.create(actualPublisher);
    createResult
        .assertNext(
            o -> {
              assertEquals("Data", o);
              return;
            })
        .expectComplete()
        .verify();
  }

  /**
   * Method under test: {@link CollectionService#collectCharge(String, String,
   * CollectChargeRequest)}
   */
  @Test
  void testCollectCharge() throws AssertionError {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PApi.collectCharge(
                Mockito.<String>any(), Mockito.<String>any(), Mockito.<CollectChargeRequest>any()))
        .thenReturn(justResult);
    CollectChargeRequest collectChargeRequest = new CollectChargeRequest();

    Mono<?> actualPublisher =
        collectionService.collectCharge("42", "Charge Type", collectChargeRequest);

    verify(m2PApi).collectCharge(eq("42"), eq("Charge Type"), isA(CollectChargeRequest.class));
    assertEquals("dd-MM-yyyy", collectChargeRequest.getDateFormat());
    assertEquals("en", collectChargeRequest.getLocale());
    assertSame(justResult, actualPublisher);
    StepVerifier.FirstStep<?> createResult = StepVerifier.create(actualPublisher);
    createResult
        .assertNext(
            o -> {
              assertEquals("Data", o);
              return;
            })
        .expectComplete()
        .verify();
  }

  /** Method under test: {@link CollectionService#fetchSOAPdf(String, String)} */
  @Test
  void testFetchSOAPdf() throws UnsupportedEncodingException, AssertionError {
    Mono<byte[]> justResult = Mono.just("AXAXAXAX".getBytes("UTF-8"));
    when(m2PApi.fetchSOAPdf(Mockito.<String>any(), Mockito.<String>any())).thenReturn(justResult);

    StepVerifier.FirstStep<byte[]> createResult =
        StepVerifier.create(collectionService.fetchSOAPdf("42", "2020-03-01"));
    createResult
        .assertNext(
            b -> {
              byte[] byteArray = b;
              assertEquals(8, byteArray.length);
              assertArrayEquals(new byte[] {65, 88, 65, 88, 65, 88, 65, 88}, byteArray);
              return;
            })
        .expectComplete()
        .verify();
    verify(m2PApi).fetchSOAPdf(eq("42"), eq("2020-03-01"));
  }

  /** Method under test: {@link CollectionService#rejectLoan(String, RejectLoanRequest)} */
  @Test
  void testRejectLoan() throws AssertionError {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PApi.rejectLoan(Mockito.<String>any(), Mockito.<RejectLoanRequest>any()))
        .thenReturn(justResult);
    RejectLoanRequest rejectLoanRequest = new RejectLoanRequest("2020-03-01", "en", "2020-03-01");

    Mono<?> actualPublisher = collectionService.rejectLoan("42", rejectLoanRequest);

    verify(m2PApi).rejectLoan(eq("42"), isA(RejectLoanRequest.class));
    assertEquals("dd MMMM yyyy", rejectLoanRequest.getDateFormat());
    assertEquals("en", rejectLoanRequest.getLocale());
    assertSame(justResult, actualPublisher);
    StepVerifier.FirstStep<?> createResult = StepVerifier.create(actualPublisher);
    createResult
        .assertNext(
            o -> {
              assertEquals("Data", o);
              return;
            })
        .expectComplete()
        .verify();
  }

  /** Method under test: {@link CollectionService#fetchNOCPdf(String)} */
  @Test
  void testFetchNOCPdf() throws UnsupportedEncodingException, AssertionError {
    Mono<byte[]> justResult = Mono.just("AXAXAXAX".getBytes("UTF-8"));
    when(m2PApi.fetchNOCPdf(Mockito.<String>any())).thenReturn(justResult);

    StepVerifier.FirstStep<byte[]> createResult =
        StepVerifier.create(collectionService.fetchNOCPdf("42"));
    createResult
        .assertNext(
            b -> {
              byte[] byteArray = b;
              assertEquals(8, byteArray.length);
              assertArrayEquals(new byte[] {65, 88, 65, 88, 65, 88, 65, 88}, byteArray);
              return;
            })
        .expectComplete()
        .verify();
    verify(m2PApi).fetchNOCPdf(eq("42"));
  }

  /** Method under test: {@link CollectionService#getRpsWithDpdByLoanAccountNumber(String)} */
  @Test
  void testGetRpsWithDpdByLoanAccountNumber() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PApi.getRpsWithDpdByLoanAccountNumber(Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualRpsWithDpdByLoanAccountNumber =
        collectionService.getRpsWithDpdByLoanAccountNumber("42");
    verify(m2PApi).getRpsWithDpdByLoanAccountNumber(Mockito.<String>any());
    assertSame(justResult, actualRpsWithDpdByLoanAccountNumber);
  }

  /** Method under test: {@link CollectionService#fetchDueAsOnDate(String, String)} */
  @Test
  void testFetchDueAsOnDate() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PApi.fetchDueAsOnDate(Mockito.<String>any(), Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualFetchDueAsOnDateResult = collectionService.fetchDueAsOnDate("42", "2020-03-01");
    verify(m2PApi).fetchDueAsOnDate(Mockito.<String>any(), Mockito.<String>any());
    assertSame(justResult, actualFetchDueAsOnDateResult);
  }

  /** Method under test: {@link CollectionService#fetchAllLoansDueAsOnDate(String)} */
  @Test
  void testFetchAllLoansDueAsOnDate() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PApi.fetchAllLoansDueAsOnDate(Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualFetchAllLoansDueAsOnDateResult =
        collectionService.fetchAllLoansDueAsOnDate("2020-03-01");
    verify(m2PApi).fetchAllLoansDueAsOnDate(Mockito.<String>any());
    assertSame(justResult, actualFetchAllLoansDueAsOnDateResult);
  }

  @Test
  void testGetAllLoanDetailsAgainstLead() {
    Mono<Object> justResult = Mono.just("Loan Details Data");
    Mockito.when(m2PApi.getAllLoanDetailsAgainstLead(Mockito.any())).thenReturn(justResult);
    Mono<Object> actualLoanDetails = collectionService.getAllLoanDetailsAgainstLead("12345");
    verify(m2PApi).getAllLoanDetailsAgainstLead(Mockito.any());
    assertSame(justResult, actualLoanDetails);
    StepVerifier.FirstStep<?> createResult = StepVerifier.create(actualLoanDetails);
    createResult
        .assertNext(
            data -> {
              assertEquals("Loan Details Data", data);
              return;
            })
        .expectComplete()
        .verify();
  }
}
