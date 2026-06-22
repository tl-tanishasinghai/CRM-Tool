package com.trillionloans.customer_portal.configuration;

import com.trillionloans.customer_portal.constant.ProductNameMapping;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Component
@ConfigurationProperties(prefix = "product-name-mapping")
public class ProductNameMappingConfig {

  private Map<String, String> mapping;

  @PostConstruct
  public void init() {
    ProductNameMapping.setProductNameMapping(mapping);
  }
}
