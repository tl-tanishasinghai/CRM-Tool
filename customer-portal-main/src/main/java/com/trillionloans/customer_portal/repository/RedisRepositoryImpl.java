package com.trillionloans.customer_portal.repository;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Slf4j
@Repository
public class RedisRepositoryImpl {
  private final ReactiveRedisOperations<String, String> redisOperations;
  private final String environmentName;
  private final String REDIS_CUSTOMER_PORTAL_REDIS_PREFIX = ":CP";

  @Autowired
  public RedisRepositoryImpl(
      ReactiveRedisOperations<String, String> redisOperations,
      @Value("${environment.name}") String environmentName) {
    this.redisOperations = redisOperations;
    this.environmentName = environmentName;
  }

  public Mono<String> getKey(String key) {
    return redisOperations
        .opsForValue()
        .get(buildKey(key))
        .doOnNext(value -> log.info("[REDIS][CACHE_HIT] for key {}", key))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info("[REDIS][CACHE_MISS] for key {}", key);
                  return Mono.empty();
                }))
        .onErrorResume(
            e -> {
              log.error("[REDIS][ERROR] Failed to get key {}: {}", key, e.getMessage(), e);
              return Mono.error(e);
            });
  }

  public Mono<Boolean> putKey(String key, String value, Duration ttl) {
    return redisOperations
        .opsForValue()
        .set(environmentName + ":" + "CP" + ":" + key, value, ttl)
        .doOnNext(
            success ->
                log.info(
                    "[REDIS] {} put value for key {}",
                    success ? "Successfully" : "Failure to",
                    key))
        .onErrorResume(
            e -> {
              log.error("[REDIS][ERROR] Failed to put key {}: {}", key, e.getMessage(), e);
              return Mono.error(e);
            });
  }

  private String buildKey(String key) {
    return environmentName + REDIS_CUSTOMER_PORTAL_REDIS_PREFIX + ":" + key;
  }

  public Mono<Void> putKey(String key, String value) {
    return redisOperations.opsForValue().set(buildKey(key), value).then();
  }

  public Mono<Void> putKeyWithTTL(String key, String value, Duration ttl) {
    return redisOperations.opsForValue().set(buildKey(key), value, ttl).then();
  }

  public Mono<Void> removeKey(String key) {
    return redisOperations.opsForValue().delete(buildKey(key)).then();
  }

  public Mono<Void> removeAll() {
    return redisOperations
        .keys(environmentName + ":*")
        .flatMap(redisOperations.opsForValue()::delete)
        .then();
  }
}
