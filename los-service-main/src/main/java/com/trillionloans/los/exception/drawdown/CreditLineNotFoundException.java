package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Exception thrown when credit line or leadId is not found. */
public class CreditLineNotFoundException extends BaseException {

  public CreditLineNotFoundException(String message) {
    super(message, null, HttpStatus.NOT_FOUND);
  }

  public CreditLineNotFoundException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.NOT_FOUND);
  }

  public CreditLineNotFoundException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
