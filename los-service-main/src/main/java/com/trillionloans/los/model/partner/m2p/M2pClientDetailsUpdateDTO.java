package com.trillionloans.los.model.partner.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class M2pClientDetailsUpdateDTO {
  private String locale;
  private String dateFormat;
  private String firstname;
  private String middlename;
  private String lastname;
  private Integer genderId;
  private String dateOfBirth;
  private String mobileNo;
  private String education;
  private String alternateMobileNo;
  private String email;
  private String externalId;
}
