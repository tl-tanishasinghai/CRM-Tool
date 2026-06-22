package com.trillionloans.los.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AmlPepConfig {
  private Boolean amlPepFeatureFlag;
  private boolean pepCheckEnabled;
  private boolean amlCheckEnabled;
  private Double amlRejectionThreshold;
  private Double amlManualVerificationThreshold;
  private Boolean decoupleFlag;
}
