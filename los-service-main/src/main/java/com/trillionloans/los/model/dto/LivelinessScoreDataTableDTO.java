package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@AllArgsConstructor
public class LivelinessScoreDataTableDTO {

  private String lead;

  @JsonProperty("loan_application_id")
  private String loanApplicationId;

  @NotNull(message = "[LIVELINESS_SCORE_CREATE] [ERROR] score cannot be null")
  @Min(value = 0, message = "[LIVELINESS_SCORE_CREATE] [ERROR] score must be at least 0")
  @Max(value = 100, message = "[LIVELINESS_SCORE_CREATE] [ERROR] score must be at most 100")
  @Pattern(
      regexp = "^(\\d{1,3})(\\.\\d{1,2})?$",
      message =
          "[LIVELINESS_SCORE_CREATE] [ERROR] score must be a valid positive number with at most 3"
              + " digits before the decimal and at most 2 decimal places")
  private String score;

  @JsonProperty("image_id")
  private String imageId;

  @Pattern(
      regexp = "^.{0,100}$",
      message = "[LIVELINESS_SCORE_CREATE] [ERROR] ip is allowed upto 100 characters")
  private String ip;

  private String timestamp;
}
