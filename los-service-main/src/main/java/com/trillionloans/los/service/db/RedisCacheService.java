package com.trillionloans.los.service.db;

import static com.trillionloans.los.constant.StringConstants.REDIS_ALL_REMOVE_MSG;
import static com.trillionloans.los.constant.StringConstants.REDIS_REMOVE_MSG;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trillionloans.los.exception.PiiDataConverterException;
import com.trillionloans.los.repository.RedisRepositoryImpl;
import com.trillionloans.los.util.EncryptionUtil;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class RedisCacheService {
  private final EncryptionUtil encryptionUtil;
  private final RedisRepositoryImpl redisRepository;

  @Value("${redis.encryption.enabled:true}")
  private boolean piiEncryptionEnabled;

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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

  public Mono<Void> putKeyWithTTL(String key, String value, Duration duration) {
    return redisRepository.putKeyWithTTL(key, value, duration);
  }

  public Mono<Boolean> acquireLock(String lockKey, Duration ttl) {
    return redisRepository.setIfAbsent(lockKey, "LOCKED", ttl).defaultIfEmpty(false);
  }

  public Mono<Void> releaseLock(String lockKey) {
    return redisRepository.removeKey(lockKey).then();
  }

  // generic method to cache object: silent on error
  public <T> Mono<Void> cacheObjectSilently(String key, T object, long ttlSeconds) {
    return this.cacheObjectSilently(key, object, ttlSeconds, false);
  }

  public <T> Mono<Void> cacheObjectSilently(
      String key, T object, long ttlSeconds, boolean encryptionEnabled) {
    return Mono.fromCallable(() -> OBJECT_MAPPER.writeValueAsString(object))
        .map(json -> encryptionEnabled ? encryptionUtil.encryptForCache(json) : json)
        .flatMap(
            json ->
                redisRepository
                    .putKeyWithTTL(key, json, Duration.ofSeconds(ttlSeconds))
                    .doOnSuccess(v -> log.info("[REDIS_OPS] [CACHE_SUCCESS] Cached object."))
                    .doOnError(e -> log.error("[REDIS_OPS] [CACHE_ERROR] Failed to cache object.")))
        // Serialization issues
        .onErrorResume(
            JsonProcessingException.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [CACHE_FAILED_SERIALIZATION] Could not serialize object. Error={}",
                  e.getMessage());
              return Mono.empty();
            })
        // Redis-related failures
        .onErrorResume(
            RedisConnectionFailureException.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [CACHE_FAILED_REDIS_CONNECTION] Redis connection issue while caching"
                      + " object. Error={}",
                  e.getMessage());
              return Mono.empty();
            })
        .onErrorResume(
            DataAccessException.class, // covers broader Redis command/data access errors
            e -> {
              log.error(
                  "[REDIS_OPS] [CACHE_FAILED_REDIS_COMMAND] Redis command failed. Error={}",
                  e.getMessage());
              return Mono.empty();
            })
        // Catch-all for anything else
        .onErrorResume(
            Exception.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [CACHE_FAILED_UNEXPECTED] Unexpected error while caching object."
                      + " Error={}",
                  e.getMessage(),
                  e);
              return Mono.empty();
            })
        .then();
  }

  public <T> Mono<T> getObjectSilently(String key, Class<T> clazz) {
    return this.getObjectSilently(key, clazz, false);
  }

  public <T> Mono<T> getObjectSilently(String key, Class<T> clazz, boolean encryptionEnabled) {
    return redisRepository
        .getKey(key)
        .flatMap(
            raw -> {
              try {
                String json = encryptionEnabled ? encryptionUtil.decryptFromCache(raw) : raw;
                T obj = OBJECT_MAPPER.readValue(json, clazz);
                log.info("[REDIS_OPS] [FETCH_SUCCESS] Key={} found in Redis", key);
                return Mono.just(obj);
              } catch (JsonProcessingException e) {
                log.error(
                    "[REDIS_OPS] [FETCH_FAILED_DESERIALIZATION] Key={} Error={}",
                    key,
                    e.getMessage());
                return Mono.empty();
              }
            })
        .onErrorResume(
            RedisConnectionFailureException.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [FETCH_FAILED_REDIS_CONNECTION] Key={} Error={}",
                  key,
                  e.getMessage());
              return Mono.empty();
            })
        .onErrorResume(
            DataAccessException.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [FETCH_FAILED_REDIS_COMMAND] Key={} Error={}", key, e.getMessage());
              return Mono.empty();
            })
        .onErrorResume(
            Exception.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [FETCH_FAILED_UNEXPECTED] Key={} Error={}", key, e.getMessage());
              return Mono.empty();
            });
  }

  public <T> Mono<T> getEncryptedObjectSilently(String key, Class<T> clazz) {
    return redisRepository
        .getKey(key)
        .flatMap(
            cachedJson -> {
              try {
                // decrypt only if encryption is enabled
                String processedJson =
                    piiEncryptionEnabled ? encryptionUtil.decrypt(cachedJson) : cachedJson;
                // deserialize into object
                T obj = OBJECT_MAPPER.readValue(processedJson, clazz);

                log.info("[REDIS_OPS] [FETCH_SUCCESS] Key={} retrieved successfully", key);
                return Mono.just(obj);
              } catch (JsonProcessingException e) {
                log.error(
                    "[REDIS_OPS] [FETCH_FAILED_DESERIALIZATION] Key={} Error={}",
                    key,
                    e.getMessage());
                return Mono.empty();
              } catch (PiiDataConverterException e) {
                log.error(
                    "[REDIS_OPS] [FETCH_FAILED_DECRYPTION] Key={} Error={}", key, e.getMessage());
                return Mono.empty();
              } catch (Exception e) {
                log.error(
                    "[REDIS_OPS] [FETCH_FAILED_UNEXPECTED] Key={} Error={}", key, e.getMessage());
                return Mono.empty();
              }
            })
        .onErrorResume(
            RedisConnectionFailureException.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [FETCH_FAILED_REDIS_CONNECTION] Key={} Error={}",
                  key,
                  e.getMessage());
              return Mono.empty();
            })
        .onErrorResume(
            DataAccessException.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [FETCH_FAILED_REDIS_COMMAND] Key={} Error={}", key, e.getMessage());
              return Mono.empty();
            })
        .onErrorResume(
            Exception.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [FETCH_FAILED_UNEXPECTED] Key={} Error={}", key, e.getMessage());
              return Mono.empty();
            });
  }

  public <T> Mono<Void> cacheEncryptedObjectSilently(String key, T object, long ttlSeconds) {
    return Mono.fromCallable(() -> OBJECT_MAPPER.writeValueAsString(object))
        .map(json -> piiEncryptionEnabled ? encryptionUtil.encrypt(json) : json)
        .flatMap(
            processedJson ->
                redisRepository
                    .putKeyWithTTL(key, processedJson, Duration.ofSeconds(ttlSeconds))
                    .doOnSuccess(
                        v -> log.info("[REDIS_OPS] [CACHE_SUCCESS] Cached object for key={}", key))
                    .doOnError(
                        e ->
                            log.error(
                                "[REDIS_OPS] [CACHE_ERROR] Failed to cache object for key={}",
                                key,
                                e)))
        .onErrorResume(
            JsonProcessingException.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [CACHE_FAILED_SERIALIZATION] Key={} Error={}", key, e.getMessage());
              return Mono.empty();
            })
        .onErrorResume(
            RedisConnectionFailureException.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [CACHE_FAILED_REDIS_CONNECTION] Key={} Error={}",
                  key,
                  e.getMessage());
              return Mono.empty();
            })
        .onErrorResume(
            DataAccessException.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [CACHE_FAILED_REDIS_COMMAND] Key={} Error={}", key, e.getMessage());
              return Mono.empty();
            })
        .onErrorResume(
            Exception.class,
            e -> {
              log.error(
                  "[REDIS_OPS] [CACHE_FAILED_UNEXPECTED] Key={} Error={}", key, e.getMessage());
              return Mono.empty();
            })
        .then();
  }
}
