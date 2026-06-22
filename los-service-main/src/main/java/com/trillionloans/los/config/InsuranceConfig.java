package com.trillionloans.los.config;

import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class InsuranceConfig {
  private Boolean insuranceFeatureFlag;
  private List<InsuranceGridRow> insuranceGrid;
  private Integer insuranceChargeId;
  private Boolean skipUpdateAddVas;

  @Getter
  @Setter
  @Builder
  public static class InsuranceGridRow {
    private Double minAmount;
    private Double maxAmount;
    private Integer tenureYear;
    private Double sumInsured;
    private Double premium;
    private Double premiumWithGst;
    private String assureKitPlanName;
  }
}
