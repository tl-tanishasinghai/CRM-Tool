package com.trillionloans.los.service.db;

import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.model.entity.ClientMiscellaneousDetails;
import com.trillionloans.los.repository.ClientMiscellaneousDetailsRepository;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@AllArgsConstructor
@Slf4j
public class LeadMiscellaneousDetailsService {

  private final ClientMiscellaneousDetailsRepository clientMiscellaneousDetailsRepository;
  private final ObjectMapper objectMapper;

  /**
   * Saves miscellaneous details for a given client/lead asynchronously. This method fires and
   * forgets - it doesn't block the caller. MDC context is preserved across thread boundaries.
   *
   * @param clientId the client ID
   * @param miscellaneousDetails map of key-value pairs to save
   */
  public void saveMiscellaneousDetailsAsync(
      Integer clientId, Map<String, String> miscellaneousDetails) {
    if (miscellaneousDetails == null || miscellaneousDetails.isEmpty()) {
      log.info("[CLIENT_MISC_DETAILS] No miscellaneous details to save for clientId: {}", clientId);
      return;
    }

    // Capture MDC context from the current thread
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    log.info(
        "[CLIENT_MISC_DETAILS] Initiating async save of {} miscellaneous details for clientId: {}",
        miscellaneousDetails.size(),
        clientId);

    String detailsJson;
    try {
      detailsJson = objectMapper.writeValueAsString(miscellaneousDetails);
    } catch (JsonProcessingException e) {
      log.error(
          "[CLIENT_MISC_DETAILS] Failed to serialize miscellaneous details for clientId: {}, error:"
              + " {}",
          clientId,
          e.getMessage());
      return;
    }

    ClientMiscellaneousDetails clientMiscellaneousDetails =
        ClientMiscellaneousDetails.builder()
            .clientId(clientId)
            .partnerId(Integer.valueOf(MDC.get(PARTNER_ID)))
            .details(detailsJson)
            .build();

    clientMiscellaneousDetailsRepository
        .save(clientMiscellaneousDetails)
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(
            saved -> {
              setMdcContext(mdcContext);
              log.info(
                  "[CLIENT_MISC_DETAILS] Successfully saved miscellaneous details for clientId: {}",
                  clientId);
            })
        .doOnError(
            error -> {
              setMdcContext(mdcContext);
              log.error(
                  "[CLIENT_MISC_DETAILS] Error saving miscellaneous details for clientId: {},"
                      + " error: {}",
                  clientId,
                  error.getMessage());
            })
        .doFinally(signalType -> MDC.clear())
        .subscribe();
  }

  /**
   * Saves miscellaneous details for a given client/lead synchronously. Returns a Mono that
   * completes when the save is done.
   *
   * @param clientId the client ID
   * @param partnerId the partner ID
   * @param miscellaneousDetails map of key-value pairs to save
   * @return Mono<ClientMiscellaneousDetails> the saved entity, or empty if nothing to save
   */
  public Mono<ClientMiscellaneousDetails> saveMiscellaneousDetails(
      Integer clientId, Integer partnerId, Map<String, String> miscellaneousDetails) {
    if (miscellaneousDetails == null || miscellaneousDetails.isEmpty()) {
      log.info("[CLIENT_MISC_DETAILS] No miscellaneous details to save for clientId: {}", clientId);
      return Mono.empty();
    }

    log.info(
        "[CLIENT_MISC_DETAILS] Saving {} miscellaneous details for clientId: {}",
        miscellaneousDetails.size(),
        clientId);

    String detailsJson;
    try {
      detailsJson = objectMapper.writeValueAsString(miscellaneousDetails);
    } catch (JsonProcessingException e) {
      log.error(
          "[CLIENT_MISC_DETAILS] Failed to serialize miscellaneous details for clientId: {}, error:"
              + " {}",
          clientId,
          e.getMessage());
      return Mono.error(
          new RuntimeException("Failed to serialize client miscellaneous details", e));
    }

    ClientMiscellaneousDetails clientMiscellaneousDetails =
        ClientMiscellaneousDetails.builder()
            .clientId(clientId)
            .partnerId(partnerId)
            .details(detailsJson)
            .build();

    return clientMiscellaneousDetailsRepository
        .save(clientMiscellaneousDetails)
        .doOnSuccess(
            saved ->
                log.info(
                    "[CLIENT_MISC_DETAILS] Successfully saved miscellaneous details for clientId:"
                        + " {}",
                    clientId))
        .doOnError(
            error ->
                log.error(
                    "[CLIENT_MISC_DETAILS] Error saving miscellaneous details for clientId: {},"
                        + " error: {}",
                    clientId,
                    error.getMessage()));
  }

  /**
   * Sets the MDC context on the current thread.
   *
   * @param mdcContext the MDC context map to set
   */
  private void setMdcContext(Map<String, String> mdcContext) {
    if (mdcContext != null) {
      MDC.setContextMap(mdcContext);
    }
  }
}
