package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.CALLBACK_RETRY;
import static com.trillionloans.los.constant.StringConstants.DISB_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.E_SIGN_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.KYC_CALLBACK_IDENTIFIER;

import com.google.gson.Gson;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.model.dto.internal.CallBackLog;
import com.trillionloans.los.model.request.CallbackSearchCriteria;
import com.trillionloans.los.model.request.m2p.M2pDisbursementCallBackRequest;
import com.trillionloans.los.model.request.m2p.M2pKycCallBackWithAmlRequest;
import com.trillionloans.los.service.db.CallbackStoreService;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Service for retrying callback operations based on stored callback logs. */
@Service
@Slf4j
@AllArgsConstructor
public class CallbackRetryService {

  private final CallbackStoreService callbackStoreService; // Service to manage callback logs
  private final M2pFacadeService m2pFacadeService; // Facade service for M2P operations
  private final Gson gson; // Gson instance for JSON serialization/deserialization

  /**
   * Retries callbacks based on the given search criteria.
   *
   * @param criteria the criteria to search for callback logs
   * @return a Mono containing a list of result messages from the retry operations
   */
  public Mono<List<String>> retryCallbacks(CallbackSearchCriteria criteria) {
    if (checkCriteria(criteria)) {
      return Mono.error(
          new ClientSideException(
              "at-least one parameters is required for fetching callback logs",
              "please provide minimum 1 parameter for fetching callback logs",
              HttpStatus.BAD_REQUEST));
    }
    return callbackStoreService
        .findAllByCriteria(criteria) // fetch callback logs based on criteria
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.info(
                        "[{}] no callback logs found for criteria",
                        CALLBACK_RETRY))) // log if no logs found
        .flatMap(this::processCallback) // process each callback log
        .collectList(); // collect results into a list
  }

  private boolean checkCriteria(CallbackSearchCriteria criteria) {
    return (criteria.getProductCodes() == null || criteria.getProductCodes().isEmpty())
        && (criteria.getTypes() == null || criteria.getTypes().isEmpty())
        && (criteria.getIds() == null || criteria.getIds().isEmpty())
        && (criteria.getStartDate() == null || criteria.getStartDate().isEmpty())
        && (criteria.getEndDate() == null || criteria.getEndDate().isEmpty())
        && (criteria.getReferenceIds() == null || criteria.getReferenceIds().isEmpty())
        && (criteria.getExceptionCheck() == null);
  }

  /**
   * Processes a single callback log and triggers the appropriate retry operation.
   *
   * @param callback the callback log to process
   * @return a Mono containing the result message of the operation
   */
  private Mono<String> processCallback(CallBackLog callback) {
    if (Objects.isNull(callback.getReferenceId()) || callback.getReferenceId().isEmpty()) {
      return Mono.just("refId - null/empty - failed - " + callback.getType());
    }
    if (!List.of(DISB_CALLBACK_IDENTIFIER, KYC_CALLBACK_IDENTIFIER, E_SIGN_CALLBACK_IDENTIFIER)
        .contains(callback.getType())) {
      return Mono.just(callback.getReferenceId() + " - not valid - failed - " + callback.getType());
    }
    if (Objects.isNull(callback.getRequest())) {
      return Mono.just(
          callback.getReferenceId() + " - request null - failed - " + callback.getType());
    }
    if (Objects.isNull(callback.getProductCode())) {
      return Mono.just(
          callback.getReferenceId() + " - product code null - failed - " + callback.getType());
    }

    // logging the retrying callback details
    log.info(
        "[{}] retrying callback {} with id: {}",
        CALLBACK_RETRY,
        callback.getType(),
        callback.getReferenceId());

    // clean and parse the JSON string from the callback request
    String cleanJsonString = cleanJsonString(callback.getRequest().toString());

    // check callback type and process accordingly
    if (Objects.equals(callback.getType(), DISB_CALLBACK_IDENTIFIER)) {
      M2pDisbursementCallBackRequest cleanedJson =
          gson.fromJson(cleanJsonString, M2pDisbursementCallBackRequest.class);
      return m2pFacadeService
          .triggerProductControlFlow(
              cleanedJson, cleanedJson.getProductCode(), DISB_CALLBACK_IDENTIFIER, true)
          .flatMap(
              data ->
                  Mono.just(
                      callback.getReferenceId()
                          + " - success - "
                          + DISB_CALLBACK_IDENTIFIER)) // return success message
          .onErrorResume(
              error ->
                  Mono.just(
                      callback.getReferenceId()
                          + " - failed - "
                          + error.getMessage()
                          + " - "
                          + DISB_CALLBACK_IDENTIFIER)); // handle errors
    }

    M2pKycCallBackWithAmlRequest cleanedJson =
        gson.fromJson(cleanJsonString, M2pKycCallBackWithAmlRequest.class);
    return m2pFacadeService
        .triggerProductControlFlow(
            cleanedJson, cleanedJson.getProductCode(), callback.getType(), true)
        .flatMap(
            data ->
                Mono.just(
                    callback.getReferenceId()
                        + " - success - "
                        + callback.getType())) // return success message
        .onErrorResume(
            error ->
                Mono.just(
                    callback.getReferenceId()
                        + " - failed - "
                        + error.getMessage()
                        + " - "
                        + callback.getType()));
  }

  /**
   * Cleans the JSON string by removing unnecessary prefixes and suffixes.
   *
   * @param jsonString the JSON string to clean
   * @return the cleaned JSON string
   */
  private String cleanJsonString(String jsonString) {
    String cleanedJsonString = jsonString;
    if (cleanedJsonString.startsWith("JsonByteArrayInput{")) {
      cleanedJsonString = cleanedJsonString.substring("JsonByteArrayInput{".length());
    }
    if (cleanedJsonString.endsWith("}}")) {
      cleanedJsonString = cleanedJsonString.substring(0, cleanedJsonString.length() - 1);
    }
    return cleanedJsonString.trim(); // trim whitespace
  }
}
