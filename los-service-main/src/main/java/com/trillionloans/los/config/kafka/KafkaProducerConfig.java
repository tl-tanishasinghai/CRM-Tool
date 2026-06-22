package com.trillionloans.los.config.kafka;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Configuration class for Kafka Producer. This class sets up the necessary configuration properties
 * for producing messages to a Kafka topic.
 */
@Configuration
public class KafkaProducerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.producer.max-request-size}")
  private int maxRequestSize;

  @Value("${spring.kafka.producer.acks}")
  private String acksConfig;

  @Value("${spring.kafka.producer.request-timeout-ms}")
  private int requestTimeoutMs;

  @Value("${spring.kafka.producer.max-block-ms}")
  private int maxBlockMs;

  @Value("${spring.kafka.producer.compression-type}")
  private String compressionType;

  /**
   * Creates a ProducerFactory bean for configuring the Kafka producer. The ProducerFactory is
   * responsible for creating Kafka producer instances.
   *
   * @return a configured ProducerFactory
   */
  @Bean
  public ProducerFactory<String, String> producerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSize);
    configProps.put(ProducerConfig.ACKS_CONFIG, acksConfig);
    configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
    configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, maxBlockMs);
    configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, compressionType);
    return new DefaultKafkaProducerFactory<>(configProps);
  }

  /**
   * Creates a KafkaTemplate bean for sending messages to Kafka topics. The KafkaTemplate simplifies
   * sending messages and provides convenient methods.
   *
   * @return a configured KafkaTemplate
   */
  @Bean
  public KafkaTemplate<String, String> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }
}
