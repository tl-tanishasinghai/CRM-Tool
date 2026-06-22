package com.trillionloans.los.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration class for Swagger UI and open API documentation setup */
@Configuration
public class SwaggerConfiguration {
  @Bean
  public OpenAPI myOpenAPI() {
    Info info =
        new Info()
            .title("LOS Service APIs")
            .version("1.0")
            .description("This API exposes endpoints for LOS Service from Trillion Loans");

    return new OpenAPI().info(info);
  }
}
