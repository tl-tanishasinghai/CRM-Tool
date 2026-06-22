package com.trillionloans.customer_portal.configuration.filter;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * A WebFilter that adds standard HTTP security headers to all responses:
 *
 * <p>1. Protects against clickjacking by preventing the site from being embedded in frames. 2.
 * Limits content sources to self-origin, mitigating XSS and data injection attacks (Content
 * Security Policy). 3. Enables XSS protection in supported browsers, blocking rendering if an
 * attack is detected. 4. Disables MIME-type sniffing to prevent content-type misinterpretation. 5.
 * Enforces HTTPS by instructing browsers to only access the site over HTTPS for one year (HSTS). 6.
 * Prevents caching of sensitive data by instructing browsers not to store responses. 7. Adds legacy
 * cache-control headers for compatibility with older browsers.
 */
@Order
@Configuration
public class SecurityHeadersFilter implements WebFilter {

  // Header names
  private static final String HEADER_X_FRAME_OPTIONS = "X-Frame-Options";
  private static final String HEADER_CONTENT_SECURITY_POLICY = "Content-Security-Policy";
  private static final String HEADER_X_XSS_PROTECTION = "X-XSS-Protection";
  private static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
  private static final String HEADER_STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";
  private static final String HEADER_PRAGMA = "Pragma";

  // Header values
  private static final List<String> VALUE_X_FRAME_OPTIONS_DENY = List.of("DENY");
  private static final List<String> VALUE_CONTENT_SECURITY_POLICY =
      List.of(
          "default-src 'self'; "
              + "script-src 'self'; "
              + "frame-ancestors 'none'; "
              + "object-src 'none'; "
              + "form-action 'self';");
  private static final List<String> VALUE_X_XSS_PROTECTION = List.of("1; mode=block");
  private static final List<String> VALUE_X_CONTENT_TYPE_OPTIONS = List.of("nosniff");
  private static final List<String> VALUE_STRICT_TRANSPORT_SECURITY =
      List.of("max-age=31536000; includeSubDomains");
  private static final List<String> VALUE_CACHE_CONTROL_NO_STORE = List.of("no-store");
  private static final List<String> VALUE_PRAGMA_NO_CACHE = List.of("no-cache");

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    if (!exchange.getResponse().isCommitted()) {
      addSecurityHeaders(exchange.getResponse().getHeaders());
    }

    return chain
        .filter(exchange)
        .doOnSuccess(
            success -> {
              if (!exchange.getResponse().isCommitted()) {
                addSecurityHeaders(exchange.getResponse().getHeaders());
              }
            });
  }

  private void addSecurityHeaders(HttpHeaders headers) {
    headers.putIfAbsent(HEADER_X_FRAME_OPTIONS, VALUE_X_FRAME_OPTIONS_DENY);
    headers.putIfAbsent(HEADER_CONTENT_SECURITY_POLICY, VALUE_CONTENT_SECURITY_POLICY);
    headers.putIfAbsent(HEADER_X_XSS_PROTECTION, VALUE_X_XSS_PROTECTION);
    headers.putIfAbsent(HEADER_X_CONTENT_TYPE_OPTIONS, VALUE_X_CONTENT_TYPE_OPTIONS);
    headers.putIfAbsent(HEADER_STRICT_TRANSPORT_SECURITY, VALUE_STRICT_TRANSPORT_SECURITY);
    headers.putIfAbsent(HttpHeaders.CACHE_CONTROL, VALUE_CACHE_CONTROL_NO_STORE);
    headers.putIfAbsent(HEADER_PRAGMA, VALUE_PRAGMA_NO_CACHE);
  }
}
