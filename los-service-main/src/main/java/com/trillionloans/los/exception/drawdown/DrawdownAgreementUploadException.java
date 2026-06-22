package com.trillionloans.los.exception.drawdown;

import com.trillionloans.los.exception.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** Exception thrown when drawdown agreement document upload fails. */
public class DrawdownAgreementUploadException extends BaseException {

  public DrawdownAgreementUploadException(String message) {
    super(message, null, HttpStatus.BAD_REQUEST);
  }

  public DrawdownAgreementUploadException(String message, Object clientResponse) {
    super(message, clientResponse, HttpStatus.BAD_REQUEST);
  }

  public DrawdownAgreementUploadException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message, clientResponse, httpStatusCode);
  }
}
