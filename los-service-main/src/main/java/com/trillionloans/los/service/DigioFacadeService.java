package com.trillionloans.los.service;

import com.trillionloans.los.model.dto.DigioMandateWebhookDTO;
import com.trillionloans.los.model.response.digio.MandateLiveBanksDigioResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface DigioFacadeService {
  /**
   * Registers the KYC status callback from Digio.
   *
   * @param digioMandateWebhook the request body containing the KYC status details
   * @return a response indicating the result of the registration process
   */
  Mono<String> registerMandateWebhook(DigioMandateWebhookDTO digioMandateWebhook);

  Flux<MandateLiveBanksDigioResponse> fetchDigioMandateLiveBanks(String productCode);
}
