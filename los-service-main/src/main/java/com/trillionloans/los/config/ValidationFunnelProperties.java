package com.trillionloans.los.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "validation-funnel")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationFunnelProperties {

  /** Master switch for the validation funnel (takes precedence over all others). */
  @Builder.Default private boolean masterFlag = false;

  @Builder.Default private Cache cache = new Cache();

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Cache {
    @Builder.Default private boolean enabled = true;
    @Builder.Default private long ttl = 86400;
    @Builder.Default private boolean encryptionEnabled = false;
  }
}
