package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(
    description =
        "Request body for saving miscellaneous details for client and/or loan application")
public class SaveMiscellaneousDetailsRequest {

  @Schema(
      description = "Client miscellaneous details as key-value pairs (optional)",
      example = "{\"key1\": \"value1\", \"key2\": \"value2\"}")
  private Map<String, String> clientMiscellaneousDetails;

  @Schema(
      description = "Loan application miscellaneous details as key-value pairs (optional)",
      example = "{\"loanKey1\": \"loanValue1\", \"loanKey2\": \"loanValue2\"}")
  private Map<String, String> loanApplicationMiscellaneousDetails;
}
