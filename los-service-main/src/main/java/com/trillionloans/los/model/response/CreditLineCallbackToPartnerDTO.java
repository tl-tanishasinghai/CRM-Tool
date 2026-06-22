package com.trillionloans.los.model.response;

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
public class CreditLineCallbackToPartnerDTO {

  private String limitId;

  private Integer limit;

  private TenureDetails tenureDetails;

  private String status;

  private String leadId;

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class TenureDetails {
    private Integer value;
    private String type;
  }
}
