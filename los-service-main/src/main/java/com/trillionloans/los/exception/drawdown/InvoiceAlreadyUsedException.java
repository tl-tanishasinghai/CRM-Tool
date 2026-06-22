package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Exception thrown when invoices have already been used in a drawdown. */
public class InvoiceAlreadyUsedException extends BaseException {

  public InvoiceAlreadyUsedException(String message) {
    super(message, null, HttpStatus.BAD_REQUEST);
  }

  public InvoiceAlreadyUsedException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.BAD_REQUEST);
  }

  public InvoiceAlreadyUsedException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
