package com.trillionloans.customer_portal.configuration.filter;

import static com.trillionloans.customer_portal.constant.StringConstants.REQUEST_LOG;

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
public class RequestMethodLogFilter implements WebFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest originalRequest = exchange.getRequest();
    log.info(
        "[{}] details: {} {}", REQUEST_LOG, originalRequest.getMethod(), originalRequest.getPath());
    return chain.filter(exchange);
  }
}
