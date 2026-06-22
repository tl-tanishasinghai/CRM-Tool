package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** DTO class for the vehicle details that are fetched from LSP. */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "Vehicle details request body")
public class VehicleDetailsRequest {

  /** The showroom price of the vehicle. */
  private Double showroomPrice;

  /** Indicates whether the vehicle is new (true) or used (false). */
  private Boolean isNew;

  /** The name of the merchant associated with the vehicle. Must not exceed 90,000 characters. */
  @Size(
      max = 90000,
      message =
          "[VehicleDetailsRequest] Value exceeds maximum allowed characters(90000) for merchantName"
              + " field")
  private String merchantName;

  /** The down payment percentage required for the vehicle purchase. */
  private Double downPaymentPercentage;

  /** The brand of the vehicle. Must not exceed 90,000 characters. */
  @Size(
      max = 90000,
      message =
          "[VehicleDetailsRequest] Value exceeds maximum allowed characters(90000) for brand field")
  private String brand;

  /** The model of the vehicle. Must not exceed 90,000 characters. */
  @Size(
      max = 90000,
      message =
          "[VehicleDetailsRequest] Value exceeds maximum allowed characters(90000) for model field")
  private String model;

  /** The insurance fee associated with the vehicle. */
  private Double insuranceFee;

  /** The RTO (Regional Transport Office) fee for the vehicle. */
  private Double rtoFee;

  /** The vehicleRegistrationCost fee associated with the vehicle. */
  private Double vehicleRegistrationCost;

  /** The charges for accessories added to the vehicle. */
  private Double standardFitmentAccessoriesChagres;

  /** The tenure for the vehicle loan in months. */
  private int tenure;

  /** The interest rate applicable for the vehicle loan. */
  private Double interestRate;

  /** The date when the repayment for the loan starts. */
  private String repaymentStartDate;

  /** The expected date for the disbursement of the loan. */
  private String expectedDisbursementDate;

  /** The date of manufacturing of the vehicle. */
  private String dateOfManufacturing;

  /** Indicates whether the vehicle is a Royal Enfield model. */
  private Boolean isRoyalEnfield;

  /** The on-road price of the vehicle. */
  private Double onRoadPrice;

  /** Indicates whether a field investigation has been completed for the vehicle. */
  private Boolean isFieldInvestigationDone;

  /** Indicates whether the vehicle is an electric vehicle (EV). */
  private Boolean isEv;

  /** The loan-to-value (LTV) ratio for the vehicle loan. */
  private int ltv;
}
