package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClientDetailsResponseDto {
  private Integer clientId;
  private String firstName;
  private String lastName;
  private String dateOfBirth;
  private String gender;
  private String mobileNo;
  private String alternateMobileNo;
  private String email;
  private String education;
  private String officeName;
  private String externalId;
  private String addressType;
  private String addressLineOne;
  private String addressLineTwo;
  private String landmark;
  private String postalCode;
  private String ffirstName;
  private String flastName;
  private String fdateOfBirth;
  private String fgender;
  private String fdocumentType;
  private String fdocumentKey;
  private Integer panId;
  private String clientPandocumentkey;
  private String ucic;
  private int bankAccountId;
  private String accountNumber;
  private String ifscCode;
  private String name;
  private Boolean supportedForRepayment;
  private Boolean supportedForDisbursement;
  private String bankAccountType;
  private String vkyc;
  private String middleName;
}
