package com.trillionloans.customer_portal.model.response.RpsResponseDto;

import lombok.Data;

@Data
public class Status {
  private Boolean transferOnHold;
  private String code;
  private Boolean transferInProgress;
  private Boolean active;
  private Boolean pendingApproval;
  private Boolean underTransfer;
  private Boolean closedObligationsMet;
  private Boolean closedRescheduled;
  private Boolean overpaid;
  private Boolean waitingForDisbursal;
  private Boolean closedWrittenOff;
  private Boolean closed;
  private Double id;
  private String value;
}
