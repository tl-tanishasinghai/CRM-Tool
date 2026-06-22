package com.trillionloans.los.exception.PanValidationExceptions;

public class BuildOpvRequestException extends RuntimeException {
  public BuildOpvRequestException(String message) {
    super(message);
  }

  public BuildOpvRequestException(String message, Throwable cause) {
    super(message, cause);
  }
}
