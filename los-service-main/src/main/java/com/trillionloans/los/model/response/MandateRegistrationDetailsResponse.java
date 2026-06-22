package com.trillionloans.los.model.response;

import com.trillionloans.los.constant.MandateType;
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
public class MandateRegistrationDetailsResponse {

  private String mandateId;
  private String state;
  private MandateType type;
  private BankDetails bankDetails;
  private String createdAt;
  private String firstCollectionDate;
  private String finalCollectionDate;
  private Double amount;
  private String authenticationTime;
  private String frequency;
  private String customerAccountNumber;
  private String destinationBankId;

  // ----- nested DTOs -----

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class BankDetails {
    private String sharedWithBank;
    private String bankName;
    private String state;
    private String authenticatedAt;
  }
}
