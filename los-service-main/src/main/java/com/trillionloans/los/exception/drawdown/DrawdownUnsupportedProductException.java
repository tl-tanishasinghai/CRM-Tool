package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Exception thrown when the provided product code is not supported for drawdown. */
public class DrawdownUnsupportedProductException extends BaseException {

  public DrawdownUnsupportedProductException(String message) {
    super(message, null, HttpStatus.BAD_REQUEST);
  }

  public DrawdownUnsupportedProductException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.BAD_REQUEST);
  }

  public DrawdownUnsupportedProductException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
