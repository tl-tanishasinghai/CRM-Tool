package com.trillionloans.los.model.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class EmploymentDetailsDTO {
  private String companyType;

  //  @Pattern(
  //      regexp = "^[A-Za-z ]+$",
  //      message = "[employmentDetails] employmentType contains invalid characters")
  private String employmentType;

  @PositiveOrZero(message = "[employmentDetails] amount cannot be negative")
  private double monthlySalary;

  private Integer totalWorkExperience;

  //  @Pattern(
  //      regexp = "^[A-Za-z -.]+$",
  //      message = "[employmentDetails] currentEmployerName contains invalid characters")
  private String currentEmployerName;

  private String existingIncomeObligation;
  private String occupationType;
}
