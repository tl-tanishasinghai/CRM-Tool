package com.trillionloans.lms.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
public class CustomException extends RuntimeException {
  private final transient Object clientResponse;
  private final HttpStatusCode httpStatusCode;

  public CustomException(String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    super(message);
    this.clientResponse = clientResponse;
    this.httpStatusCode = httpStatusCode;
  }
}
