package com.trillionloans.customer_portal.exception;

import static com.trillionloans.customer_portal.constant.StringConstants.JSON_PROCESSING_EXCEPTION_MESSAGE;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class JsonProcessingFailureException extends ResponseStatusException {

  public JsonProcessingFailureException(String exceptionMessage) {
    super(
        HttpStatus.INTERNAL_SERVER_ERROR,
        String.format(JSON_PROCESSING_EXCEPTION_MESSAGE, exceptionMessage));
  }

  public JsonProcessingFailureException(String exceptionMessage, Throwable cause) {
    super(
        HttpStatus.INTERNAL_SERVER_ERROR,
        String.format(JSON_PROCESSING_EXCEPTION_MESSAGE, exceptionMessage),
        cause);
  }
}
