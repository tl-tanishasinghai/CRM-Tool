package com.trillionloans.los.config.health;

import com.trillionloans.los.model.response.m2p.M2pPlatformHealthResponse;
import com.trillionloans.los.model.response.m2p.M2pPlatformHealthResponse.M2pPlatformHealthResult;
import com.trillionloans.los.service.M2pFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Slf4j
//@Configuration
public class M2pHealthConfiguration {

  @Bean("m2pHealth")
  public ReactiveHealthIndicator m2pHealthIndicator(M2pFacadeService m2pFacadeService) {
    return () -> {
      log.debug("M2P platform health check triggered");
      return m2pFacadeService
          .getM2pHealthStatus()
          .map(M2pHealthConfiguration::toHealth)
          .onErrorResume(
              ex -> {
                log.warn("Health check failed : M2P FAILED - {}", ex.getMessage());
                return Mono.just(
                    Health.down()
                        .withDetail("m2p", "M2P")
                        .withDetail("error", ex.getMessage())
                        .build());
              });
    };
  }

  private static Health toHealth(M2pPlatformHealthResponse m2pPlatformHealthResponse) {
    Health.Builder up =
        Health.up()
            .withDetail("m2p", "M2P")
            .withDetail("code", m2pPlatformHealthResponse != null ? m2pPlatformHealthResponse.getCode() : null)
            .withDetail("status", m2pPlatformHealthResponse != null ? m2pPlatformHealthResponse.getStatus() : null);
    if (m2pPlatformHealthResponse != null && m2pPlatformHealthResponse.getResult() != null) {
      M2pPlatformHealthResult result = m2pPlatformHealthResponse.getResult();
      up.withDetail("serverStatus", result.getServerStatus() != null ? result.getServerStatus() : null);
      up.withDetail("redisStatus", result.getRedisStatus() != null ? result.getRedisStatus() : null);
    }
    if (isM2pPlatformHealthy(m2pPlatformHealthResponse)) {
      return up.build();
    }
    return Health.down()
        .withDetail("m2p", "M2P")
        .withDetail("code", m2pPlatformHealthResponse != null ? m2pPlatformHealthResponse.getCode() : null)
        .withDetail("status", m2pPlatformHealthResponse != null ? m2pPlatformHealthResponse.getStatus() : null)
        .withDetail(
            "serverStatus",
                m2pPlatformHealthResponse != null && m2pPlatformHealthResponse.getResult() != null ? m2pPlatformHealthResponse.getResult().getServerStatus() : null)
        .withDetail(
            "redisStatus",
                m2pPlatformHealthResponse != null && m2pPlatformHealthResponse.getResult() != null ? m2pPlatformHealthResponse.getResult().getRedisStatus() : null)
        .withDetail("reason", "M2P server or redis status is not success")
        .build();
  }

  static boolean isM2pPlatformHealthy(M2pPlatformHealthResponse body) {
    if (body == null || body.getResult() == null) {
      return false;
    }
    M2pPlatformHealthResult result = body.getResult();
    if (!isSuccessToken(result.getServerStatus())) {
      return false;
    }
    if (!isSuccessToken(result.getRedisStatus())) {
      return false;
    }
    return true;
  }

  private static boolean isSuccessToken(String value) {
    return value != null && "success".equalsIgnoreCase(value.trim());
  }
}
