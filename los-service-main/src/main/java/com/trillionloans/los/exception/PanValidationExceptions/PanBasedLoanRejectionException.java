package com.trillionloans.los.exception.PanValidationExceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
public class PanBasedLoanRejectionException extends RuntimeException {
  private final transient Object clientResponse;
  private final HttpStatusCode httpStatusCode;
  private final String url;

  public PanBasedLoanRejectionException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message);
    this.clientResponse = clientResponse;
    this.httpStatusCode = httpStatusCode;
    this.url = null;
  }
}
