package com.trillionloans.lms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing status details for the collection module.
 *
 * @author sofiyan
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Status DTO for collection module")
public class StatusDTO {

  private int id;
  private String code;
  private String value;
  private boolean pendingApproval;
  private boolean waitingForDisbursal;
  private boolean active;
  private boolean closedObligationsMet;
  private boolean closedWrittenOff;
  private boolean closedRescheduled;
  private boolean closed;
  private boolean overpaid;
  private boolean transferInProgress;
  private boolean transferOnHold;
  private boolean underTransfer;
}
