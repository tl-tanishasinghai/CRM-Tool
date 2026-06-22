package com.trillionloans.lms.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.model.entity.PartnerMasterEntity;
import com.trillionloans.lms.service.db.PartnerMasterService;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
class PartnerFilterConfigurationTest {

  @Mock PartnerMasterService partnerMasterService;
  @Mock ServerWebExchange exchange;
  @Mock WebFilterChain chain;
  @Mock ServerHttpRequest request;
  @Mock HttpHeaders headers;
  @Mock ServerHttpResponse response;
  @Mock DataBuffer dataBuffer;
  @Mock ServerHttpRequest.Builder requestBuilder;

  PartnerFilterConfiguration filterConfig;

  static final String PARTNER_ID_HEADER = "partnerId";
  static final String PRODUCT_CODE_HEADER = "productCode";
  static final String ACTIVE = "A";

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    filterConfig = new PartnerFilterConfiguration(partnerMasterService);

    when(exchange.getRequest()).thenReturn(request);
    when(exchange.getResponse()).thenReturn(response);
    when(request.getHeaders()).thenReturn(headers);
    when(exchange.getAttributes()).thenReturn(new HashMap<>());
    when(response.bufferFactory()).thenReturn(new DefaultDataBufferFactory());

    // Mock mutate builder chain
    when(request.mutate()).thenReturn(requestBuilder);
    when(requestBuilder.header(anyString(), anyString())).thenReturn(requestBuilder);
    when(requestBuilder.build()).thenReturn(request);
  }

  @Test
  void filter_withValidPartnerId_callsServiceAndChain() {
    String partnerId = "42";
    when(headers.getFirst(PARTNER_ID_HEADER)).thenReturn(partnerId);

    PartnerMasterEntity entity =
        PartnerMasterEntity.builder().partnerId(partnerId).productCode("prodCode").build();

    when(partnerMasterService.findByPartnerIdAndStatus(partnerId, ACTIVE))
        .thenReturn(Mono.just(entity));
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    Mono<Void> result = filterConfig.filter(exchange, chain);

    StepVerifier.create(result).verifyComplete();

    verify(request).mutate();
    verify(chain).filter(exchange);
    verify(partnerMasterService).findByPartnerIdAndStatus(partnerId, ACTIVE);
    assertEquals(partnerId, exchange.getAttributes().get("partnerId"));
  }

  @Test
  void filter_withMissingPartnerId_defaultsTo1001() {
    when(headers.getFirst(PARTNER_ID_HEADER)).thenReturn(null);

    PartnerMasterEntity entity =
        PartnerMasterEntity.builder().partnerId("1001").productCode("defaultProd").build();

    when(partnerMasterService.findByPartnerIdAndStatus("1001", ACTIVE))
        .thenReturn(Mono.just(entity));
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    Mono<Void> result = filterConfig.filter(exchange, chain);

    StepVerifier.create(result).verifyComplete();
    assertEquals("1001", exchange.getAttributes().get("partnerId"));
    verify(partnerMasterService).findByPartnerIdAndStatus("1001", ACTIVE);
    verify(chain).filter(exchange);
  }

  @Test
  void filter_whenServiceThrowsBaseException_returnsBadRequestWithErrorJson() {
    String partnerId = "42";
    when(headers.getFirst(PARTNER_ID_HEADER)).thenReturn(partnerId);

    when(partnerMasterService.findByPartnerIdAndStatus(partnerId, ACTIVE))
        .thenReturn(Mono.error(new BaseException("error", null, HttpStatus.BAD_REQUEST)));

    DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    when(response.bufferFactory()).thenReturn(bufferFactory);

    HttpHeaders mockedHeaders = mock(HttpHeaders.class);
    when(response.getHeaders()).thenReturn(mockedHeaders);
    when(response.writeWith(any())).thenReturn(Mono.empty());

    Mono<Void> result = filterConfig.filter(exchange, chain);

    StepVerifier.create(result).verifyComplete();
    verify(response).setStatusCode(HttpStatus.BAD_REQUEST);
    verify(mockedHeaders).setContentType(MediaType.APPLICATION_JSON);
    verify(response).writeWith(any());
  }

  @Test
  void filter_whenServiceThrowsOtherException_propagatesError() {
    when(headers.getFirst(PARTNER_ID_HEADER)).thenReturn("42");

    RuntimeException ex = new RuntimeException("unexpected");
    when(partnerMasterService.findByPartnerIdAndStatus("42", ACTIVE)).thenReturn(Mono.error(ex));

    Mono<Void> result = filterConfig.filter(exchange, chain);

    StepVerifier.create(result).expectError(RuntimeException.class).verify();
  }

  @Test
  void filter_whenPartnerIdIsBlank_defaultsTo1001() {
    when(headers.getFirst(PARTNER_ID_HEADER)).thenReturn(" ");

    PartnerMasterEntity entity =
        PartnerMasterEntity.builder().partnerId("1001").productCode("defaultProd").build();

    when(partnerMasterService.findByPartnerIdAndStatus("1001", ACTIVE))
        .thenReturn(Mono.just(entity));
    when(chain.filter(exchange)).thenReturn(Mono.empty());

    Mono<Void> result = filterConfig.filter(exchange, chain);

    StepVerifier.create(result).verifyComplete();
    assertEquals("1001", exchange.getAttributes().get("partnerId"));
    verify(partnerMasterService).findByPartnerIdAndStatus("1001", ACTIVE);
    verify(chain).filter(exchange);
  }
}
