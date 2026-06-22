package com.trillionloans.los.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.PartnerApi;
import com.trillionloans.los.model.request.PartnerPostPutRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class PartnerService {
  private final PartnerApi partnerApi;
  private final CreditLineService creditLineService;
  private final ObjectMapper objectMapper;

  public Mono<Object> registerBreCallback(PartnerPostPutRequest partnerPostPutRequest) {
    return partnerApi.registerBreCallback(
        partnerPostPutRequest.getRequestBody(),
        partnerPostPutRequest.getUri(),
        partnerPostPutRequest.getCallMethod(),
        partnerPostPutRequest.getPartnerCode(),
        partnerPostPutRequest.getRetryCount());
  }
}
