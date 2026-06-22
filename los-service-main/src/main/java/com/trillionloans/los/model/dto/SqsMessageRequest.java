package com.trillionloans.los.model.dto;

import lombok.Data;

@Data
public class SqsMessageRequest {
  private String messagePayload;
  private String messageGroupID;
  private String externalId;
  private String partnerId;
  private String partnerName;
}
