package com.trillionloans.los.model.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoanLevelClientDetailsCacheDTO {
  private Integer clientId;
  private Integer loanApplicationId;
  private String productCode;
  private String firstName;
  private String middleName;
  private String lastName;
  private String fatherFirstName;
  private String fatherLastName;
  private String dateOfBirth;
  private String panNumber;
  private String mobileNo;
  private String pincode;
}
