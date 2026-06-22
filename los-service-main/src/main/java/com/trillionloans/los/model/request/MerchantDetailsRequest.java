package com.trillionloans.los.model.request;

import com.trillionloans.los.validation.ValidLimitAmount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO class for the merchant details that are fetched from LSP. */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Schema(description = "Merchant/anchor details request body")
public class MerchantDetailsRequest {

  /** The name of the merchant. */
  @Size(max = 200, message = "[merchant] name should be under 200 characters")
  @NotEmpty(message = "[merchant] name is required")
  private String name;

  /** The short name or abbreviation for the merchant. */
  @Size(max = 50, message = "[merchant] shortName should be under 50 characters")
  @NotEmpty(message = "[merchant] shortName is required")
  private String shortName;

  /** The limit amount associated with the merchant. */
  @NotEmpty(message = "[merchant] limitAmount is required")
  @ValidLimitAmount(
      message = "[merchant] limitAmount should be a valid number with up to 2 decimal places")
  private String limitAmount;

  /** The end date of the relationship with the merchant. */
  @NotEmpty(message = "[merchant] relationshipEndDate is required")
  private String relationshipEndDate;

  /** The category under which the merchant falls (e.g., retail, services). */
  @Size(max = 30, message = "[merchant] category should be under 30 characters")
  private String category;

  /** The office ID associated with the merchant. */
  @NotNull(message = "[merchant] officeId is required")
  private Integer officeId;

  /** Details of the contact person associated with the merchant. */
  @Valid private ContactPersonDetails contactPersonDetails;

  /** DTO class for the contact person details associated with the merchant. */
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @Setter
  @Getter
  @Valid
  public static class ContactPersonDetails {

    /** The name of the contact person. */
    @Size(max = 30, message = "[merchant.contactPersonDetails] name should be under 30 characters")
    private String name;

    /** The designation of the contact person within the merchant's organization. */
    @Size(
        max = 30,
        message = "[merchant.contactPersonDetails] designation should be under 30 characters")
    private String designation;

    /** The mobile number of the contact person. */
    @Size(
        max = 30,
        message = "[merchant.contactPersonDetails] mobileNumber should be under 30 characters")
    private String mobileNumber;

    /** The email ID of the contact person. */
    @Size(
        max = 100,
        message = "[merchant.contactPersonDetails] emailId should be under 100 characters")
    private String emailId;

    /** The sector in which the merchant operates. */
    @Size(
        max = 50,
        message = "[merchant.contactPersonDetails] sector should be under 50 characters")
    private String sector;

    /** The sub-sector of the merchant's operation for more specific categorization. */
    @Size(
        max = 50,
        message = "[merchant.contactPersonDetails] subSector should be under 50 characters")
    private String subSector;
  }
}
