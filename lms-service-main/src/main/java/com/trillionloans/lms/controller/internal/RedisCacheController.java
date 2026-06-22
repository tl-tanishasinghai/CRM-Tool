package com.trillionloans.lms.controller.internal;

import com.trillionloans.lms.service.db.RedisCacheService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/internal/redis")
@Hidden
@AllArgsConstructor
@RestController
public class RedisCacheController {
  private final RedisCacheService redisCacheService;

  @PostMapping("/clear-key/{key}")
  public Mono<ResponseEntity<Mono<String>>> removeKey(@PathVariable(name = "key") String key) {
    return Mono.just(ResponseEntity.ok(redisCacheService.removeKey(key)));
  }

  @PostMapping("/clear/all")
  public Mono<ResponseEntity<Mono<String>>> removeAll() {
    return Mono.just(ResponseEntity.ok(redisCacheService.removeAll()));
  }
}
