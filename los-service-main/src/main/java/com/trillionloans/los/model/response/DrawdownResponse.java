package com.trillionloans.los.model.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DrawdownResponse {
  private String anchorId;
  private String partnerId;
  private String drawdownId;
  private String transactionId;
  private String status;
  private String externalId;
}
