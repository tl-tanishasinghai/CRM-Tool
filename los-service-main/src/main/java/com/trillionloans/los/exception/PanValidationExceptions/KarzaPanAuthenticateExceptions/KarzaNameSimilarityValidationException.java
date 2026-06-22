package com.trillionloans.los.exception.PanValidationExceptions.KarzaPanAuthenticateExceptions;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatusCode;

@Getter
@Setter
public class KarzaNameSimilarityValidationException extends RuntimeException {
  private final transient Object response;
  private final HttpStatusCode httpStatusCode;

  public KarzaNameSimilarityValidationException(
      String message, Object response, HttpStatusCode httpStatusCode) {
    super(message);
    this.response = response;
    this.httpStatusCode = httpStatusCode;
  }
}
