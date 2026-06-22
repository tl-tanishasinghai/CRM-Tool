package com.trillionloans.los.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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
public class InitiateCreditLineRequestDTO {

  @NotNull(message = "allowedLimit is required")
  private Integer allowedLimit;

  @NotNull(message = "tenureDetails is required")
  @Valid
  private TenureDetails tenureDetails;

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class TenureDetails {
    @NotNull(message = "tenure value is required")
    private Integer value;

    @NotNull(message = "tenure type is required")
    private String type; // MONTHS or DAYS
  }
}
