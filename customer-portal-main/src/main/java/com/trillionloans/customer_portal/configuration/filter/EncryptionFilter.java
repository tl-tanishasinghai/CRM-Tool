package com.trillionloans.customer_portal.configuration.filter;

import com.trillionloans.customer_portal.configuration.EncryptResponse;
import com.trillionloans.customer_portal.util.EncryptionUtil;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class EncryptionFilter implements WebFilter {

  private final String base64SecretKey;
  private final RequestMappingHandlerMapping handlerMapping;
  private static final Logger LOGGER = Logger.getLogger(EncryptionFilter.class.getName());

  public EncryptionFilter(
      @Value("${base64-secret-key}") String base64SecretKey,
      RequestMappingHandlerMapping handlerMapping) {
    this.base64SecretKey = base64SecretKey;
    this.handlerMapping = handlerMapping;
  }

  @NonNull
  @Override
  public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    return handlerMapping
        .getHandler(exchange)
        .flatMap(
            handler -> {
              if (handler instanceof HandlerMethod handlerMethod) {
                EncryptResponse encryptResponse =
                    handlerMethod.getMethodAnnotation(EncryptResponse.class);
                if (encryptResponse != null) {
                  ServerHttpResponse originalResponse = exchange.getResponse();
                  ServerHttpResponseDecorator decoratedResponse =
                      new ServerHttpResponseDecorator(originalResponse) {
                        @NonNull
                        @Override
                        public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                          Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                          return super.writeWith(
                              fluxBody
                                  .buffer()
                                  .map(
                                      dataBuffers -> {
                                        DataBuffer joinedDataBuffer = dataBuffers.get(0);
                                        for (int i = 1; i < dataBuffers.size(); i++) {
                                          joinedDataBuffer.write(dataBuffers.get(i).asByteBuffer());
                                        }
                                        byte[] content =
                                            new byte[joinedDataBuffer.readableByteCount()];
                                        joinedDataBuffer.read(content);
                                        DataBufferUtils.release(joinedDataBuffer);

                                        try {
                                          SecretKey secretKey =
                                              EncryptionUtil.getKeyFromString(base64SecretKey);
                                          String encryptedContent =
                                              EncryptionUtil.encrypt(
                                                  new String(content, StandardCharsets.UTF_8),
                                                  secretKey);
                                          byte[] encryptedBytes =
                                              encryptedContent.getBytes(StandardCharsets.UTF_8);
                                          DataBuffer buffer =
                                              originalResponse.bufferFactory().wrap(encryptedBytes);
                                          originalResponse
                                              .getHeaders()
                                              .setContentLength(encryptedBytes.length);
                                          originalResponse
                                              .getHeaders()
                                              .setContentType(
                                                  MediaType
                                                      .TEXT_PLAIN); // Ensure the content type is
                                          // set
                                          return buffer;
                                        } catch (Exception e) {
                                          throw new RuntimeException(
                                              "Failed to encrypt response", e);
                                        }
                                      }));
                        }
                      };
                  return chain.filter(exchange.mutate().response(decoratedResponse).build());
                }
              }
              return chain.filter(exchange);
            });
  }
}
