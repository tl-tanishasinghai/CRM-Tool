package com.trillionloans.lms.config;

import static com.trillionloans.lms.constant.StringConstants.ACTIVE;
import static com.trillionloans.lms.constant.StringConstants.PARTNER_ID;

import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.service.db.PartnerMasterService;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filter interface implementation class for intercepting the requests and check for partnerId value
 * correctness. Logic checks for correctness of availability and correctness of partnerId and swaps
 * the header value with productCode as it is required for running the flow
 */
@Slf4j
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class PartnerFilterConfiguration implements WebFilter {

  private final PartnerMasterService partnerMasterService;

  public PartnerFilterConfiguration(PartnerMasterService partnerMasterService) {
    this.partnerMasterService = partnerMasterService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (isActuatorOrProbeRequest(exchange)) {
      return chain.filter(exchange);
    }
    log.info("[{}] header fields: {}", "REQUEST_HEADERS", exchange.getRequest().getHeaders());
    String partnerId = exchange.getRequest().getHeaders().getFirst("partnerId");

    if (partnerId == null || partnerId.isEmpty() || partnerId.isBlank()) {
      partnerId = "1001";
    }
    exchange.getAttributes().put(PARTNER_ID, partnerId);
    String errorMessage =
        "{\"status\":\"FAIL\",\"message\":\"client error\",\"data\":\"please provide"
            + " correct value for header - partnerId\"}";
    if (!Objects.isNull(partnerId)) {
      return partnerMasterService
          .findByPartnerIdAndStatus(partnerId, ACTIVE)
          .flatMap(
              entity -> {
                ServerHttpRequest.Builder builder = exchange.getRequest().mutate();
                builder.header("productCode", entity.getProductCode());
                builder.header("partnerId", entity.getPartnerId());
                return chain.filter(exchange);
              })
          .onErrorResume(
              error -> {
                if (error instanceof BaseException) {

                  byte[] errorMessageBytes = errorMessage.getBytes(StandardCharsets.UTF_8);
                  DataBuffer buffer =
                      exchange.getResponse().bufferFactory().wrap(errorMessageBytes);
                  exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                  exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                  return exchange.getResponse().writeWith(Mono.just(buffer));
                }
                return Mono.error(error);
              });
    }
    return chain.filter(exchange);
  }

  private boolean isActuatorOrProbeRequest(ServerWebExchange exchange) {
    String path = exchange.getRequest().getPath().toString();
    String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
    return path.contains("/actuator/") || (userAgent != null && userAgent.contains("kube-probe"));
  }
}
