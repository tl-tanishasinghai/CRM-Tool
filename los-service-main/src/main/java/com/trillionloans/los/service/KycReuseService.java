package com.trillionloans.los.service;

import com.trillionloans.los.model.dto.KycClientDetails;
import com.trillionloans.los.model.request.ConsentRequest;
import com.trillionloans.los.model.response.ResponseDTO;
import reactor.core.publisher.Mono;

public interface KycReuseService {
  Mono<KycClientDetails> getKycClientDetailsFromAaadhar(String clientId);

  public Mono<ResponseDTO<Object>> registerKycReuseConsent(
      String productCode, String clientId, String loanId, ConsentRequest consentRequest);
}
