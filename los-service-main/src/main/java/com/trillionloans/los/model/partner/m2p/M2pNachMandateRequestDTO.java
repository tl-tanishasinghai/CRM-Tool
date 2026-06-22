package com.trillionloans.los.model.partner.m2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class M2pNachMandateRequestDTO {
  private String status;
  private String umrn;
  private String bankAccountType;
  private String bankAccountHolderName;
  private String bankName;
  private String branchName;
  private String bankAccountNumber;
  private String micr;
  private String ifsc;
  private String mandateRegistrationRequestedDate;
  private String dateFormat;
  private String periodStartDate;
  private String periodEndDate;
  private Boolean periodUntilCancelled;
  private String debitTypeEnum;
  private String debitFrequencyEnum;
  private Double amount;

  @JsonProperty("externalRefernceNumber")
  @SerializedName("externalRefernceNumber")
  private String externalReferenceNumber;

  private String mode;
}
