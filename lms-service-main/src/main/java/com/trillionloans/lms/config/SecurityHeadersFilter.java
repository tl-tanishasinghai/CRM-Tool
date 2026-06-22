package com.trillionloans.lms.config;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * SecurityHeadersFilter is a WebFilter that adds HTTP security headers to all server responses to
 * help mitigate various security vulnerabilities.
 */
@Configuration
@Order
public class SecurityHeadersFilter implements WebFilter {

  /**
   * Adds common security headers to the HTTP response to improve security.
   *
   * @param exchange the current server exchange
   * @param chain the web filter chain
   * @return a Mono indicating filter completion
   */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    // Pre-processing logic (e.g., add headers)
    if (!exchange.getResponse().isCommitted()) {
      appendHeaders(exchange);
    }
    // Proceed with the filter chain
    return chain
        .filter(exchange)
        .doOnSuccess(
            success -> {
              // Post-processing logic (e.g., add headers) if response not committed
              if (!exchange.getResponse().isCommitted()) {
                appendHeaders(exchange);
              }
            });
  }

  private static void appendHeaders(ServerWebExchange exchange) {
    // Prevents the application from being loaded in frames (clickjacking protection)
    exchange.getResponse().getHeaders().putIfAbsent("X-Frame-Options", List.of("DENY"));

    // Restricts content sources to only self-origin to mitigate XSS and data injection attacks
    exchange
        .getResponse()
        .getHeaders()
        .putIfAbsent(
            "Content-Security-Policy",
            List.of(
                "default-src 'self'; "
                    + "script-src 'self'; "
                    + "frame-ancestors 'self'; "
                    + "object-src 'none'; "
                    + "form-action 'self';"));

    // Enables XSS filtering in supported browsers and blocks rendering if an XSS attack is detected
    exchange.getResponse().getHeaders().putIfAbsent("X-XSS-Protection", List.of("1; mode=block"));

    // Disables MIME-type sniffing to prevent browsers from interpreting files as a different MIME
    // type
    exchange.getResponse().getHeaders().putIfAbsent("X-Content-Type-Options", List.of("nosniff"));

    // Enforces HTTPS by instructing the browser to interact with the site over HTTPS for one year
    exchange
        .getResponse()
        .getHeaders()
        .putIfAbsent("Strict-Transport-Security", List.of("max-age=31536000; includeSubDomains"));

    // Directs the browser not to cache any response data (useful for sensitive data)
    exchange.getResponse().getHeaders().putIfAbsent("Cache-Control", List.of("no-store"));

    // Legacy header to ensure older browsers also do not cache response data
    exchange.getResponse().getHeaders().putIfAbsent("Pragma", List.of("no-cache"));
  }
}
