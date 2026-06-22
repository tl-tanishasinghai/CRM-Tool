package com.trillionloans.los.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
public class ValidationFunnelConfiguration {
  // validation funnel feature flag
  private boolean validationFunnelFlagEnabled;

  // This flag controls whether the DOB waterfall validation funnel should run alongside the main
  // validation funnel.
  private boolean dobWaterfallFunnelFeatureFlagEnabled;

  // This flag controls whether the result of the validation funnel should be considered for
  // rejection during the KYC callback.
  private boolean enableValidationFunnelKycRejection;

  // vendor service configs
  private NsdlPanValidationConfig nsdlPanValidationConfig;
  private KarzaPanValidationConfig karzaPanValidationConfig;
  private KarzaNameSimilarityConfig karzaNameSimilarityConfig;
  private KarzaPanAadharLinkageConfig karzaPanAadharLinkageConfig;

  @Getter
  @Setter
  @Builder
  @ToString
  @AllArgsConstructor
  public static class KarzaPanValidationConfig {
    private boolean karzaPanValidationFeatureFlag;
  }

  @Getter
  @Setter
  @Builder
  @ToString
  @AllArgsConstructor
  public static class KarzaNameSimilarityConfig {
    private boolean karzaNameSimilarityFeatureFlag;

    private double score;
    private boolean allowPartialMatch;
    private boolean suppressReorderPenalty;
  }

  @Getter
  @Setter
  @Builder
  @ToString
  @AllArgsConstructor
  public static class NsdlPanValidationConfig {
    private boolean panValidationFeatureFlag;

    // pan related flags
    private boolean panStatusCheckEnabled;
    private String panStatusExpected;
    private boolean panStatusIsCritical;

    // name related flags
    private boolean nameMatchCheckEnabled;
    private String nameMatchStatusExpected;
    private boolean nameMatchIsCritical;

    // dob related flags
    private boolean dobMatchCheckEnabled;
    private String dobMatchStatusExpected;
    private boolean dobMatchIsCritical;

    // seeding status related flags
    private boolean seedingStatusCheckEnabled;
    private String seedingStatusExpectedValue;
    private boolean seedingMatchIsCritical;
  }

  @Getter
  @Setter
  @Builder
  @ToString
  @AllArgsConstructor
  public static class KarzaPanAadharLinkageConfig {
    private boolean karzaPanAadharLinkageFeatureFlag;
  }
}
