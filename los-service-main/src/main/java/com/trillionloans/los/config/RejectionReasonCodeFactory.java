package com.trillionloans.los.config;

import org.springframework.stereotype.Component;

@Component
public class RejectionReasonCodeFactory {

  private final RejectionReasonCodeProperties properties;

  public RejectionReasonCodeFactory(RejectionReasonCodeProperties properties) {
    this.properties = properties;
  }

  public Integer getKycFailReasonCode() {
    return properties.getKycFail();
  }

  public Integer getBreFailReasonCode() {
    return properties.getBreFail();
  }

  public Integer getBreOfferExpiryCode() {
    return properties.getBreExpired();
  }

  public Integer getPennyDropCode(){
    return properties.getPennyDrop();
  }

  public Integer getPartnerRejectionReasonCode() {
    return properties.getPartnerRejection();
  }
}
