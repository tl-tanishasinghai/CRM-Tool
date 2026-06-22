package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.model.request.LoanReject;
import com.trillionloans.los.model.request.RiskOperationTableUpdateRequest;
import com.trillionloans.los.model.response.m2p.ExperianBureauCtaUpdateResponse;
import com.trillionloans.los.model.response.m2p.M2PApplicationTypeFundlyDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanRejectResponseDTO;
import com.trillionloans.los.model.response.m2p.RequestedLoanDetailsResponse;
import com.trillionloans.los.service.M2pService;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/los/api/v1")
@AllArgsConstructor
@RestController
@Validated
public class M2pController {

  private final M2pService m2pService;

  @GetMapping("/fetch-bureau/{bureauId}")
  public Mono<String> fetchBureauData(
      @PathVariable String bureauId, @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.fetchBureauData(bureauId);
  }

  @GetMapping("/fetch-crif/{loanId}")
  public Mono<Object> callCrif(
      @PathVariable String loanId, @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.callCrif(loanId);
  }

  @PostMapping(value = "/bre-datatable/{loanId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Object> updateBreDataToM2P(
      @RequestBody Object breDatatableDTO,
      @PathVariable @NonNull String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.updateBreDataToM2P(breDatatableDTO, loanId);
  }

  @GetMapping("/get-loan/{loanId}")
  public Mono<?> getLoanApplicationByLoanId(
      @PathVariable String loanId, @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.getLoanApplicationByLoanId(loanId);
  }

  @PostMapping(value = "/register-cta/{identifier}")
  public Mono<?> registerCta(
      @RequestBody String ctaName,
      @PathVariable @NonNull String identifier,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.registerCta(identifier, ctaName);
  }

  @PutMapping("/reject-loan/{loanId}")
  public Mono<M2pLoanRejectResponseDTO> rejectLoanApplication(
      @RequestBody LoanReject rejectionData,
      @PathVariable @NonNull String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.rejectLoanApplication(rejectionData, loanId);
  }

  @GetMapping("/client-loan-performance/{clientId}/{loanId}")
  public Mono<Object> getClientLoanPerformanceReport(
      @PathVariable String clientId,
      @PathVariable String loanId,
      @RequestParam String reportApi,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.getClientLoanPerformanceReport(clientId, reportApi, loanId);
  }

  @PostMapping(value = "/bre-bureau-report/{leadId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ExperianBureauCtaUpdateResponse> updateExperianReportData(
      @RequestBody Object bureauData,
      @PathVariable @NonNull String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.updateExperianReportData(bureauData, leadId);
  }

  @PostMapping(
      value = "/aa-identity-match-result-datatable/{loanId}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Object> updateAAIdentityMatchResultDataToM2P(
      @RequestBody Object aaIdentityMatchResultDatatableDTO,
      @PathVariable @NonNull String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.updateAAIdentityMatchResultDataToM2P(
        aaIdentityMatchResultDatatableDTO, loanId);
  }

  @GetMapping("/loan-details/{loanApplicationId}")
  public Mono<RequestedLoanDetailsResponse> getRequestedLoanDetails(
      @PathVariable String loanApplicationId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.getRequestedLoanDetails(loanApplicationId, productCode);
  }

  @PutMapping(
      value = "/risk-operation/{loanApplicationId}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Object> updateRiskOperation(
      @PathVariable String loanApplicationId,
      @RequestBody RiskOperationTableUpdateRequest riskOperationTableUpdateRequest) {
    return m2pService.updateRiskOperationTable(riskOperationTableUpdateRequest, loanApplicationId);
  }

  @GetMapping("/application-type-scf/{loanApplicationId}")
  public Mono<M2PApplicationTypeFundlyDTO> getApplicationTypeForFundly(
      @PathVariable String loanApplicationId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.getApplicationTypeForFundly(loanApplicationId);
  }

  @GetMapping("/drawdown-performance-data/{accountNumber}")
  public Mono<Object> getDrawdownPerformanceReport(
      @PathVariable String accountNumber,
      @RequestParam String reportApi,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return m2pService.getDrawdownPerformanceReport(accountNumber, reportApi);
  }
}
