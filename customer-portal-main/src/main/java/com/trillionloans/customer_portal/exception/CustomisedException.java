package com.trillionloans.customer_portal.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public class CustomisedException extends RuntimeException {

  private final String message;

  /** -- GETTER -- Retrieves the HTTP status associated with this exception. */
  @Getter private final HttpStatus httpStatus;

  @Getter private final String loggerHeader;

  /**
   * Constructs a new CustomException with the specified message and HTTP status.
   *
   * @param message the detail message (which is saved for later retrieval by the getMessage()
   *     method).
   * @param httpStatus the HTTP status associated with this exception.
   */
  public CustomisedException(String message, HttpStatus httpStatus, String loggerHeader) {
    this.message = message;
    this.httpStatus = httpStatus;
    this.loggerHeader = loggerHeader;
  }

  /**
   * Retrieves the detail message of this exception.
   *
   * @return the detail message.
   */
  @Override
  public String getMessage() {
    return message;
  }
}
