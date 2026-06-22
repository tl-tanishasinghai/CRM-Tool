package com.trillionloans.customer_portal.configuration;

import static com.trillionloans.customer_portal.constant.StringConstants.NONE;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.session.CookieWebSessionIdResolver;
import org.springframework.web.server.session.WebSessionIdResolver;

@Configuration
//@Profile("qa")
public class SessionConfig {
    @Value("${cookie.sameSite:None}")
    private String sameSite;

  @Bean
  public WebSessionIdResolver webSessionIdResolver() {
    CookieWebSessionIdResolver resolver = new CookieWebSessionIdResolver();
    resolver.addCookieInitializer(builder -> builder.secure(Boolean.TRUE).sameSite(sameSite));
    return resolver;
  }
}