package com.trillionloans.los.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
public class CustomTimeoutException extends RuntimeException {
  private final transient Object clientResponse;
  private final HttpStatusCode httpStatusCode;

  public CustomTimeoutException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message);
    this.httpStatusCode = httpStatusCode;
    this.clientResponse = clientResponse;
  }
}
