package com.trillionloans.los.repository;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class RedisRepositoryImpl {

  private final ReactiveRedisOperations<String, String> redisOperations;
  private final String environmentName;

  @Autowired
  public RedisRepositoryImpl(
      ReactiveRedisOperations<String, String> redisOperations,
      @Value("${environment.name}") String environmentName) {
    this.redisOperations = redisOperations;
    this.environmentName = environmentName;
  }

  private String buildKey(String key) {
    return environmentName + ":" + key;
  }

  public Mono<String> getKey(String key) {
    return redisOperations.opsForValue().get(buildKey(key));
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

  public Mono<Boolean> setIfAbsent(String key, String value, Duration ttl) {
    return redisOperations.opsForValue().setIfAbsent(buildKey(key), value, ttl);
  }
}
