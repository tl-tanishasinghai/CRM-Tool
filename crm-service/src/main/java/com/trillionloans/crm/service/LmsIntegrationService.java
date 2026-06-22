package com.trillionloans.crm.service;

import com.trillionloans.crm.integration.lms.LmsDueDetailsDto;
import com.trillionloans.crm.integration.lms.LmsForeclosureDto;
import com.trillionloans.crm.integration.lms.LmsLoanDetailsDto;
import com.trillionloans.crm.integration.lms.LmsRpsWithDpdDto;
import com.trillionloans.crm.integration.lms.LmsTransactionDetailDto;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LmsIntegrationService {

  private static final Logger log = LoggerFactory.getLogger(LmsIntegrationService.class);
  private static final DateTimeFormatter DUE_DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");

  private final RestClient lmsClient;

  public LmsIntegrationService(ExternalDataService externalDataService) {
    this.lmsClient = externalDataService.lmsClient();
  }

  public List<LmsLoanDetailsDto> fetchLoansByLeadId(String leadId) {
    try {
      List<LmsLoanDetailsDto> loans =
          lmsClient
              .get()
              .uri("/partners/api/v1/collection/{leadId}/loan/details", leadId)
              .headers(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
              .retrieve()
              .body(new ParameterizedTypeReference<List<LmsLoanDetailsDto>>() {});
      return loans == null ? List.of() : loans;
    } catch (Exception ex) {
      log.warn("LMS loan list failed for leadId={}: {}", leadId, ex.getMessage());
      return List.of();
    }
  }

  public Optional<LmsRpsWithDpdDto> fetchRpsWithDpd(String loanAccountNumber) {
    try {
      return Optional.ofNullable(
          lmsClient
              .get()
              .uri(
                  "/partners/api/v1/collection/loanAccounts/{loanAccountNumber}/repayment-schedule-with-dpd",
                  loanAccountNumber)
              .headers(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
              .retrieve()
              .body(LmsRpsWithDpdDto.class));
    } catch (Exception ex) {
      log.warn("LMS RPS+DPD failed for {}: {}", loanAccountNumber, ex.getMessage());
      return Optional.empty();
    }
  }

  public Optional<LmsDueDetailsDto> fetchDueAsOnDate(String loanAccountNumber, LocalDate date) {
    try {
      return Optional.ofNullable(
          lmsClient
              .get()
              .uri(
                  "/partners/api/v1/collection/loanAccounts/{loanAccountNumber}/fetch-due/as-on-date/{date}",
                  loanAccountNumber,
                  date.format(DUE_DATE))
              .headers(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
              .retrieve()
              .body(LmsDueDetailsDto.class));
    } catch (Exception ex) {
      log.warn("LMS due fetch failed for {}: {}", loanAccountNumber, ex.getMessage());
      return Optional.empty();
    }
  }

  public Optional<LmsTransactionDetailDto> fetchTransactions(String loanAccountNumber) {
    try {
      return Optional.ofNullable(
          lmsClient
              .get()
              .uri(
                  "/partners/api/v1/collection/loan/{loanAccountNumber}/transaction-details",
                  loanAccountNumber)
              .headers(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
              .retrieve()
              .body(LmsTransactionDetailDto.class));
    } catch (Exception ex) {
      log.warn("LMS transactions failed for {}: {}", loanAccountNumber, ex.getMessage());
      return Optional.empty();
    }
  }

  public Optional<LmsForeclosureDto> fetchForeclosure(String loanAccountNumber, LocalDate asOnDate) {
    try {
      return Optional.ofNullable(
          lmsClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path(
                              "/partners/api/v1/collection/loanAccounts/{loanAccountNumber}/foreclosure-details")
                          .queryParam("transactionDate", asOnDate.format(DUE_DATE))
                          .queryParam("isTotalOutstandingInterest", false)
                          .queryParam("includePreClosureReason", false)
                          .build(loanAccountNumber))
              .headers(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
              .retrieve()
              .body(LmsForeclosureDto.class));
    } catch (Exception ex) {
      log.warn("LMS foreclosure failed for {}: {}", loanAccountNumber, ex.getMessage());
      return Optional.empty();
    }
  }
}
