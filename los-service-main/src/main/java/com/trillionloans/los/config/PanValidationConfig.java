package com.trillionloans.los.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
public class PanValidationConfig {

  private boolean panValidationFeatureFlag;

  private boolean panStatusCheckEnabled;

  private String panStatusExpected;

  private boolean nameMatchCheckEnabled;

  private String nameMatchStatusExpected;

  private boolean dobMatchCheckEnabled;

  private String dobMatchStatusExpected;

  private boolean seedingStatusCheckEnabled;

  private String seedingStatusExpectedValue;
}
