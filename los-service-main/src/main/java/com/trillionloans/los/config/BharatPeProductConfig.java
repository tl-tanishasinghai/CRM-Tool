package com.trillionloans.los.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bharatpe-products-config")
public class BharatPeProductConfig {

  private String products; // "ELO,ELTO,BPBL1,PLO1"

  public Set<String> getProductSet() {
    return Arrays.stream(products.split(",")).map(String::trim).collect(Collectors.toSet());
  }
}
