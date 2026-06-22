package com.trillionloans.los.exception;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class BatchDownloadException extends RuntimeException {
  private final transient Object clientResponse;
  private final HttpStatusCode httpStatusCode;
  private final String url;

  public BatchDownloadException(
      String message, Object clientResponse, HttpStatusCode httpStatusCode) {
    this(message, clientResponse, httpStatusCode, null);
  }

  public BatchDownloadException(
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
