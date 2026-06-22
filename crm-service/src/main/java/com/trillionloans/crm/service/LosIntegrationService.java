package com.trillionloans.crm.service;

import com.trillionloans.crm.integration.los.LosLeadProfileDto;
import com.trillionloans.crm.integration.los.LosLoanApplicationDto;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class LosIntegrationService {

  private static final Logger log = LoggerFactory.getLogger(LosIntegrationService.class);

  private final RestClient losClient;
  private final String productCode;

  public LosIntegrationService(ExternalDataService externalDataService) {
    this.losClient = externalDataService.losClient();
    this.productCode = externalDataService.productCode();
  }

  public Optional<LosLeadProfileDto> fetchCpProfile(String leadId) {
    try {
      return Optional.ofNullable(
          losClient
              .get()
              .uri("/partners/api/v1/lead/cp/{leadId}", leadId)
              .headers(
                  headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("productCode", productCode);
                  })
              .retrieve()
              .body(LosLeadProfileDto.class));
    } catch (Exception ex) {
      log.warn("LOS CP profile failed for leadId={}: {}", leadId, ex.getMessage());
      return Optional.empty();
    }
  }

  public List<LosLoanApplicationDto> fetchLoanApplications(String leadId) {
    try {
      List<LosLoanApplicationDto> applications =
          losClient
              .get()
              .uri("/partners/api/v1/lead/{leadId}/details", leadId)
              .headers(
                  headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("productCode", productCode);
                  })
              .retrieve()
              .body(new ParameterizedTypeReference<List<LosLoanApplicationDto>>() {});
      return applications == null ? List.of() : applications;
    } catch (Exception ex) {
      log.warn("LOS applications failed for leadId={}: {}", leadId, ex.getMessage());
      return List.of();
    }
  }
}
