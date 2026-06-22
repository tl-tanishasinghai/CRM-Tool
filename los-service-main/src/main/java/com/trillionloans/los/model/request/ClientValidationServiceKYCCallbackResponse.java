package com.trillionloans.los.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class ClientValidationServiceKYCCallbackResponse {
  private ClientValidationFunnelStatus.FinalStatus finalStatus;
  private ClientValidationFunnelStatus.StepStatus nsdlPanValidationStatus;
  private ClientValidationFunnelStatus.StepStatus karzaPanValidationStatus;
  private ClientValidationFunnelStatus.StepStatus karzaNameSimilarityStatus;
  private ClientValidationFunnelStatus.StepStatus dobWaterFallStatus;
  private final String nameFuzzyMatchPercentage;
}
