package com.trillionloans.customer_portal.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebConfig implements WebFluxConfigurer {

  @Value("${allowed.origin:*}")
  private String allowedOrigin;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOriginPatterns(allowedOrigin)
        .allowedMethods(HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.OPTIONS.name())
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}
