package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Exception thrown when a drawdown is not found (by id, transactionId, or externalId). */
public class DrawdownNotFoundException extends BaseException {

  public DrawdownNotFoundException(String message) {
    super(message, null, HttpStatus.NOT_FOUND);
  }

  public DrawdownNotFoundException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.NOT_FOUND);
  }

  public DrawdownNotFoundException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
