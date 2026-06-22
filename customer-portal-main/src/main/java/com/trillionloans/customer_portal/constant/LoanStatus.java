package com.trillionloans.customer_portal.constant;

import static com.trillionloans.customer_portal.constant.StringConstants.UNKNOWN_LOAN_STATUS;

import lombok.Getter;

@Getter
public enum LoanStatus {
  ACTIVE(300, "ACTIVE"),
  CLOSED(600, "CLOSED"),
  OVERPAID(700, "OVERPAID"),
  WRITTEN_OFF(601, "WRITTEN_OFF");

  private final int code;
  private final String status;

  LoanStatus(int code, String status) {
    this.code = code;
    this.status = status;
  }

  public static String getStatusNameByCode(int code) {

    for (LoanStatus status : LoanStatus.values()) {
      if (status.getCode() == code) {
        return status.getStatus();
      }
    }
    return UNKNOWN_LOAN_STATUS;
  }
}
