package com.trillionloans.customer_portal.configuration;

import com.trillionloans.customer_portal.constant.OfficeLogoMapping;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Component
@ConfigurationProperties(prefix = "office")
public class OfficeLogoMappingConfig {

  private Map<String, String> url;

  @PostConstruct
  public void init() {
    OfficeLogoMapping.setUrlMapping(url);
  }
}
