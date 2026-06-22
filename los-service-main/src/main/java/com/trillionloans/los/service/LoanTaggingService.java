package com.trillionloans.los.service;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.entity.LoanDetails;
import com.trillionloans.los.model.entity.MasterDocument;
import com.trillionloans.los.model.response.m2p.M2pDocumentTagDTO;
import com.trillionloans.los.repository.LoanTagRepository;
import com.trillionloans.los.repository.MasterTagConfigRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanTaggingService {

  private final MasterTagConfigRepository configRepository;
  private final LoanTagRepository loanTagRepository;
  private final M2PWrapperApi m2PWrapperApi;

  private static final String TAG_VALUE_KEY = "tagValue"; // Key for lookup

  /**
   * Performs the PSL tag check by treating the M2P response as a generic List of Maps, comparing
   * the 'tagValue' field directly against configured PSL tags.
   *
   * @param loanApplicationId The ID of the loan application.
   * @return Mono<Void> indicating completion.
   */
  public Mono<Void> tagLoanForPsl(String loanApplicationId) {

    return isPslEligible(loanApplicationId)
        .filter(Boolean::booleanValue)
        .flatMap(
            eligible ->
                loanTagRepository
                    .countByLoanApplicationId(loanApplicationId)
                    .filter(count -> count == 0)
                    .map(count -> createPslLoanTag(loanApplicationId))
                    .flatMap(loanTagRepository::save))
        .then()
        .onErrorResume(
            e -> {
              log.info("Error tagging loan " + loanApplicationId + ": " + e.getMessage());
              return Mono.empty();
            });
  }

  public Mono<Boolean> isPslEligible(String loanApplicationId) {

    return configRepository
        .findAllByIsPslTrueAndIsActiveTrue()
        .map(MasterDocument::getDocumentTag)
        .collect(Collectors.toSet())
        .flatMap(
            pslTags -> {
              log.info(
                  "Checking PSL eligibility for loanId={}, PSL tags={}",
                  loanApplicationId,
                  pslTags);

              Mono<Boolean> clientDocumentMatchMono =
                  m2PWrapperApi
                      .getLoanApplicationByLoanIdV2(loanApplicationId, null)
                      .doOnNext(
                          resp ->
                              log.debug(
                                  "Fetched loan application for loanId={}, clientId={}",
                                  loanApplicationId,
                                  resp.getClientId()))
                      .map(resp -> resp.getClientId())
                      .filter(Objects::nonNull)
                      .map(Object::toString)
                      .switchIfEmpty(
                          Mono.defer(
                              () -> {
                                log.info("ClientId not found for loanId={}", loanApplicationId);
                                return Mono.empty();
                              }))
                      .flatMap(
                          clientId ->
                              m2PWrapperApi
                                  .getLatestBusinessProofDocumentByClientId(clientId)
                                  .doOnNext(
                                      dto ->
                                          log.debug(
                                              "Fetched client business proof doc for clientId={},"
                                                  + " tag={}",
                                              clientId,
                                              dto.getTagName()))
                                  .map(M2pDocumentTagDTO::getTagName)
                                  .filter(Objects::nonNull)
                                  .map(
                                      tag -> {
                                        boolean match = pslTags.contains(tag);
                                        if (match) {
                                          log.info(
                                              "PSL matched via CLIENT document. loanId={}, tag={}",
                                              loanApplicationId,
                                              tag);
                                        }
                                        return match;
                                      })
                                  .filter(Boolean::booleanValue) // emits ONLY if true
                          );

              Mono<Boolean> loanDocumentMatchMono =
                  m2PWrapperApi
                      .getDocumentList(loanApplicationId)
                      .doOnSubscribe(
                          s ->
                              log.debug(
                                  "Checking loan-level documents for loanId={}", loanApplicationId))
                      .cast(List.class)
                      .map(list -> (List<Map<String, Object>>) list)
                      .flatMapMany(Flux::fromIterable)
                      .map(docMap -> docMap.get(TAG_VALUE_KEY))
                      .filter(String.class::isInstance)
                      .cast(String.class)
                      .doOnNext(
                          tag ->
                              log.debug(
                                  "Found loan document tag={} for loanId={}",
                                  tag,
                                  loanApplicationId))
                      .any(
                          tag -> {
                            boolean match = pslTags.contains(tag);
                            if (match) {
                              log.info(
                                  "PSL matched via LOAN document. loanId={}, tag={}",
                                  loanApplicationId,
                                  tag);
                            }
                            return match;
                          });

              // STOP EARLY: loan docs are evaluated ONLY if client doc didn't match
              return clientDocumentMatchMono
                  .switchIfEmpty(
                      loanDocumentMatchMono
                          .filter(Boolean::booleanValue)
                          .doOnSuccess(
                              v ->
                                  log.debug(
                                      "Client PSL not matched, loan PSL matched for loanId={}",
                                      loanApplicationId)))
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            log.info("PSL NOT eligible for loanId={}", loanApplicationId);
                            return Mono.just(false);
                          }));
            })
        .defaultIfEmpty(false)
        .doOnSuccess(
            result ->
                log.info("Final PSL eligibility for loanId={} => {}", loanApplicationId, result));
  }

  private LoanDetails createPslLoanTag(String loanApplicationId) {
    log.info("createPslLoanTag for loanId={}", loanApplicationId);
    return new LoanDetails(
        null,
        loanApplicationId,
        true, // is_psl = TRUE
        LocalDateTime.now(),
        LocalDateTime.now());
  }
}
