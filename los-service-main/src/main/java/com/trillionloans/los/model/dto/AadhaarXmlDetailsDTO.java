package com.trillionloans.los.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AadhaarXmlDetailsDTO {

  private String careOf;
  private String house;
  private String street;
  private String landmark;
  private String locality;
  private String vtc;
  private String subdistrict;
  private String district;
  private String state;
  private String pincode;
  private String country;
  private String name;
  private String dob;
  private String fatherName;
  private String dependent;
  private String photoBase64;
  private String ts;
}
