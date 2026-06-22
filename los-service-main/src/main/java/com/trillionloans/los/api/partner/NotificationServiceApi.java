package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.LOGGER_HEADER;

import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.WebClientUtil;
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

  public NotificationServiceApi(
      @Value("${notification-service.api.base-url}") String baseUrl,
      Environment environment,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService) {
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl,
            "NOTIFICATION_SERVICE",
            environment,
            kafkaLoggingService,
            kafkaEventProducerService);
    this.environment = environment;
    this.webClientUtil = new WebClientUtil();
  }

  public Mono<?> triggerNotification(Object requestBody, String loanApplicationId) {
    return triggerNotification(requestBody, loanApplicationId, Event.LEAD_ACK);
  }

  public Mono<?> triggerNotification(Object requestBody, String loanApplicationId, Event event) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("notification-service.api.send-sms")))
            .toUriString();
    EventContext eventContext = new EventContext(event, loanApplicationId);
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(null, "SEND_SMS", 0, true, true, eventContext);
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
          return webClientFactory.postDataWithoutStringSerialization(
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
