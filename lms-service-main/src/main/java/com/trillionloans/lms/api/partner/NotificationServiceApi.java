package com.trillionloans.lms.api.partner;

import static com.trillionloans.lms.constant.StringConstants.LOGGER_HEADER;

import com.trillionloans.lms.api.util.WebClientFactory;
import com.trillionloans.lms.api.util.WebClientFactoryImpl;
import com.trillionloans.lms.config.WebClientTimeoutProperties;
import com.trillionloans.lms.model.dto.internal.WebClientParameters;
import com.trillionloans.lms.service.KafkaLoggingService;
import com.trillionloans.lms.util.WebClientUtil;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class NotificationServiceApi {

  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final WebClientUtil webClientUtil;
  private final WebClientTimeoutProperties webClientTimeoutProperties;

  public NotificationServiceApi(
      @Value("${notification-service.api.base-url}") String baseUrl,
      Environment environment,
      KafkaLoggingService kafkaLoggingService,
      WebClientTimeoutProperties webClientTimeoutProperties) {
    this.webClientFactory =
        new WebClientFactoryImpl(baseUrl, "NOTIFICATION_SERVICE", environment, kafkaLoggingService);
    this.environment = environment;
    this.webClientUtil = new WebClientUtil();
    this.webClientTimeoutProperties = webClientTimeoutProperties;
  }

  public Mono<Object> triggerNotification(Object requestBody) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("notification-service.api.send-sms")))
            .toUriString();
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            null, "SEND_SMS", 0, true, webClientTimeoutProperties.getSmall());
    return Mono.deferContextual(
        context -> {
          String traceIdForNotificationService =
              context.get("traceId") + "_" + UUID.randomUUID().toString().substring(0, 7);
          log.info(
              "[{}] calling notification service api with traceId: {}",
              "NOTIFICATION_SERVICE",
              traceIdForNotificationService);
          HttpHeaders headers = getNotificationServiceHeaders(traceIdForNotificationService);
          headers.add(
              LOGGER_HEADER,
              "POST " + environment.getProperty("notification-service.api.send-sms"));
          return webClientFactory.postData(
              uri, requestBody, headers, Object.class, webClientParameters);
        });
  }

  public Mono<?> triggerBulkNotification(Object requestBody) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("notification-service.api.send-bulk-sms")))
            .toUriString();
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            null, "SEND_BULK_SMS", 0, true, webClientTimeoutProperties.getSmall());
    return Mono.deferContextual(
        context -> {
          String traceIdForNotificationService =
              context.get("traceId") + "_" + UUID.randomUUID().toString().substring(0, 7);
          log.info(
              "[{}] calling notification service bulk api with traceId: {}",
              "NOTIFICATION_SERVICE",
              traceIdForNotificationService);
          HttpHeaders headers = getNotificationServiceHeaders(traceIdForNotificationService);
          headers.add(
              LOGGER_HEADER,
              "POST " + environment.getProperty("notification-service.api.send-bulk-sms"));
          return webClientFactory.postData(
              uri, requestBody, headers, Object.class, webClientParameters);
        });
  }

  private HttpHeaders getNotificationServiceHeaders(String traceId) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Content-Type", "application/json");
    headers.add("traceId", traceId);
    return headers;
  }
}
