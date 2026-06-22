package com.trillionloans.lms.model.dto.restructure;

/**
 * Enum representing restructure execution status.
 *
 * @author Pawan Kumar
 */
public enum RestructureStatus {

  /** Eligibility has been checked */
  NOT_TRIGGERED,

  /** Restructure initiated */
  INITIATED,

  /** Restructure failed */
  FAIL,

  /** Restructure successful */
  SUCCESS,

  /** Row has been invalidated */
  INVALIDATED;

  /** Parses a string to RestructureStatus. Returns null if value is null or invalid. */
  public static RestructureStatus from(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return valueOf(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
