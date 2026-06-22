package com.trillionloans.lms.config;

import static com.trillionloans.lms.constant.StringConstants.REQUEST_LOG;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filter interface implementation class for logging the request metadata like request method, path,
 * etc.
 */
@Configuration
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestMethodLogConfiguration implements WebFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest originalRequest = exchange.getRequest();
    String loggerHeader = exchange.getRequest().getHeaders().getFirst("loggerheader");

    if (isActuatorOrProbeRequest(exchange)) {
      return chain.filter(exchange);
    }

    log.info(
        "[{}] details: {} {}, loggerHeader: {}",
        REQUEST_LOG,
        originalRequest.getMethod(),
        originalRequest.getPath(),
        loggerHeader);
    return chain.filter(exchange);
  }

  private boolean isActuatorOrProbeRequest(ServerWebExchange exchange) {
    String path = exchange.getRequest().getPath().toString();
    String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
    return path.contains("/actuator/") || (userAgent != null && userAgent.contains("kube-probe"));
  }
}
