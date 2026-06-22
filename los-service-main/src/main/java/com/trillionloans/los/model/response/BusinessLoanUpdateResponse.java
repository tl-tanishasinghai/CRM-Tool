package com.trillionloans.los.model.response;

import com.trillionloans.los.constant.ResponseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response for business loan details update")
public class BusinessLoanUpdateResponse {
  @Schema(description = "Outcome: SUCCESS, FAIL, or SERVER_ERROR", example = "SUCCESS")
  private ResponseStatus status;

  @Schema(description = "Message describing the result")
  private String message;
}
