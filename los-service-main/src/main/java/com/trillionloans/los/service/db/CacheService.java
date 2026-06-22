package com.trillionloans.los.service.db;

import static com.trillionloans.los.constant.StringConstants.CAFFEINE_ALL_REMOVE_MSG;
import static com.trillionloans.los.constant.StringConstants.CAFFEINE_REMOVE_MSG;

import com.trillionloans.los.repository.CaffeineRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class CacheService {
  private final CaffeineRepository caffeineRepository;

  public Mono<String> removeKey(String productCode) {
    caffeineRepository.removeKey(productCode);
    return Mono.just(CAFFEINE_REMOVE_MSG);
  }

  public <T> Mono<T> getKey(String key, Class<T> tClass) {
    return caffeineRepository.getKey(key).cast(tClass);
  }

  public void putKey(String key, Object value) {
    caffeineRepository.putKey(key, value);
  }

  public Mono<String> removeAll() {
    caffeineRepository.removeAll();
    return Mono.just(CAFFEINE_ALL_REMOVE_MSG);
  }
}
