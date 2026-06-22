package com.trillionloans.los.model.dto;

import java.util.List;
import lombok.Data;

@Data
public class AssurekitCreatePlanResponse {
  private Result result;
  private String errorCode;
  private String message;
  private Boolean status;
  private String responseTimestamp;
  private String requestId;

  @Data
  public static class Result {
    private String programId;
    private String insurerId;
    private String clientId;
    private String reInsurerId;
    private String clientDisplayName;
    private String clientName;
    private String programDisplayName;
    private String programName;
    private String ruleId;
    private String protectionPlanId;
    private String downloadProtectionPlanLink;
    private String loanId;
    private String customerName;
    private String customerMobileNo;
    private String customerEmail;
    private Double loanAmount;
    private String loanStartTime;
    private String loanEndTime;
    private Double emiAmount;
    private String firstEmiTime;
    private Double interestPerAnnum;
    private Integer tenureOfLoan;
    private String planStartTime;
    private String planEndTime;
    private Double planNetPrice;
    private Double planTaxValue;
    private Double planDiscountValue;
    private Double planGrossPrice;
    private Double sumInsured;
    private String planName;
    private String pdfTemplateVersion;
    private String pdfPlaceholderVersion;
    private String loanType;
    private String status;
    private Integer cancellationRaisedCount;
    private Integer claimRaisedCount;
    private Integer claimWaitPeriodInDays;
    private String channelPartner;
    private String assetName;
    private String assetIdentifier;
    private String assetPincode;
    private String assetAddress;
    private List<Update> updates;
    private List<Benefit> benefits;
    private List<Exclusion> exclusions;
    private String createdBy;
    private String updatedBy;
    private String createdAt;
    private String updatedAt;
  }

  @Data
  public static class Update {
    private String status;
    private String updatedAt;
  }

  @Data
  public static class Benefit {
    private String title;
    private String description;
    private String icon;
    private String code;
  }

  @Data
  public static class Exclusion {
    private String title;
    private String description;
    private String icon;
    private String code;
  }
}
