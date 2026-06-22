package com.trillionloans.los.model.response.m2p;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class M2pProductDetailsResponseDTO {
  private LosWorkflowMapping losWorkflowMapping;
  private LosLoanProductMapping losLoanProductMapping;
  private LosCreditBureauMapping losCreditBureauMapping;
  private List<LosProductOfficeMapping> losProductOfficeMapping;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class LosWorkflowMapping {
    private int mappedWorkflowId;
    private String workflowMappingName;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class LosLoanProductMapping {
    private int loanProductId;
    private String loanProductName;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class LosCreditBureauMapping {
    private int creditBureauId;
    private String creditBureauName;
    private int stalePeriod;
  }

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class LosProductOfficeMapping {
    private int officeId;
    private String productKey;
  }
}
