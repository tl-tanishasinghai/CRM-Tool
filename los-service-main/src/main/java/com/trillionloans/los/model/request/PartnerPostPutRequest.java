package com.trillionloans.los.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PartnerPostPutRequest {
  private Object requestBody;
  private String uri;
  private String callMethod;
  private String partnerCode;
  private Integer retryCount;
  private String loggerHeader;
}
