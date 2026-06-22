package com.trillionloans.los.exception;

public class PiiDataConverterException extends RuntimeException {
  public PiiDataConverterException(String message, Throwable t) {
    super(message, t);
  }
}
