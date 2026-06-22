package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Exception thrown when a drawdown is not eligible for an operation (e.g. approve/reject when not
 * in OPS_APPROVAL_PENDING state).
 */
public class DrawdownNotEligibleException extends BaseException {

  public DrawdownNotEligibleException(String message) {
    super(message, null, HttpStatus.BAD_REQUEST);
  }

  public DrawdownNotEligibleException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.BAD_REQUEST);
  }

  public DrawdownNotEligibleException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
