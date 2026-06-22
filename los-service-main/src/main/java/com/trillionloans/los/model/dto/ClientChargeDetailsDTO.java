package com.trillionloans.los.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ClientChargeDetailsDTO {
  private int chargeId;
  private String dateFormat;
  private String locale;
  private String dueDate;
  private double amount;
  private Boolean syncMeeting;
}
