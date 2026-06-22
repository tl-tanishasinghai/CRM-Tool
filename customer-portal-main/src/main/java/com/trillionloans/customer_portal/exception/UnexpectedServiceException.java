package com.trillionloans.customer_portal.exception;

import org.springframework.http.HttpStatus;

public class UnexpectedServiceException extends BaseException {
  public UnexpectedServiceException(String message, Throwable cause, HttpStatus status) {
    super(message, cause, status);
  }
}
