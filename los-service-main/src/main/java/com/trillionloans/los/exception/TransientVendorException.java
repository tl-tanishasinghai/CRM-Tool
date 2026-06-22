package com.trillionloans.los.exception;

public class TransientVendorException extends RuntimeException {
  public TransientVendorException(String message) {
    super(message);
  }
}
