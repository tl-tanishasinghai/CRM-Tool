package com.trillionloans.customer_portal.controller;

import com.trillionloans.customer_portal.configuration.ProtectedPath;
import com.trillionloans.customer_portal.model.dto.CollectionStatusResponse;
import com.trillionloans.customer_portal.model.dto.CreatePaymentRequest;
import com.trillionloans.customer_portal.model.dto.CreatePaymentResponse;
import com.trillionloans.customer_portal.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/payments/api/v1/")
@AllArgsConstructor
@RestController
@Validated
public class PaymentController {

  private final PaymentService paymentService;

  @ProtectedPath
  @PostMapping(
    value = "customer/{clientId}/loan-account/{loanAccountNumber}/create-payment",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<CreatePaymentResponse>>> createPayment(
    @Valid @RequestBody CreatePaymentRequest createPaymentRequest,
    @PathVariable(name = "loanAccountNumber")
    @Size(max = 100, message = "[CreatePayment] loanAccountNumber exceeds max length of 100")
    String loanAccountNumber,
    @PathVariable(name = "clientId")
    @Size(max = 100, message = "[CreatePayment] clientId exceeds max length of 100")
    String clientId) {
    return Mono.just(
      ResponseEntity.ok(
        paymentService.createPayment(clientId, loanAccountNumber, createPaymentRequest)));
  }


  @ProtectedPath
  @GetMapping("loan-account/{loanAccountNumber}/collection-status/{collectionId}")
  public Mono<ResponseEntity<Mono<CollectionStatusResponse>>> getCollectionStatus(
    @PathVariable(name = "loanAccountNumber")
    @Size(
      max = 100,
      message = "[GetCollectionStatus] loanAccountNumber exceeds max length of 100")
    String loanAccountNumber,
    @PathVariable(name = "collectionId")
    @Size(max = 100, message = "[GetCollectionStatus] collectionId exceeds max length of 100")
    @Pattern(
      regexp = "^\\d+$",
      message = "[GetCollectionStatus] collectionId must be a valid number")
    String collectionId) {
    return Mono.just(
      ResponseEntity.ok(paymentService.getCollectionStatus(loanAccountNumber, collectionId)));
  }
}