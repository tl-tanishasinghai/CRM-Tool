package com.trillionloans.los.service;

import com.trillionloans.los.model.dto.DigioMandateWebhookDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.CallbackLogEntity;
import com.trillionloans.los.model.entity.MandateRegistrationDetailsEntity;
import com.trillionloans.los.model.response.MandateRegistrationDetailsResponse;
import com.trillionloans.los.model.response.digio.MandateLiveBanksDigioResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MandateDetailsService {

  Mono<Object> processDigioCallbackWithUpdatingM2p(
      String mandateId,
      DigioMandateWebhookDTO digioMandateWebhook,
      MandateRegistrationDetailsEntity mandateRegistrationDetails,
      String productCode,
      ProductControl.Flow mandateRegistrationProductFlow,
      String partnerCode);

  Mono<MandateRegistrationDetailsResponse> processFetchMandateRegistration(
      String leadId, String loanId, String mandateId, String productCode);

  Flux<MandateLiveBanksDigioResponse> processFetchDigioMandateLiveBanks(String productCode);

  Mono<MandateRegistrationDetailsResponse> errorInUpdatingDatabase(
      String leadId, String loanId, String mandateId, Throwable error);

  void setErrorDataInCallbackEntity(CallbackLogEntity callback, Throwable error);
}
