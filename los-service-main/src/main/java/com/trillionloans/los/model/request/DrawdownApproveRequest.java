package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for approving a drawdown. Called by OPS to approve a drawdown that is in
 * OPS_APPROVAL_PENDING state.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrawdownApproveRequest {

  /** Transaction time in epoch milliseconds */
  @NotNull(message = "Transaction time is required")
  private Long transactionTime;

  /** Notes for the approval */
  private String notes;

  /** Payment type (e.g., "online transfer") */
  @NotBlank(message = "Payment type is required")
  private String paymentType;

  /** Payment reference number / Receipt number */
  @NotBlank(message = "Reference number is required")
  private String referenceNumber;
}
