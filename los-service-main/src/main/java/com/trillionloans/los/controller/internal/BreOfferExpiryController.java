package com.trillionloans.los.controller.internal;

import com.trillionloans.los.service.LoanOfferExpiryService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequestMapping("/partners/api/v2/loan")
@RestController
@Hidden
@AllArgsConstructor
@Validated
public class BreOfferExpiryController {
  private final LoanOfferExpiryService loanOfferExpiryService;

  @PostMapping("/loan-offer-expiry")
  @Operation(summary = "BRE Offer Expiry for expired loans based on product-specific tenure")
  public Mono<ResponseEntity<String>> triggerLoanExpiryJob() {
    Mono<Void> jobMono = loanOfferExpiryService.runExpiryJob();

    Mono<ResponseEntity<String>> responseMono =
        Mono.just(ResponseEntity.accepted().body("Accepted for processing."));

    return responseMono.doOnSuccess(
        response -> {
          jobMono.subscribeOn(Schedulers.boundedElastic()).subscribe();
        });
  }
}
