package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Exception thrown when a drawdown with the given external ID already exists. */
public class DrawdownExternalIdAlreadyExistsException extends BaseException {

  public DrawdownExternalIdAlreadyExistsException(String message) {
    super(message, null, HttpStatus.BAD_REQUEST);
  }

  public DrawdownExternalIdAlreadyExistsException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.BAD_REQUEST);
  }

  public DrawdownExternalIdAlreadyExistsException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
