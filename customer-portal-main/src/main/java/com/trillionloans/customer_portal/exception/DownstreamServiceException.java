package com.trillionloans.customer_portal.exception;

import org.springframework.http.HttpStatus;

public class DownstreamServiceException extends RuntimeException {
  private final HttpStatus httpStatus;

  public DownstreamServiceException(String message, Throwable cause, HttpStatus status) {
    super(message, cause);
    this.httpStatus = status;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
