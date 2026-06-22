package com.trillionloans.los.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties("web-client.timeout")
public class WebClientTimeoutProperties {
  private int extraSmall;
  private int small;
  private int medium;
  private int large;
  private int extraLarge;
}
