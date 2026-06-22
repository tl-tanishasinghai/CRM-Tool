package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanInsuranceRequestDto {

  @JsonProperty("loan_id")
  private Long loanId;

  @JsonProperty("client_id")
  private Long clientId;

  @JsonProperty("name")
  private String name;

  @JsonProperty("full_name")
  private String fullName;

  @JsonProperty("phone")
  private String phone;

  @JsonProperty("email")
  private String email;

  @JsonProperty("loan_start_date")
  private String loanStartDate;

  @JsonProperty("approved_principal")
  private Double approvedPrincipal;

  @JsonProperty("term_frequency")
  private Integer termFrequency;

  @JsonProperty("term_period_frequency_enum")
  private Integer termPeriodFrequencyEnum;
}
