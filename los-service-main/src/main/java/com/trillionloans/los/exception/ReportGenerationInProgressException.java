package com.trillionloans.los.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportGenerationInProgressException extends RuntimeException {
  public ReportGenerationInProgressException(String message) {
    super(message);
  }
}
