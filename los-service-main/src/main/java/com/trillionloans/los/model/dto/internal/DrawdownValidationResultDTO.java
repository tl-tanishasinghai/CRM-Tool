package com.trillionloans.los.model.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO for drawdown validation result. Contains the validation status and associated client ID. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DrawdownValidationResultDTO {

  /** Indicates whether the drawdown validation passed. */
  private boolean isValid;

  /** The client ID associated with the loan. */
  private String clientId;
}
