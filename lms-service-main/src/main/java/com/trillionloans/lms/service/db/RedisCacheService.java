package com.trillionloans.lms.service.db;

import static com.trillionloans.lms.constant.StringConstants.REDIS_ALL_REMOVE_MSG;
import static com.trillionloans.lms.constant.StringConstants.REDIS_REMOVE_MSG;

import com.trillionloans.lms.repository.RedisRepositoryImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class RedisCacheService {
  private final RedisRepositoryImpl redisRepository;

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
}
