package com.trillionloans.customer_portal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.customer_portal.api.external.FreshdeskApi;
import com.trillionloans.customer_portal.api.internal.LmsApi;
import com.trillionloans.customer_portal.api.internal.LosApi;
import com.trillionloans.customer_portal.constant.ResponseStatus;
import com.trillionloans.customer_portal.model.dto.ClientDetailsCpResponseDto;
import com.trillionloans.customer_portal.model.dto.FreshdeskTicketResponse;
import com.trillionloans.customer_portal.model.dto.LeadDetailsDTO;
import com.trillionloans.customer_portal.model.dto.LeadDetailsResponse;
import com.trillionloans.customer_portal.model.dto.SubmitFormRequest;
import com.trillionloans.customer_portal.model.internal.RpsPdfBuilder;
import com.trillionloans.customer_portal.model.response.ResponseDTO;
import com.trillionloans.customer_portal.model.response.RpsResponseDto.ResponseRpsDTO;
import com.trillionloans.customer_portal.util.LeadDetailsUtil;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

@ContextConfiguration(classes = {CustomerService.class})
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = "redis.cache.details-ttl=3600000")
public class CustomerServiceTest {

  @Autowired private CustomerService customerService;

  @MockBean private LmsApi lmsApi;

  @MockBean private LosApi losApi;

  @MockBean private FreshdeskApi freshdeskApi;

  @MockBean private RpsPdfBuilder rpsPdfBuilder;

  @MockBean private RedisCacheService redisCacheService;

  @Test
  void testGetDetails_skipRedis() {
    // Arrange
    String leadId = "42";

    LeadDetailsResponse dummyResponse = new LeadDetailsResponse();

    LeadDetailsDTO dummyDTO = new LeadDetailsDTO();

    CustomerService spyService = spy(customerService);

    // Mock fetchFromCache to skip Redis
    doReturn(Mono.empty()).when(spyService).fetchFromCache(leadId);

    // Mock external API call
    when(losApi.fetchLeadDetails(leadId)).thenReturn(Mono.just(dummyResponse));

    // Mock static method LeadDetailsUtil.transformLeadDetails to convert response to DTO
    try (MockedStatic<LeadDetailsUtil> theMock = mockStatic(LeadDetailsUtil.class)) {
      theMock.when(() -> LeadDetailsUtil.transformLeadDetails(dummyResponse)).thenReturn(dummyDTO);

      // Mock asyncCacheLeadDetails to complete without error
      doReturn(Mono.empty()).when(spyService).asyncCacheLeadDetails(dummyDTO);

      // Act
      Mono<LeadDetailsDTO> resultMono = spyService.getDetails(leadId);

      // Assert
      StepVerifier.create(resultMono).expectNext(dummyDTO).verifyComplete();

      // Verify interactions
      verify(spyService).fetchFromCache(leadId);
      verify(losApi).fetchLeadDetails(leadId);
      verify(spyService).asyncCacheLeadDetails(dummyDTO);
    }
  }

  @Test
  void testGetSOA() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(lmsApi.fetchSOA(Mockito.<String>any(), Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualSOAData = customerService.getSOA("42");
    verify(lmsApi).fetchSOA(Mockito.<String>any(), Mockito.<String>any());
    assertSame(justResult, actualSOAData);
  }

  @Test
  void testGetNOC() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(lmsApi.fetchNOC(Mockito.<String>any())).thenReturn(justResult);
    Mono<?> actualNOCData = customerService.getNOC("42");
    verify(lmsApi).fetchNOC(Mockito.<String>any());
    assertSame(justResult, actualNOCData);
  }

  @Test
  void testGetDocument() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            losApi.fetchDocumentAgainstId(Mockito.<String>any(), Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualDocument = customerService.getDocument("33", "33");
    verify(losApi).fetchDocumentAgainstId(Mockito.<String>any(), Mockito.<String>any());
    assertSame(justResult, actualDocument);
  }

  @Test
  void testGetAllDocumentDetails() {

    Flux<?> justResult = Flux.empty().switchIfEmpty(Flux.just("Data"));
    Mockito.<Flux<?>>when(losApi.fetchAllDocumentDetailsAgainstLoanAppId(Mockito.<String>any()))
        .thenReturn(justResult);
    Flux<?> actualDocumentDetails = customerService.getAllDocumentDetails("42");
    verify(losApi).fetchAllDocumentDetailsAgainstLoanAppId(Mockito.<String>any());
    assertEquals(justResult.getClass(), actualDocumentDetails.getClass());
  }

