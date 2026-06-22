package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Exception thrown when invoice validation fails (e.g. missing/invalid invoice data). */
public class InvoiceValidationException extends BaseException {

  public InvoiceValidationException(String message) {
    super(message, null, HttpStatus.BAD_REQUEST);
  }

  public InvoiceValidationException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.BAD_REQUEST);
  }

  public InvoiceValidationException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
