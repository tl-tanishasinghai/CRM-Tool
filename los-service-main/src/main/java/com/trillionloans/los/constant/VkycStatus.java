package com.trillionloans.los.constant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(name = "vkyc status", description = "Enum values for vkyc status types")
public enum VkycStatus {
  NEW("New"),
  INPROGRESS("In_Progress"),
  REJECTED("Rejected"),
  APPROVED("Approved"),
  CANCELLED("Cancelled"),
  OTHERS("others");

  private final String displayName;

  VkycStatus(String displayName) {
    this.displayName = displayName;
  }
}
