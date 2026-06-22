package com.trillionloans.los.constant;

public enum DocumentEvaluationStatus {
  NOT_READY,
  QUALIFIED,
  NOT_QUALIFIED,
  /** Analytics did not return a usable score for this document (name and/or address). */
  FAIL
}
