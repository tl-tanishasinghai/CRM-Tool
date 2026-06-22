package com.trillionloans.los.model.dto;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdditionalDetailsDTO {
  private String dataTableName;
  private String appTable;
  private String mPin;

  @Pattern(
      regexp = "^[A-Za-z_/& -. ]+$",
      message = "[additionalDetails] subIndustry contains invalid characters")
  private String subIndustry;

  @Pattern(
      regexp = "^[A-Za-z_/& ]+$",
      message = "[additionalDetails] industry contains invalid characters")
  private String industry;

  @Pattern(
      regexp = "^[A-Za-z ]+$",
      message = "[additionalDetails] businessType contains invalid characters")
  private String businessType;

  private String businessAddress;

  @Pattern(
      regexp = "^[A-Za-z &]+$",
      message = "[additionalDetails] state contains invalid characters")
  private String state;

  @Pattern(
      regexp = "^[A-Za-z0-9. &]+$",
      message = "[additionalDetails] city contains invalid characters")
  private String city;

  @Pattern(
      regexp = "^[A-Za-z ]+$",
      message = "[additionalDetails] country contains invalid characters")
  private String country;

  private Integer postalCode;

  @Pattern(
      regexp = "^[A-Za-z ]+$",
      message = "[additionalDetails] businessAddressType contains invalid characters")
  private String businessAddressType;

  @Pattern(
      regexp = "^[A-Za-z.&/ -']+$",
      message = "[additionalDetails] businessName contains invalid characters")
  private String businessName;

  private String vernacularPreference;
  private String vcipStatus;
  private String vcipRejectionReason;
  private String okycTimeStamp;

  @Pattern(
      regexp = "^[A-Za-z0-9 ]+$",
      message = "[additionalDetails] gstNumber contains invalid characters")
  private String gstNumber;

  @Pattern(
      regexp = "^[A-Za-z&/ -]+$",
      message = "[additionalDetails] businessDocument contains invalid characters")
  private String businessDocument;

  private String legalName;
  private String tradeName;
  private String bankBeneName;

  private String udhyamNumber;
  private String dateOfIncorporation;
  private String categoryOfBusiness;
}
