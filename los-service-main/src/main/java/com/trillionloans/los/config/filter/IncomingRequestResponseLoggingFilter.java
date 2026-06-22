package com.trillionloans.los.config.filter;

import com.trillionloans.los.service.IncomingRequestResponseLoggingService;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filter component for logging incoming requests and responses. This filter intercepts HTTP
 * requests and responses to log their details using the IncomingRequestResponseLoggingService for
 * sending it to kafka topic.
 */
@Component
public class IncomingRequestResponseLoggingFilter implements WebFilter {
  private final IncomingRequestResponseLoggingService incomingRequestResponseLoggingService;

  /**
   * Constructor for IncomingRequestResponseLoggingFilter.
   *
   * @param incomingRequestResponseLoggingService the service used for logging
   */
  public IncomingRequestResponseLoggingFilter(
      IncomingRequestResponseLoggingService incomingRequestResponseLoggingService) {
    this.incomingRequestResponseLoggingService = incomingRequestResponseLoggingService;
  }

  /**
   * Filters incoming HTTP requests to log their details. This method is called for every incoming
   * request to the application.
   *
   * @param exchange the current ServerWebExchange (contains the request and response)
   * @param chain the WebFilterChain to pass the request through to the next filter
   * @return a Mono that indicates when the filtering is complete
   */
  @Override
  @NonNull
  public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    if (isActuatorOrProbeRequest(exchange)) {
      return chain.filter(exchange);
    }
    return incomingRequestResponseLoggingService.logIncomingRequest(exchange, chain);
  }

  private boolean isActuatorOrProbeRequest(ServerWebExchange exchange) {
    String path = exchange.getRequest().getPath().toString();
    String userAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
    return path.contains("/actuator/")
            || (userAgent != null && userAgent.contains("kube-probe"));
  }
}
