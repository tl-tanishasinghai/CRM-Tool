package com.trillionloans.los.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "application-rejection.codes")
@Getter
@Setter
public class RejectionReasonCodeProperties {
  private Integer kycFail;
  private Integer breFail;
  private Integer partnerRejection;
  private Integer breExpired;
  private Integer pennyDrop;
}
