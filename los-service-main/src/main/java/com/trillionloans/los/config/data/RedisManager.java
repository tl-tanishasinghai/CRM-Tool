package com.trillionloans.los.config.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisManager {

  @Value("${spring.redis.host}")
  private String redisHost;

  @Value("${spring.redis.port}")
  private int redisPort;

  @Value("${spring.redis.password}")
  private String redisPassword;

  @Value("${spring.redis.database}")
  private String redisDatabase;

  @Bean
  public LettuceConnectionFactory lettuceConnectionFactory() {
    RedisStandaloneConfiguration redisStandaloneConfig = new RedisStandaloneConfiguration();
    redisStandaloneConfig.setHostName(redisHost);
    redisStandaloneConfig.setPort(redisPort);
    redisStandaloneConfig.setPassword(redisPassword);
    redisStandaloneConfig.setDatabase(Integer.parseInt(redisDatabase));
    return new LettuceConnectionFactory(redisStandaloneConfig);
  }

  @Bean
  public ReactiveRedisOperations<String, String> redisOperations(
      LettuceConnectionFactory lettuceConnectionFactory) {
    RedisSerializationContext<String, String> serializationContext =
        RedisSerializationContext.<String, String>newSerializationContext(
                new StringRedisSerializer())
            .key(new StringRedisSerializer())
            .value(new GenericToStringSerializer<>(String.class))
            .build();
    return new ReactiveRedisTemplate<>(lettuceConnectionFactory, serializationContext);
  }
}
