package com.trillionloans.customer_portal.api;

import static com.trillionloans.customer_portal.constant.StringConstants.DEFAULT_ERROR_MESSAGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.trillionloans.customer_portal.exception.ClientSideException;
import com.trillionloans.customer_portal.exception.DownstreamServiceException;
import com.trillionloans.customer_portal.exception.ForbiddenException;
import com.trillionloans.customer_portal.exception.ServerErrorException;
import com.trillionloans.customer_portal.model.internal.WebClientParameters;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for {@link WebClientFactoryImpl} using {@link MockWebServer}. Validates GET, POST, and
 * Flux interactions, including success cases, 4xx/5xx errors, malformed responses, retries, and
 * connection failures.
 */
class WebClientFactoryImplTest {
  Environment env;
  private MockWebServer server;
  private WebClientFactoryImpl webClientFactory;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    env = Mockito.mock(Environment.class);
    setMockEnvProperties(env);

    webClientFactory =
        new WebClientFactoryImpl(server.url("/").toString(), "TestPartner", env, Object.class);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  /** Verifies that a successful GET request returns the expected response. */
  @Test
  void testGetData_success() {
    String jsonResponse = new Gson().toJson(Map.of("message", "ok"));
    server.enqueue(
        new MockResponse()
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200));

    Mono<Map> result =
        webClientFactory.getData("/test", new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result)
        .expectNextMatches(map -> "ok".equals(map.get("message")))
        .verifyComplete();
  }

  /** Tests that a GET request receiving a 400 Bad Request triggers a ClientSideException. */
  @Test
  void testGetData_4xxClientError() {
    server.enqueue(
        new MockResponse()
            .setBody("{\"error\":\"Bad Request\"}")
            .setResponseCode(400)
            .addHeader("Content-Type", "application/json"));

    Mono<?> result =
        webClientFactory.getData("/bad-request", new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result).expectError(ClientSideException.class).verify();
  }

  /** Tests that a GET request receiving a 403 Forbidden triggers a ForbiddenException. */
  @Test
  void testGetData_4xxForbidden() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(403)
            .setBody("{\"status\": \"403\", \"message\": \"Forbidden\"}")
            .addHeader("Content-Type", "application/json"));

    Mono<?> result =
        webClientFactory.getData("/unauthorized", new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result).expectError(ForbiddenException.class).verify();
  }

  /**
   * Tests that a GET request receiving a 500 Internal Server Error triggers a ServerErrorException.
   */
  @Test
  void testGetData_5xxServerError() {
    server.enqueue(
        new MockResponse()
            .setBody("{\"error\":\"Internal Error\"}")
            .setResponseCode(500)
            .addHeader("Content-Type", "application/json"));

    Mono<?> result =
        webClientFactory.getData("/server-error", new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result).expectError(ServerErrorException.class).verify();
  }

  /** Tests that a POST request receiving a 400 Bad Request triggers a ClientSideException. */
  @Test
  void testPost_4xxError() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setBody("{\"message\":\"Bad Request\"}")
            .addHeader("Content-Type", "application/json"));

    Mono<?> result =
        webClientFactory.post(
            "/bad-post", Map.of("key", "value"), new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result).expectError(ClientSideException.class).verify();
  }

  /** Tests that a POST request receiving a 502 Bad Respone triggers a ServerErrorException. */
  @Test
  void testPost_5xxError() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(502)
            .setBody("{\"message\": \"Gateway Error\"}")
            .addHeader("Content-Type", "application/json"));

    Mono<?> responseMono =
        webClientFactory.post(
            "/server-error-post",
            Map.of("key", "value"),
            new HttpHeaders(),
            Map.class,
            defaultParams());

    StepVerifier.create(responseMono).expectError(ServerErrorException.class).verify();
  }

  /**
   * Tests that a POST request receiving a malformed JSON throws a DecodingException ||
   * JsonParseException || JsonSyntaxException.
   */
  @Test
  void testPost_malformedJson_shouldThrowUnexpectedServiceException() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("not-a-json") // malformed JSON
            .addHeader("Content-Type", "application/json"));

    Mono<?> result =
        webClientFactory.post(
            "/malformed", Map.of("key", "value"), new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof DecodingException
                    || throwable instanceof JsonParseException
                    || throwable instanceof JsonSyntaxException)
        .verify();
  }

  /** Verifies that a successful GET FLUX request returns the expected response. */
  @Test
  void testGetFluxData_success() {
    String json = new Gson().toJson(Map.of("id", 1));
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("[" + json + "]")
            .addHeader("Content-Type", "application/json"));

    StepVerifier.create(
            webClientFactory.getFluxData("/flux", new HttpHeaders(), Map.class, defaultParams()))
        .expectNextMatches(map -> ((Number) map.get("id")).intValue() == 1)
        .verifyComplete();
  }

  /** Verifies that a 400 Bad Request on getFluxData triggers ClientSideException. */
  @Test
  void testGetFluxData_4xx_shouldThrowClientSideException() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":\"Invalid Request\"}")
            .addHeader("Content-Type", "application/json"));

    Flux<?> result =
        webClientFactory.getFluxData("/flux-bad", new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result).expectError(ClientSideException.class).verify();
  }

  /** Verifies that a 500 error on getFluxData triggers ServerErrorException. */
  @Test
  void testGetFluxData_5xx_shouldThrowServerErrorException() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .setBody("{\"error\":\"Internal Error\"}")
            .addHeader("Content-Type", "application/json"));

    Flux<Map> result =
        webClientFactory.getFluxData("/flux-error", new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result).expectError(ServerErrorException.class).verify();
  }

  /**
   * Verifies that malformed JSON array on getFluxData throws DecodingException ||
   * JsonParseException || JsonSyntaxException.
   */
  @Test
  void testGetFluxData_malformedJson_shouldThrowJsonSyntaxException() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("[{invalid json}")
            .addHeader("Content-Type", "application/json"));

    Flux<?> result =
        webClientFactory.getFluxData(
            "/flux-malformed", new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof DecodingException
                    || throwable instanceof JsonParseException
                    || throwable instanceof JsonSyntaxException)
        .verify();
  }

  /** Verifies that an empty array response completes without errors for getFluxData. */
  @Test
  void testGetFluxData_emptyArray_shouldReturnEmptyFlux() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("[]")
            .addHeader("Content-Type", "application/json"));

    Flux<?> result =
        webClientFactory.getFluxData("/flux-empty", new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result).expectComplete().verify();
  }

  /** Tests POST retry logic success and failure based on retry count. */
  @Test
  void testPost_retryLogic_successThenFailureBasedOnRetryCount() {
    // -- First case: retryCount = 2 → success on 3rd attempt
    WebClientParameters paramsSuccess = defaultParams();
    paramsSuccess.setRetryCount(2);

    // First 2 fail, 3rd succeeds
    server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"message\":\"error\"}"));
    server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"message\":\"error\"}"));
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("{\"x\":\"y\"}")
            .addHeader("Content-Type", "application/json"));

    Mono<Map> successResult =
        webClientFactory.post(
            "/retry-success", Map.of("x", "y"), new HttpHeaders(), Map.class, paramsSuccess);

    StepVerifier.create(successResult)
        .expectNextMatches(map -> "y".equals(map.get("x")))
        .verifyComplete();

    // -- Second case: retryCount = 1 → 2 total attempts → still fails
    WebClientParameters paramsFail = defaultParams();
    paramsFail.setRetryCount(1);

    server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"message\":\"error\"}"));
    server.enqueue(new MockResponse().setResponseCode(500).setBody("{\"message\":\"error\"}"));

    Mono<Map> failResult =
        webClientFactory.post(
            "/retry-fail", Map.of("x", "y"), new HttpHeaders(), Map.class, paramsFail);

    StepVerifier.create(failResult).expectError(ServerErrorException.class).verify();
  }

  /**
   * Simulates server unavailability and checks that DownstreamServiceException is thrown for all
   * API types (getData, post, getFluxData).
   */
  @Test
  void connectionRefused_shouldThrowDownstreamServiceException() throws IOException {
    // Shutdown server to simulate connection refused
    server.shutdown();

    // Define 3 test paths: getData, post, and getFluxData
    List<Mono<?>> testMonos =
        List.of(
            webClientFactory.getData("/unreachable", new HttpHeaders(), Map.class, defaultParams()),
            webClientFactory.post(
                "/unreachable",
                Map.of("key", "value"),
                new HttpHeaders(),
                Map.class,
                defaultParams()),
            webClientFactory
                .getFluxData("/unreachable", new HttpHeaders(), Map.class, defaultParams())
                .collectList());

    for (Mono<?> mono : testMonos) {
      StepVerifier.create(mono)
          .expectErrorSatisfies(
              error -> {
                assertThat(error)
                    .isInstanceOf(DownstreamServiceException.class)
                    .hasMessageContaining(DEFAULT_ERROR_MESSAGE);
              })
          .verify();
    }
  }

  @Test
  void testConnectionTimeout_shouldThrowDownstreamServiceException() {
    setMockEnvProperties(env);

    // Use unroutable IP to simulate connection timeout
    WebClientFactoryImpl factory =
        new WebClientFactoryImpl("http://10.255.255.1", "TestPartner", env, Map.class);

    StepVerifier.create(factory.getFluxData("/flux", new HttpHeaders(), Map.class, defaultParams()))
        .expectErrorMatches(
            t ->
                t instanceof DownstreamServiceException
                    && t.getCause() instanceof WebClientRequestException)
        .verify();
  }

  private WebClientParameters defaultParams() {
    return WebClientParameters.builder()
        .partnerName("test-partner")
        .requestLogRequired(true)
        .loggerHeader("TRACE-ID-123")
        .responseLogRequired(true)
        .retryCount(0)
        .build();
  }

  @Test
  void testPostMultipart_success() {
    String jsonResponse = new Gson().toJson(Map.of("message", "ok"));
    server.enqueue(
        new MockResponse()
            .setBody(jsonResponse)
            .addHeader("Content-Type", "application/json")
            .setResponseCode(200));

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("email", "test@example.com");

    Mono<Map> result =
        webClientFactory.postMultipart(
            "/multipart-success", bodyBuilder, new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result)
        .expectNextMatches(map -> "ok".equals(map.get("message")))
        .verifyComplete();
  }

  @Test
  void testPostMultipart_4xx_shouldThrowClientSideException() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(400)
            .setBody("{\"error\":\"Bad Request\"}")
            .addHeader("Content-Type", "application/json"));

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("email", "test@example.com");

    Mono<Map> result =
        webClientFactory.postMultipart(
            "/multipart-bad-request", bodyBuilder, new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result).expectError(ClientSideException.class).verify();
  }

  @Test
  void testPostMultipart_5xx_shouldThrowServerErrorException() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .setBody("{\"error\":\"Internal Error\"}")
            .addHeader("Content-Type", "application/json"));

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("email", "test@example.com");

    Mono<Map> result =
        webClientFactory.postMultipart(
            "/multipart-server-error", bodyBuilder, new HttpHeaders(), Map.class, defaultParams());

    StepVerifier.create(result).expectError(ServerErrorException.class).verify();
  }

  @Test
  void testPostMultipart_malformedJson_shouldThrowDownstreamServiceException() {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("not-a-json")
            .addHeader("Content-Type", "application/json"));

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("email", "test@example.com");

    Mono<Map> result =
        webClientFactory.postMultipart(
            "/multipart-malformed-json",
            bodyBuilder,
            new HttpHeaders(),
            Map.class,
            defaultParams());

    StepVerifier.create(result)
        .expectErrorMatches(
            throwable ->
                throwable instanceof DownstreamServiceException
                    && throwable.getMessage().contains("Invalid response"))
        .verify();
  }

  @Test
  void testPostMultipart_retryLogic_successThenFailureBasedOnRetryCount() {
    WebClientParameters paramsSuccess = defaultParams();
    paramsSuccess.setRetryCount(1);

    // First 500
    server.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .setBody("{\"message\":\"error\"}")
            .addHeader("Content-Type", "application/json"));

    // Retry 200
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("{\"x\":\"y\"}")
            .addHeader("Content-Type", "application/json"));

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("email", "test@example.com");

    Mono<Map> result =
        webClientFactory.postMultipart(
            "/multipart-retry-success", bodyBuilder, new HttpHeaders(), Map.class, paramsSuccess);

    StepVerifier.create(result).expectNextMatches(map -> "y".equals(map.get("x"))).verifyComplete();

    // Now — test failure case when retries exhausted
    WebClientParameters paramsFail = defaultParams();
    paramsFail.setRetryCount(1);

    server.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .setBody("{\"message\":\"error\"}")
            .addHeader("Content-Type", "application/json"));

    server.enqueue(
        new MockResponse()
            .setResponseCode(500)
            .setBody("{\"message\":\"error\"}")
            .addHeader("Content-Type", "application/json"));

    Mono<Map> failResult =
        webClientFactory.postMultipart(
            "/multipart-retry-fail", bodyBuilder, new HttpHeaders(), Map.class, paramsFail);

    StepVerifier.create(failResult).expectError(ServerErrorException.class).verify();
  }

  private void setMockEnvProperties(Environment env) {
    Mockito.when(env.getProperty("web-client.max-idle-time")).thenReturn("60");
    Mockito.when(env.getProperty("web-client.max-life-time")).thenReturn("60");
    Mockito.when(env.getProperty("web-client.pending-acquire-timeout")).thenReturn("60");
    Mockito.when(env.getProperty("web-client.evict-background")).thenReturn("60");
    Mockito.when(env.getProperty("web-client.retry-backoff")).thenReturn("100");
  }
}
