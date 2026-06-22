package com.trillionloans.los.model.partner.m2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO class for the vehicle details that are to be stamped at M2P's end */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class M2pVehicleDetailsDTO {
  private String additionalDetailsFormKey;
  private VehicleAdditionalDetails vehicleAdditionalDetails;

  @JsonProperty("registrationCharges")
  @SerializedName("registrationCharges")
  private Double vehicleRegistrationCost;

  @JsonProperty("accessoriesCharges")
  @SerializedName("accessoriesCharges")
  private Double standardFitmentAccessoriesChagres;

  private Double showroomPrice;
  private Boolean isNew;

  @JsonProperty("insuranceAmount")
  @SerializedName("insuranceAmount")
  private Double insuranceFee;

  @JsonProperty("miscellaneousPrices")
  @SerializedName("miscellaneousPrices")
  private Double miscellaneousFee;

  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Setter
  @Getter
  public static class VehicleAdditionalDetails {
    private String merchantName;

    @JsonProperty("downPaymentPercetage")
    @SerializedName("downPaymentPercetage")
    private Double downPaymentPercentage;

    private String brand;

    private String model;

    @JsonProperty("InsuranceFee")
    @SerializedName("InsuranceFee")
    private Double insuranceFee;

    @JsonProperty("RTOFee")
    @SerializedName("RTOFee")
    private Double rtoFee;

    @JsonProperty("HypothecationFee")
    @SerializedName("HypothecationFee")
    private Double vehicleRegistrationCost;

    @JsonProperty("AccessoriesCharges")
    @SerializedName("AccessoriesCharges")
    private Double standardFitmentAccessoriesChagres;

    @JsonProperty("Tenure")
    @SerializedName("Tenure")
    private int tenure;

    @JsonProperty("InterestRate")
    @SerializedName("InterestRate")
    private Double interestRate;

    @JsonProperty("RepaymentStartDate")
    @SerializedName("RepaymentStartDate")
    private String repaymentStartDate;

    @JsonProperty("ExpectedDisbursementDate")
    @SerializedName("ExpectedDisbursementDate")
    private String expectedDisbursementDate;

    @JsonProperty("RoyalEnfield")
    @SerializedName("RoyalEnfield")
    private Boolean isRoyalEnfield;

    @JsonProperty("onroadprice")
    @SerializedName("onroadprice")
    private Double onRoadPrice;

    @JsonProperty("FieldInvestigationDone")
    @SerializedName("FieldInvestigationDone")
    private Boolean isFieldInvestigationDone;

    @JsonProperty("EV")
    @SerializedName("EV")
    private Boolean isEv;

    @JsonProperty("LTV")
    @SerializedName("LTV")
    private int ltv;

    @JsonProperty("DateofManufacturing")
    @SerializedName("DateofManufacturing")
    private String dateOfManufacturing;
  }
}
