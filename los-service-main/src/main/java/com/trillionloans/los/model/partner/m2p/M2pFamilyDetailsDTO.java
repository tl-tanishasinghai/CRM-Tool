package com.trillionloans.los.model.partner.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class M2pFamilyDetailsDTO {
  private String firstName;
  private String lastName;
  private String dateOfBirth;
  private String documentType;
  private String documentKey;
  private String relationship;
  private String gender;
}
