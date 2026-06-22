package com.trillionloans.los.model.partner.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class M2pClientDetailsDTO {
  private String firstName;
  private String middleName;
  private String lastName;
  private String gender;
  private String dateOfBirth;
  private String email;
  private String mobileNo;
  private String alternateMobileNo;
  private String officeName;
  private String education;
  private String externalId;
  private String submittedOnDate;
  private String activationDate;
}
