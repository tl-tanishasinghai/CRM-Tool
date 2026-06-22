package com.trillionloans.los.model.dto.internal;

import java.time.LocalDateTime;
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
public class LoanFunnelDTO {

  private String requestId;
  private String leadId;
  private String clientId;
  private String status;
  private Stage stage;
  private SubStage subStage;
  private LocalDateTime createdAt;
  private String serviceName;
  private String traceId;
  private String rejectionReason; // only rejection reasons
  private String failReason; // only failed reasons

  public enum Stage {
    LOAN_CREATION,
    BRE,
    MANDATE_REGISTRATION
  }

  public enum SubStage {
    TOP_OF_FUNNEL,
    BRE_INITIATED,
    BRE_M2P_GET_LOAN_DETAIL,
    BRE_BUREAU_HARD_PULL,
    BRE_SCIENAPTIC,
    BRE_PERFORMANCE_DATA,
    BRE_M2P_UPDATE,
    BRE_CTA,

    // Mandate Registration related sub stages
    MR_PRODUCT_CONFIG,
    MR_CLIENT_DETAILS,
    MR_LOAN_DETAILS,
    MR_BANK_DETAILS,
    MR_DIGIO_CREATE,
    MR_FINAL,

    // Mandate Registration Status related sub stages
    MRS_PRODUCT_CONFIG,
    MRS_DETAILS,
    MRS_DIGIO_STATUS,
    MRS_M2P_UPDATE,
    MRS_FINAL,
  }
}
