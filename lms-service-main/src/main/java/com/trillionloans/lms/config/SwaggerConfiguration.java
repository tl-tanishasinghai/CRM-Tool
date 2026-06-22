package com.trillionloans.lms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

  @Bean
  public OpenAPI myOpenAPI() {
    Info info =
        new Info()
            .title("LMS Service APIs")
            .version("1.0")
            .description("This API exposes endpoints for LMS Service from Trillion Loans");

    return new OpenAPI().info(info);
  }
}
