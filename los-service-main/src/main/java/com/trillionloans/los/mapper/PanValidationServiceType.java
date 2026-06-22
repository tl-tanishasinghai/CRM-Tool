package com.trillionloans.los.mapper;

import lombok.Getter;

@Getter
public enum PanValidationServiceType {
  STANDALONE_NSDL_PAN_VALIDATION,
  VALIDATION_FUNNEL,
  NONE
}
