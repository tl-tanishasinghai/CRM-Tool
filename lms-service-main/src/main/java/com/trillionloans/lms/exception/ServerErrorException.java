package com.trillionloans.lms.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
public class ServerErrorException extends RuntimeException {
  private final transient Object clientResponse;
  private final HttpStatusCode httpStatusCode;
  private final String url;

  public ServerErrorException(
      String errorMessage, Object clientResponse, HttpStatusCode httpStatusCode, String url) {
    super(errorMessage);
    this.clientResponse = clientResponse;
    this.httpStatusCode = httpStatusCode;
    this.url = url;
  }

  public ServerErrorException(
      String errorMessage, Object clientResponse, HttpStatusCode httpStatusCode) {
    this(errorMessage, clientResponse, httpStatusCode, null);
  }
}
