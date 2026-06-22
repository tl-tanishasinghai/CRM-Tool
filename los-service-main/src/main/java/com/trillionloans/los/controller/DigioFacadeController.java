package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.model.dto.DigioMandateWebhookDTO;
import com.trillionloans.los.model.response.digio.MandateLiveBanksDigioResponse;
import com.trillionloans.los.service.DigioFacadeService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/partners/api/v1")
@RestController
@Hidden
@AllArgsConstructor
@Validated
public class DigioFacadeController {

  private final DigioFacadeService digioFacadeService;

  /**
   * Registers the Digio Webhook for mandate.
   *
   * @param digioMandateWebhook The request body containing the KYC status details.
   * @return A Mono wrapping a ResponseEntity containing another Mono that holds the result of the
   *     operation.
   */
  @PostMapping("/callback/digio/mandate")
  public Mono<ResponseEntity<Mono<String>>> registerMandateWebhook(
      @SecureInput @RequestBody DigioMandateWebhookDTO digioMandateWebhook) {
    return Mono.just(
        ResponseEntity.ok(digioFacadeService.registerMandateWebhook(digioMandateWebhook)));
  }

  @GetMapping("/digio/live/banks")
  public Mono<ResponseEntity<Flux<MandateLiveBanksDigioResponse>>> fetchDigioMandateLiveBanks(
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(digioFacadeService.fetchDigioMandateLiveBanks(productCode)));
  }
}
