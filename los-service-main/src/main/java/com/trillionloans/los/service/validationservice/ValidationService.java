package com.trillionloans.los.service.validationservice;

import reactor.core.publisher.Mono;

public interface ValidationService<VendorRequest, EvaluatedResult, VendorResponse> {

  String getVendorName();

  Mono<EvaluatedResult> init(
      Mono<VendorRequest> request, String productCode, String clientId, String loanApplicationId);

  Mono<EvaluatedResult> readThroughCache(
      Mono<VendorRequest> request, String productCode, String clientId);

  Mono<?> writeThroughCache(
      Mono<VendorRequest> vendorRequest,
      Mono<VendorResponse> vendorResponse,
      Mono<EvaluatedResult> evaluatedResult,
      String clientID,
      String productCode);

  Mono<EvaluatedResult> evaluateResult(Mono<VendorResponse> response, String productCode);

  Mono<?> persist(
      Mono<VendorRequest> vendorRequest,
      Mono<VendorResponse> vendorResponse,
      Mono<EvaluatedResult> evaluatedResult,
      String clientID,
      String productCode);
}
