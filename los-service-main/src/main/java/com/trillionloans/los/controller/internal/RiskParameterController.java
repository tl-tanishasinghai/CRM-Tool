package com.trillionloans.los.controller.internal;

import com.trillionloans.los.service.RiskParameterService;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@AllArgsConstructor
@RequestMapping("/internal/v1/risk-parameter")
public class RiskParameterController {
  private final RiskParameterService riskParameterService;

  @GetMapping("/metrics")
  public Mono<ResponseEntity<Mono<Map<String, Double>>>> getAllPortfolioMetrics() {
    return Mono.just(ResponseEntity.ok(riskParameterService.fetchAllAndWarmCache()));
  }

  @PostMapping("/initialize")
  public Mono<ResponseEntity<String>> triggerHistoricalCalculation() {

    riskParameterService.initializePortfolio().subscribeOn(Schedulers.boundedElastic()).subscribe();

    return Mono.just(
        ResponseEntity.accepted()
            .body("Historical portfolio calculation job started successfully."));
  }
}
