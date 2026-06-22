package com.trillionloans.customer_portal.service;

import com.trillionloans.customer_portal.api.internal.CollectionApi;
import com.trillionloans.customer_portal.model.dto.CollectionDetailsResponse;
import com.trillionloans.customer_portal.model.dto.CollectionStatusResponse;
import com.trillionloans.customer_portal.model.dto.CreatePaymentRequest;
import com.trillionloans.customer_portal.model.dto.CreatePaymentResponse;
import com.trillionloans.customer_portal.model.dto.LatestCollectionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class PaymentService {
  private final CollectionApi collectionApi;

  public PaymentService(CollectionApi collectionApi) {
    this.collectionApi = collectionApi;
  }

  public Mono<CreatePaymentResponse> createPayment(
    String clientId, String loanAccountNumber, CreatePaymentRequest createPaymentRequest) {
    return collectionApi.createPayment(clientId, loanAccountNumber, createPaymentRequest);
  }

  public Mono<CollectionStatusResponse> getCollectionStatus(
    String loanAccountNumber, String collectionId) {
    return collectionApi.getCollectionStatus(loanAccountNumber, collectionId);
  }

  public Mono<CollectionDetailsResponse> getCollectionDetails(
    String loanAccountNumber) {
    loanAccountNumber = loanAccountNumber.replaceAll("^0+", "");
    return collectionApi.getCollectionDetails(loanAccountNumber);
  }

  public Mono<LatestCollectionResponse> getLatestCollection(
    String loanAccountNumber) {
    loanAccountNumber = loanAccountNumber.replaceAll("^0+", "");
    return collectionApi.getLatestCollection(loanAccountNumber);
  }
}