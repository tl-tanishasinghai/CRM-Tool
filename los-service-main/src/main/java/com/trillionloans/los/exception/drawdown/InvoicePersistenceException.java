package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Exception thrown when invoice persistence fails. */
public class InvoicePersistenceException extends BaseException {

  public InvoicePersistenceException(String message) {
    super(message, null, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public InvoicePersistenceException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public InvoicePersistenceException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
