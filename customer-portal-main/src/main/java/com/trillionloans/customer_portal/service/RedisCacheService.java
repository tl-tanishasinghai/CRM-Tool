package com.trillionloans.customer_portal.service;

import static com.trillionloans.customer_portal.constant.StringConstants.REDIS_ALL_REMOVE_MSG;
import static com.trillionloans.customer_portal.constant.StringConstants.REDIS_REMOVE_MSG;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.customer_portal.repository.RedisRepositoryImpl;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class RedisCacheService {
  private final RedisRepositoryImpl redisRepository;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public Mono<String> removeKey(String key) {
    redisRepository.removeKey(key).subscribe();
    return Mono.just(REDIS_REMOVE_MSG);
  }

  public Mono<String> removeAll() {
    redisRepository.removeAll().subscribe();
    return Mono.just(REDIS_ALL_REMOVE_MSG);
  }

  public Mono<String> getKey(String key) {
    return redisRepository.getKey(key);
  }

  public Mono<Void> putKey(String key, String value) {
    return redisRepository.putKey(key, value);
  }

  public Mono<Void> putKeyWithTTL(String key, String value, Duration ttl) {
    return redisRepository.putKeyWithTTL(key, value, ttl);
  }

  // generic method to cache object: silent on error
  public <T> Mono<Void> cacheObjectSilently(String key, T object, long ttlSeconds) {
    return Mono.fromCallable(() -> OBJECT_MAPPER.writeValueAsString(object))
        .flatMap(
            json ->
                redisRepository
                    .putKeyWithTTL(key, json, Duration.ofSeconds(ttlSeconds))
                    .doOnSuccess(v -> log.info("[REDIS_OPS] Cached object for key {}", key))
                    .doOnError(
                        e -> log.error("[REDIS_OPS] Failed to cache object for key {}", key, e)))
        .onErrorResume(
            JsonProcessingException.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [CACHE_FAILED] Failed to serialize object for key={}, error={}",
                  key,
                  e.getMessage());
              return Mono.empty();
            })
        .then();
  }
}