  @Test
  void testFetchLeadIdAgainstMobileNumber() {

    Flux<?> justResult = Flux.just("Data").switchIfEmpty(Flux.just("Fallback"));
    Mockito.<Flux<?>>when(losApi.fetchLeadDetailAgainstMobileNumber(Mockito.any()))
        .thenReturn(justResult);
    Flux<?> actualLeadDetails = customerService.fetchLeadIdAgainstMobileNumber("42");
    verify(losApi).fetchLeadDetailAgainstMobileNumber(Mockito.any());
    assertSame(justResult.collectList().getClass(), actualLeadDetails.collectList().getClass());
  }

  @Test
  void testGetTransactionDetails() {

    Mono<?> justResult = Mono.just("Data").flatMap(data -> Mono.just(data + " transformed"));
    Mockito.<Mono<?>>when(lmsApi.fetchTransactionDetails(Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualTransactionDetails = customerService.getTransactionDetails("42");
    verify(lmsApi).fetchTransactionDetails(Mockito.<String>any());
    assertEquals(justResult.getClass(), actualTransactionDetails.getClass());
  }

  @Test
  void testGetAllLoanDetails() {

    Flux<?> justResult = Flux.just("Data").switchIfEmpty(Flux.just("Fallback"));
    Mockito.<Flux<?>>when(lmsApi.fetchAllLoansDetails(Mockito.any())).thenReturn(justResult);
    Flux<?> actualLoanDetails = customerService.getAllLoanDetails("42", null);
    verify(lmsApi).fetchAllLoansDetails(Mockito.any());
    assertSame(justResult.collectList().getClass(), actualLoanDetails.collectList().getClass());
  }

  @Test
  void testSubmitForm_failure_skipValidationAndRedis() {
    // Arrange
    SubmitFormRequest request = new SubmitFormRequest();
    request.setEmail("test@example.com");
    request.setRegisteredMobileNumber("+919876543210");
    request.setConcernCategory("Loan Issue");
    request.setDescription("Some description exceeding 30 chars.");
    request.setLoanId("loan123");
    request.setPanCard("ABCDE1234F");

    CustomerService spyService = Mockito.spy(customerService);

    LeadDetailsDTO dummyLeadDetails = new LeadDetailsDTO();
    dummyLeadDetails.setEmail("mismatch@example.com"); // Cause validation fail
    dummyLeadDetails.setMobileNo("9876543210");
    dummyLeadDetails.setPanNumber("ABCDE1234F");
    dummyLeadDetails.setLoanAccounts(List.of("loan123"));
    dummyLeadDetails.setDateOfBirth("01-01-1980");

    Mockito.doReturn(Mono.just(dummyLeadDetails)).when(spyService).resolveClientDetails("123");
    Mockito.doReturn(Mono.error(new IllegalArgumentException("Email mismatch")))
        .when(spyService)
        .validateRequest(request, dummyLeadDetails);
    Mockito.when(redisCacheService.getKey("123")).thenReturn(Mono.empty());

    // Mock freshdeskApi.createTicket to return empty Mono instead of no interaction
    Mockito.when(freshdeskApi.createTicket(Mockito.any())).thenReturn(Mono.empty());

    // Act
    String testTraceId = UUID.randomUUID().toString();
    Mono<ResponseDTO<String>> resultMono =
        spyService.submitForm(request, "123").contextWrite(Context.of("traceId", testTraceId));

    // Assert
    StepVerifier.create(resultMono)
        .assertNext(
            response -> {
              assertEquals(ResponseStatus.FAIL, response.getStatus());
              assertEquals("Email mismatch", response.getMessage());
              assertEquals(testTraceId, response.getTraceId());
              assertNull(response.getData());
            })
        .verifyComplete();

    Mockito.verify(spyService).resolveClientDetails("123");
    Mockito.verify(spyService).validateRequest(request, dummyLeadDetails);
  }

  @Test
  void testSubmitForm_skipRedisAndValidation() {
    // Arrange
    SubmitFormRequest request = new SubmitFormRequest();
    request.setEmail("test@example.com");
    request.setRegisteredMobileNumber("+919876543210");
    request.setConcernCategory("Loan Issue");
    request.setDescription("This is a detailed description of the issue at hand.");
    request.setLoanId("loan123");
    request.setPanCard("ABCDE1234F");

    FreshdeskTicketResponse ticketResponse = new FreshdeskTicketResponse();
    ticketResponse.setId(101L);
    ticketResponse.setStatus(2);

    LeadDetailsDTO dummyLeadDetails = new LeadDetailsDTO();
    dummyLeadDetails.setEmail(request.getEmail());
    dummyLeadDetails.setMobileNo("9876543210");
    dummyLeadDetails.setPanNumber(request.getPanCard());
    dummyLeadDetails.setLoanAccounts(List.of("loan123"));
    dummyLeadDetails.setDateOfBirth("01-01-1980");

    // Create a spy of the customerService
    CustomerService spyCustomerService = spy(customerService);

    // Stub resolveClientDetails to skip Redis and return dummy LeadDetailsDTO
    doReturn(Mono.just(dummyLeadDetails)).when(spyCustomerService).resolveClientDetails("123");

    // Stub validateRequest to skip validation logic
    doReturn(Mono.empty()).when(spyCustomerService).validateRequest(request, dummyLeadDetails);

    // Stub redisCacheService.getKey to empty Mono to skip Redis in the spy
    when(redisCacheService.getKey("123")).thenReturn(Mono.empty());

    // Mock freshdeskApi.createTicket normally
    when(freshdeskApi.createTicket(eq(request))).thenReturn(Mono.just(ticketResponse));

    // Act
    String testTraceId = UUID.randomUUID().toString();
    Mono<ResponseDTO<String>> resultMono =
        spyCustomerService
            .submitForm(request, "123")
            .contextWrite(Context.of("traceId", testTraceId));

    // Assert
    StepVerifier.create(resultMono)
        .assertNext(
            response -> {
              assertEquals(ResponseStatus.SUCCESS, response.getStatus());
              assertEquals("Form submitted successfully!", response.getMessage());
              assertEquals(testTraceId, response.getTraceId());
              assertEquals("Ticket created with ID: 101", response.getData());
            })
        .verifyComplete();
  }

  @Test
  void testFetchLeadIdAgainstMobileNumberAndDOB() {

    Flux<?> justResult = Flux.just("Data").switchIfEmpty(Flux.just("Fallback"));
    Mockito.<Flux<?>>when(
            losApi.fetchLeadDetailAgainstMobileNumberAndDOB(Mockito.any(), Mockito.any()))
        .thenReturn(justResult);
    Flux<?> actualLeadDetails = customerService.fetchLeadIdAgainstMobileNumberAndDOB("42", "11");
    verify(losApi).fetchLeadDetailAgainstMobileNumberAndDOB(Mockito.any(), Mockito.any());
    assertSame(justResult.collectList().getClass(), actualLeadDetails.collectList().getClass());
  }

  @Test
  void testFetchLeadIdAgainstMobileNumberDOBAndPAN() {

    Flux<?> justResult = Flux.just("Data").switchIfEmpty(Flux.just("Fallback"));
    Mockito.<Flux<?>>when(
            losApi.fetchLeadDetailAgainstMobileNumberDOBAndPAN(
                Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(justResult);
    Flux<?> actualLeadDetails =
        customerService.fetchLeadIdAgainstMobileNumberDOBAndPAN("42", "11", "12");
    verify(losApi)
        .fetchLeadDetailAgainstMobileNumberDOBAndPAN(Mockito.any(), Mockito.any(), Mockito.any());
    assertSame(justResult.collectList().getClass(), actualLeadDetails.collectList().getClass());
  }

  @Test
  void testGetRPS_success() throws IOException {
    String loanAccountNumber = "123";
    String leadId = "456";

    ClientDetailsCpResponseDto clientDetails = new ClientDetailsCpResponseDto();
    clientDetails.setLoanAccountNumber(loanAccountNumber);

    ResponseRpsDTO responseRpsDTO = new ResponseRpsDTO();
    byte[] pdfBytes = new byte[] {1, 2, 3, 4};

    Mockito.<Mono<ClientDetailsCpResponseDto>>when(
            (Mono<ClientDetailsCpResponseDto>)
                losApi.getCpRpsLeadData(eq(leadId), eq(loanAccountNumber)))
        .thenReturn(Mono.just(clientDetails));
    Mockito.<Mono<ResponseRpsDTO>>when(lmsApi.fetchRPS(Mockito.eq(loanAccountNumber)))
        .thenReturn(Mono.just(responseRpsDTO));
    Mockito.when(rpsPdfBuilder.generatePdf(responseRpsDTO, clientDetails, loanAccountNumber))
        .thenReturn(pdfBytes);

    Mono<byte[]> pdfMono = customerService.getRPS(loanAccountNumber, leadId);

    StepVerifier.create(pdfMono)
        .expectNextMatches(bytes -> bytes.length == pdfBytes.length)
        .verifyComplete();
  }

  private Method validateRequestMethod;

  @BeforeEach
  void setUp() throws Exception {
    validateRequestMethod =
        CustomerService.class.getDeclaredMethod(
            "validateRequest", SubmitFormRequest.class, LeadDetailsDTO.class);
    validateRequestMethod.setAccessible(true);
  }

  @Test
  void testValidateRequest() throws Exception {
    SubmitFormRequest request = new SubmitFormRequest();
    LeadDetailsDTO leadDetails = new LeadDetailsDTO();

    // Common valid data
    request.setEmail("test@example.com");
    leadDetails.setEmail("Test@Example.com");

    request.setRegisteredMobileNumber("+919876543210");
    leadDetails.setMobileNo("9876543210");

    request.setPanCard("ABCDE1234F");
    leadDetails.setPanNumber("abcde1234f");

    request.setLoanId("loan123");
    leadDetails.setLoanAccounts(Arrays.asList("loan123", "loan456"));

    // Test valid input: should return Mono.empty
    Mono<Void> result =
        (Mono<Void>) validateRequestMethod.invoke(customerService, request, leadDetails);
    StepVerifier.create(result).verifyComplete();

    // Email mismatch
    request.setEmail("mismatch@example.com");
    Mono<Void> emailMismatch =
        (Mono<Void>) validateRequestMethod.invoke(customerService, request, leadDetails);
    StepVerifier.create(emailMismatch).expectErrorMessage("Email mismatch").verify();
    request.setEmail("test@example.com"); // revert

    // Mobile number mismatch
    request.setRegisteredMobileNumber("+911234567890");
    Mono<Void> mobileMismatch =
        (Mono<Void>) validateRequestMethod.invoke(customerService, request, leadDetails);
    StepVerifier.create(mobileMismatch).expectErrorMessage("Mobile number mismatch").verify();
    request.setRegisteredMobileNumber("+919876543210"); // revert

    // PAN mismatch
    request.setPanCard("WRONGPAN");
    Mono<Void> panMismatch =
        (Mono<Void>) validateRequestMethod.invoke(customerService, request, leadDetails);
    StepVerifier.create(panMismatch).expectErrorMessage("PAN mismatch").verify();
    request.setPanCard("ABCDE1234F"); // revert

    // Loan ID not found in leadAccounts
    request.setLoanId("loan999");
    Mono<Void> loanIdNotFound =
        (Mono<Void>) validateRequestMethod.invoke(customerService, request, leadDetails);
    StepVerifier.create(loanIdNotFound)
        .expectErrorMessage("Loan ID not found for this client")
        .verify();

    // Loan ID required for client (lead has loan accounts but request loanId is empty)
    request.setLoanId("");
    Mono<Void> loanIdRequired =
        (Mono<Void>) validateRequestMethod.invoke(customerService, request, leadDetails);
    StepVerifier.create(loanIdRequired)
        .expectErrorMessage("Loan ID is required for this client")
        .verify();

    // Lead has no loan accounts and request loanId is empty - should be valid
    request.setLoanId("");
    leadDetails.setLoanAccounts(Collections.emptyList());
    Mono<Void> validNoLoanAccounts =
        (Mono<Void>) validateRequestMethod.invoke(customerService, request, leadDetails);
    StepVerifier.create(validNoLoanAccounts).verifyComplete();
  }
}
