package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigInteger;
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
public class ClientDetailsCpResponseDto {
  private BigInteger leadId;
  private String name;
  private String dateOfBirth;
  private Integer age;
  private String address;
  private String email;
  private String mobileNo;
  private String ucic;
  private String panNumber;
  private String loanAccounts;
}
