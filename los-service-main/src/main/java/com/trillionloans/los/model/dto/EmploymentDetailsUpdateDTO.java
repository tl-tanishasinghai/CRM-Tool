package com.trillionloans.los.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class EmploymentDetailsUpdateDTO {
  private String companyType;
  private String employmentType;

  @PositiveOrZero(message = "[employmentDetails] amount cannot be negative")
  private double monthlySalary;

  @Min(value = 0, message = "[EmploymentDetails]  totalWorkExperience must be a positive integer")
  @Max(
      value = 99999,
      message =
          "[EmploymentDetails] Value exceeds maximum allowed digits(5) for totalWorkExperience"
              + " field")
  private Integer totalWorkExperience;

  private String currentEmployerName;
}
