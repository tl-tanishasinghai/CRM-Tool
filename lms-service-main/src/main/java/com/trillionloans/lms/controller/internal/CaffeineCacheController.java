package com.trillionloans.lms.controller.internal;

import static com.trillionloans.lms.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.lms.service.db.CacheService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/internal/cache")
@Hidden
@AllArgsConstructor
@RestController
public class CaffeineCacheController {
  private final CacheService cacheService;

  @PostMapping("/clear/{productCode}")
  public Mono<ResponseEntity<Mono<String>>> removeKey(
      @PathVariable(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(cacheService.removeKey(productCode)));
  }

  @PostMapping("/clear/all")
  public Mono<ResponseEntity<Mono<String>>> removeAll() {
    return Mono.just(ResponseEntity.ok(cacheService.removeAll()));
  }
}
