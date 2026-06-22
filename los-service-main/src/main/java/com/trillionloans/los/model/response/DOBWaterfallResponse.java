package com.trillionloans.los.model.response;

import java.util.List;
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
public class DOBWaterfallResponse {
  private boolean dobWaterFallFinalStatus;

  private boolean ruleOneTriggered;
  private boolean ruleOnePass;

  private boolean ruleTwoTriggered;
  private boolean ruleTwoPass;

  private List<String> rejectionReasons;
}
