package com.trillionloans.los.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service for matching business names and addresses. Currently returns true for all comparisons. To
 * be integrated with actual matching API later.
 */
@Service
@Slf4j
public class NameAddressMatchService {

  public Mono<Boolean> matchBusinessName(String originalName, String documentName) {
    log.info(
        "[NAME_MATCH] Comparing business names - original: '{}', document: '{}'",
        originalName,
        documentName);
    return Mono.just(true);
  }

  public Mono<Boolean> matchBusinessAddress(String originalAddress, String documentAddress) {
    log.info(
        "[ADDRESS_MATCH] Comparing business addresses - original: '{}', document: '{}'",
        originalAddress,
        documentAddress);
    return Mono.just(true);
  }
}
