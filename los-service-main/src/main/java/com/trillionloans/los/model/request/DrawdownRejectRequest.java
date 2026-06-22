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
 * Request body for rejecting a drawdown. Called by OPS to reject a drawdown that is in
 * OPS_APPROVAL_PENDING state.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DrawdownRejectRequest {

  /** Transaction time in epoch milliseconds */
  @NotNull(message = "Transaction time is required")
  private Long transactionTime;

  /** Rejection notes/reason */
  @NotBlank(message = "Rejection notes are required")
  private String rejectionNotes;
}
