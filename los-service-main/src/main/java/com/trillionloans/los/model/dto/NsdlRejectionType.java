package com.trillionloans.los.model.dto;

import lombok.Getter;

public enum NsdlRejectionType {
  PAN_STATUS("PAN status"),
  SEEDING_STATUS("Seeding status"),
  NAME_STATUS("Name"),
  DOB_STATUS("DOB");

  @Getter private final String fieldName;

  NsdlRejectionType(String fieldName) {
    this.fieldName = fieldName;
  }
}
