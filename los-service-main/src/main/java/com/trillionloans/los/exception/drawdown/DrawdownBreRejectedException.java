package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Exception thrown when a drawdown is rejected by the BRE (Business Rules Engine). */
public class DrawdownBreRejectedException extends BaseException {

  public DrawdownBreRejectedException(String message) {
    super(message, null, HttpStatus.BAD_REQUEST);
  }

  public DrawdownBreRejectedException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.BAD_REQUEST);
  }

  public DrawdownBreRejectedException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
