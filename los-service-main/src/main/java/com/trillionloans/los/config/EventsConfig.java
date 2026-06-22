package com.trillionloans.los.config;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "events-config")
@Data
@Slf4j
public class EventsConfig {
  public final Map<String, EventConfig> events = new HashMap<>();

  @Data
  public static class EventConfig {
    private SourceConfig request;
    private SourceConfig response;
  }

  @Data
  public static class SourceConfig {
    private String clientId;
    private String loanAppId;
    private Map<String, String> metadata;
  }

  @PostConstruct
  public void init() {
    log.info("Loaded events config: {}", events);
  }
}
