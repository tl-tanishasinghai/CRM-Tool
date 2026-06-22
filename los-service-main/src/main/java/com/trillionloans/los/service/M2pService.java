package com.trillionloans.los.service;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.request.LoanReject;
import com.trillionloans.los.model.request.RiskOperationTableUpdateRequest;
import com.trillionloans.los.model.response.m2p.ExperianBureauCtaUpdateResponse;
import com.trillionloans.los.model.response.m2p.M2PApplicationTypeFundlyDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanRejectResponseDTO;
import com.trillionloans.los.model.response.m2p.RequestedLoanDetailsResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class M2pService {

  private final M2PWrapperApi m2PWrapperApi;
  private final LoanLevelClientDetailsService loanLevelClientDetailsService;

  public Mono<String> fetchBureauData(String bureauId) {
    return m2PWrapperApi.fetchBureauData(bureauId);
  }

  public Mono<Object> callCrif(String bureauId) {
    return m2PWrapperApi.callExperian(bureauId);
  }

  public Mono<Object> updateBreDataToM2P(Object breDatatableDTO, String loanId) {
    return m2PWrapperApi.updateBreDataToM2P(breDatatableDTO, loanId);
  }

  public Mono<?> getLoanApplicationByLoanId(String loanId) {
    return m2PWrapperApi.getLoanApplicationByLoanId(loanId);
  }

  public Mono<?> registerCta(String identifier, String ctaName) {
    return m2PWrapperApi.registerCta(identifier, ctaName);
  }

  public Mono<M2pLoanRejectResponseDTO> rejectLoanApplication(
      LoanReject rejectionData, String loanId) {
    return m2PWrapperApi.rejectLoanApplication(rejectionData, loanId);
  }

  public Mono<Object> getClientLoanPerformanceReport(
      String clientId, String reportApi, String loanId) {
    return m2PWrapperApi.getClientLoanPerformanceReport(clientId, reportApi, loanId);
  }

  public Mono<?> getKycUseDetails(String clientId, String loanId) {
    return m2PWrapperApi.getKycUseDetails(clientId, loanId);
  }

  public Mono<ExperianBureauCtaUpdateResponse> updateExperianReportData(
      Object bureauData, String leadId) {
    return m2PWrapperApi.updateExperianReportData(leadId, bureauData);
  }

  public Mono<Object> updateAAIdentityMatchResultDataToM2P(
      Object aaIdentityMatchResultDatatableDTO, String leadId) {
    return m2PWrapperApi.updateAAIdentityMatchResultDataToM2P(
        leadId, aaIdentityMatchResultDatatableDTO);
  }

  public Mono<RequestedLoanDetailsResponse> getRequestedLoanDetails(
      String loanApplicationId, String productCode) {
    return m2PWrapperApi
        .getRequestedLoanDetailsM2p(loanApplicationId)
        .next()
        .flatMap(
            requestedLoanDetailsResponse -> {
              String clientId = requestedLoanDetailsResponse.getClientId();
              return loanLevelClientDetailsService
                  .fetchLoanLevelClientDetails(clientId, loanApplicationId, productCode)
                  .map(
                      loanLevelClientDetails -> {
                        requestedLoanDetailsResponse.setDob(
                            loanLevelClientDetails.getDateOfBirth());
                        requestedLoanDetailsResponse.setPincode(
                            loanLevelClientDetails.getPincode());
                        return requestedLoanDetailsResponse;
                      })
                  .switchIfEmpty(Mono.just(requestedLoanDetailsResponse));
            });
  }

  public Mono<Object> updateRiskOperationTable(
      RiskOperationTableUpdateRequest riskOperationDbUpdateRequest, String loanApplicationId) {
    return m2PWrapperApi.updateRiskOperationTable(riskOperationDbUpdateRequest, loanApplicationId);
  }

  public Mono<M2PApplicationTypeFundlyDTO> getApplicationTypeForFundly(String loanApplicationId) {
    return m2PWrapperApi.getApplicationTypeForFundly(loanApplicationId);
  }

  public Mono<Object> getDrawdownPerformanceReport(String accountNumber, String reportApi) {
    return m2PWrapperApi.getDrawdownPerformanceReport(accountNumber, reportApi);
  }
}
