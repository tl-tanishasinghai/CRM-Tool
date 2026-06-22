package com.trillionloans.los.model.partner.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class M2pEmploymentDetailsDTO {
  private String companyType;
  private String employmentType;
  private double monthlySalary;
  private Integer totalWorkExperience;
  private String currentEmployerName;
  private String existingIncomeObligation;
  private String occupationType;
}
