package com.trillionloans.los.config.health;

import io.r2dbc.spi.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class HealthCheckLoggingConfiguration {

    @Bean("rdsHealth")
    public ReactiveHealthIndicator rdsHealthIndicator(ConnectionFactory connectionFactory) {
        return () -> {
            log.info("RDS health check triggered");
            return Mono.defer(
                            () ->
                                    Mono.usingWhen(
                                            connectionFactory.create(),
                                            connection ->
                                                    Mono.just(
                                                            Health.up().withDetail("database", "RDS").build()),
                                            connection -> Mono.from(connection.close())))
                    .onErrorResume(
                            ex -> {
                                log.warn("Health check failed : RDS FAILED - {}", ex.getMessage());
                                return Mono.just(
                                        Health.down()
                                                .withDetail("database", "RDS")
                                                .withDetail("error", ex.getMessage())
                                                .build());
                            });
        };
    }

    @Bean("redisHealth")
    public ReactiveHealthIndicator redisHealthIndicator(
            ReactiveRedisConnectionFactory redisConnectionFactory) {
        return () -> {
            log.info("Redis health check triggered");
            return Mono.defer(
                            () ->
                                    redisConnectionFactory
                                            .getReactiveConnection()
                                            .ping()
                                            .map(result -> Health.up().withDetail("redis", "REDIS").build()))
                    .onErrorResume(
                            ex -> {
                                log.warn("Health check failed : REDIS FAILED - {}", ex.getMessage());
                                return Mono.just(
                                        Health.down()
                                                .withDetail("redis", "REDIS")
                                                .withDetail("error", ex.getMessage())
                                                .build());
                            });
        };
    }
}
