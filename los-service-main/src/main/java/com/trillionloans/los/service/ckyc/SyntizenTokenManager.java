package com.trillionloans.los.service.ckyc;

import static java.util.Objects.requireNonNull;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.response.ckyc.SyntizenAuthResponse;
import com.trillionloans.los.service.db.RedisCacheService;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.WebClientUtil;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class SyntizenTokenManager {

  private static final String TOKEN_CACHE_KEY = "SYNTIZEN_AUTH:TOKEN";
  private static final String TOKEN_LOCK_KEY = "SYNTIZEN_AUTH:TOKEN:LOCK";
  private static final Duration LOCK_TTL = Duration.ofSeconds(30);
  private static final int MAX_WAIT_ATTEMPTS = 15;
  private static final Duration WAIT_INTERVAL = Duration.ofMillis(200);

  private final WebClientUtil util;
  private final Environment environment;
  private final WebClientFactory webClientFactory;
  private final RedisCacheService redisCacheService;
  private final String syntizenKey;
  private final String syntizenUsername;
  private final String syntizenPassword;

  public SyntizenTokenManager(
      @Value("${syntizen.api.baseUrl}") String baseUrl,
      @Value("${syntizen.api.apikey}") String syntizenKey,
      @Value("${syntizen.auth.username}") String syntizenUsername,
      @Value("${syntizen.auth.password}") String syntizenPassword,
      @Value("${syntizen.api.disable-ssl-verification:true}") boolean disableSslVerification,
      Environment environment,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService,
      RedisCacheService redisCacheService) {

    this.syntizenKey = syntizenKey;
    this.syntizenUsername = syntizenUsername;
    this.syntizenPassword = syntizenPassword;
    this.redisCacheService = redisCacheService;
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl,
            "Syntizen",
            environment,
            kafkaLoggingService,
            kafkaEventProducerService,
            disableSslVerification);
    this.environment = environment;
    this.util = new WebClientUtil();
  }

  private HttpHeaders getSyntizenAuthHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(CONTENT_TYPE, "application/json");
    headers.add("apikey", syntizenKey);
    return headers;
  }

  public Mono<SyntizenAuthResponse> generateAuthToken() {
    Map<String, String> authTokenBody = new HashMap<>();
    authTokenBody.put("username", syntizenUsername);
    authTokenBody.put("password", syntizenPassword);
    String uri =
        UriComponentsBuilder.fromUriString(
                requireNonNull(environment.getProperty("syntizen.api.authentication")))
            .toUriString();
    WebClientParameters webClientParameters =
        util.getWebClientParameters(null, "SYNTIZEN_TOKEN", 3, false, false, null);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        authTokenBody,
        getSyntizenAuthHeaders(),
        SyntizenAuthResponse.class,
        webClientParameters);
  }

  public Mono<String> getValidToken() {
    return redisCacheService
        .getKey(TOKEN_CACHE_KEY)
        .doOnNext(token -> log.info("[SYNTIZEN_TOKEN] cache hit - using existing token"))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info("[SYNTIZEN_TOKEN] cache miss - refreshing token");
                  return refreshTokenWithLock();
                }));
  }

  public Mono<Void> invalidateAndRefresh() {
    log.info("[SYNTIZEN_TOKEN] token invalidated - triggering refresh");
    return redisCacheService.releaseLock(TOKEN_CACHE_KEY).then(refreshTokenWithLock()).then();
  }

  private Mono<String> refreshTokenWithLock() {
    return redisCacheService
        .acquireLock(TOKEN_LOCK_KEY, LOCK_TTL)
        .flatMap(
            acquired -> {
              if (Boolean.TRUE.equals(acquired)) {
                log.info("[SYNTIZEN_TOKEN] lock acquired - refreshing token from vendor");
                return generateAuthToken()
                    .flatMap(this::cacheToken)
                    .doFinally(
                        signal -> {
                          log.debug("[SYNTIZEN_TOKEN] releasing lock");
                          redisCacheService.releaseLock(TOKEN_LOCK_KEY).subscribe();
                        });
              } else {
                log.info("[SYNTIZEN_TOKEN] lock held by another pod - waiting for token");
                return waitForToken(MAX_WAIT_ATTEMPTS);
              }
            });
  }

  private Mono<String> waitForToken(int attemptsLeft) {
    if (attemptsLeft <= 0) {
      log.error(
          "[SYNTIZEN_TOKEN] timeout waiting for token refresh after {} attempts",
          MAX_WAIT_ATTEMPTS);
      return Mono.error(new IllegalStateException("timeout waiting for Syntizen token refresh"));
    }

    return Mono.delay(WAIT_INTERVAL)
        .then(redisCacheService.getKey(TOKEN_CACHE_KEY))
        .doOnNext(token -> log.debug("[SYNTIZEN_TOKEN] token received from redis after waiting"))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.debug(
                      "[SYNTIZEN_TOKEN] token not ready, {} attempts remaining", attemptsLeft - 1);
                  return waitForToken(attemptsLeft - 1);
                }));
  }

  private Mono<String> cacheToken(SyntizenAuthResponse response) {
    if (response.getAuthkey() == null || response.getAuthkey().isBlank()) {
      return Mono.error(new IllegalStateException("syntizen auth returned empty authKey"));
    }

    long expiryMinutes;
    try {
      expiryMinutes = Long.parseLong(response.getAuthkeyexpriry());
    } catch (NumberFormatException e) {
      return Mono.error(
          new IllegalStateException(
              "invalid authKey expiry value: " + response.getAuthkeyexpriry(), e));
    }

    log.info("[SYNTIZEN_TOKEN] caching new token with TTL of {} minutes", expiryMinutes);

    return redisCacheService
        .putKeyWithTTL(TOKEN_CACHE_KEY, response.getAuthkey(), Duration.ofMinutes(expiryMinutes))
        .thenReturn(response.getAuthkey());
  }
}
