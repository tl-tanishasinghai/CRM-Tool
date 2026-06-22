package com.trillionloans.los.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
public class ClientSideException extends RuntimeException {
  private final transient Object clientResponse;
  private final HttpStatusCode httpStatusCode;
  private final String url;

  public ClientSideException(String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    this(message, clientResponse, httpStatusCode, null);
  }

  public ClientSideException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode, String url) {
    super(message);
    this.clientResponse = clientResponse;
    this.httpStatusCode = httpStatusCode;
    this.url = url;
  }

  public Object getResponseBody() {
    return clientResponse;
  }
}
