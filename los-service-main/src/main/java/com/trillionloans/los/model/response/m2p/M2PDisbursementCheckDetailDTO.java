package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@Builder
@NoArgsConstructor
@Getter
@Setter
public class M2PDisbursementCheckDetailDTO {

  private Boolean hasLoan;
  private Boolean esignStatus;
  private Boolean hasLimit;
  private Double loanAmountRequest;
  private Double breAmount;
  private Double apr;
  private Double roi;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate clientDob;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  private LocalDate aadhaarDob;

  private Double tenure;
  private Double pf;
}
