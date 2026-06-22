package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Credit line lead creation request body")
public class CreditLineLoanApplication {

  @Size(max = 100, message = "[loanApplication] externalId should be under 100 characters")
  private String externalId;

  @Builder.Default private final String losProductKey = "FUND";
}
