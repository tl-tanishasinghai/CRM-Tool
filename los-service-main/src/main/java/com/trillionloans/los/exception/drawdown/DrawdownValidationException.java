package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Exception thrown when drawdown validation fails (e.g. anchor not found, invalid anchor, GST
 * mismatch).
 */
public class DrawdownValidationException extends BaseException {

  public DrawdownValidationException(String message) {
    super(message, null, HttpStatus.BAD_REQUEST);
  }

  public DrawdownValidationException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.BAD_REQUEST);
  }

  public DrawdownValidationException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
