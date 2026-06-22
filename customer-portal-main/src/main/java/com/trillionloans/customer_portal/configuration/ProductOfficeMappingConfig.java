package com.trillionloans.customer_portal.configuration;

import com.trillionloans.customer_portal.constant.ProductOfficeMapping;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Component
@ConfigurationProperties(prefix = "product.office")
public class ProductOfficeMappingConfig {

  private Map<Integer, String> mapping;

  @PostConstruct
  public void init() {
    ProductOfficeMapping.setOfficeMapping(mapping);
  }
}
