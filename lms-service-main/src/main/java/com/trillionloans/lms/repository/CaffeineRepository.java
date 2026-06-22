package com.trillionloans.lms.repository;

import com.github.benmanes.caffeine.cache.Cache;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@AllArgsConstructor
public class CaffeineRepository {
  private final Cache<String, Object> cache;

  public Mono<Object> getKey(String key) {
    return Mono.justOrEmpty(cache.getIfPresent(key));
  }

  public void putKey(String key, Object value) {
    cache.put(key, value);
  }

  public void removeKey(String key) {
    cache.invalidate(key);
  }

  public void removeAll() {
    cache.invalidateAll();
  }
}
