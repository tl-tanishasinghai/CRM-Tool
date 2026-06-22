package com.trillionloans.los.exception.PanValidationExceptions;

public class PanResultsPersistenceException extends RuntimeException {
  public PanResultsPersistenceException(String message) {
    super(message);
  }

  public PanResultsPersistenceException(String message, Throwable cause) {
    super(message, cause);
  }
}
