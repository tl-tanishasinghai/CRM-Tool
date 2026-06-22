package com.trillionloans.customer_portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class CustomerPortalApplication {

  public static void main(String[] args) {
    SpringApplication.run(CustomerPortalApplication.class, args);
  }
}
