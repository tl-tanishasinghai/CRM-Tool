package com.trillionloans.lms.config.data;

import static com.trillionloans.lms.constant.StringConstants.CAFFEINE_OPS;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Caffeine cache initializer and configuration class along with removal and eviction listeners */
@Configuration
@Slf4j
public class CacheManager {
  @Bean
  Cache<String, Object> initializeCaffeine() {
    return Caffeine.newBuilder()
        .removalListener(
            (key, value, removalCause) ->
                log.info(
                    "[{}] key: {} is [removed] due to cause: {}", CAFFEINE_OPS, key, removalCause))
        .evictionListener(
            (key, value, evictionCause) ->
                log.info(
                    "[{}] key: {} is [evicted] due to cause: {}", CAFFEINE_OPS, key, evictionCause))
        .build();
  }
}
