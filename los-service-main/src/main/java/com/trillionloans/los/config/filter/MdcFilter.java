package com.trillionloans.los.config.filter;

import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;

import com.trillionloans.los.util.LogUtil;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

/**
 * Filter interface implementation class for setting up the traceId in Reactor context Purpose is to
 * get the traceId printed in each and every log traceId is fetched from the headers, if not found
 * then it is generated using uuid class
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcFilter implements WebFilter {
  @Override
  public Mono<Void> filter(@Nullable ServerWebExchange exchange, WebFilterChain chain) {
    if (exchange == null) {
      throw new IllegalArgumentException("ServerWebExchange must not be null");
    }
    String traceId = LogUtil.getTraceId(exchange.getRequest().getHeaders());
    String partnerId = exchange.getRequest().getHeaders().getFirst(PARTNER_ID);
    MDC.put(TRACE_ID, traceId);
    MDC.put(PARTNER_ID, partnerId);
    return chain
        .filter(exchange)
        .contextWrite(
            reactorContext ->
                reactorContext
                    .put(TRACE_ID, traceId)
                    .put(PARTNER_ID, null != partnerId ? partnerId : "1001"))
        .doFinally(signal -> MDC.clear());
  }
}
