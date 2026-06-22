package com.trillionloans.los.model.dto;

import com.trillionloans.los.constant.MandateAuthMode;
import com.trillionloans.los.constant.MandateRegistrationVendor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MandateRegistrationConfigDTO {
  private MandateAuthMode authMode;
  private String corporateConfigId;
  private String mandateType;
  private Boolean notifyCustomer;
  private Boolean isRecurring;
  private String frequencyType;
  private String managementCategory;
  private Long firstCollectionIncrement;
  private Long finalCollectionIncrement;
  private Boolean generateAccessToken;
  private String instrumentType;
  private String redirectionUrl;
  private MandateRegistrationVendor vendorName;
  private Boolean periodUntilCancelled;
  private String debitTypeEnum;
  private String mode;
}
