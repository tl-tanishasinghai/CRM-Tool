package com.trillionloans.lms.service;

import com.trillionloans.lms.model.request.NotificationRequest;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class KafkaNotificationProducerService {

  private final KafkaTemplate<String, NotificationRequest> kafkaTemplate;

  @Value("${notification.kafka.topic}")
  private String topicName;

  @SuppressWarnings("unchecked")
  public KafkaNotificationProducerService(KafkaTemplate<?, ?> kafkaTemplate) {
    this.kafkaTemplate = (KafkaTemplate<String, NotificationRequest>) kafkaTemplate;
  }

  public Mono<SendResult<String, NotificationRequest>> sendNotification(
      NotificationRequest notificationRequest) {

    String messageKey = UUID.randomUUID().toString();

    CompletableFuture<SendResult<String, NotificationRequest>> future =
        kafkaTemplate.send(topicName, messageKey, notificationRequest);

    return Mono.fromFuture(future)
        .doOnSuccess(
            result ->
                log.info(
                    "[NOTIFICATION] Sent notification with key {} to topic {}",
                    messageKey,
                    topicName))
        .doOnError(
            KafkaException.class,
            error ->
                log.error(
                    "[NOTIFICATION] Kafka exception while sending notification to topic {} with key"
                        + " {}",
                    topicName,
                    messageKey,
                    error))
        .doOnError(
            RuntimeException.class,
            error ->
                log.error(
                    "[NOTIFICATION] Unexpected runtime error while sending notification to topic {}"
                        + " with key {}",
                    topicName,
                    messageKey,
                    error));
  }
}
