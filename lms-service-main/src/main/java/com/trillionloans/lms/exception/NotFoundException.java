package com.trillionloans.lms.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
@Setter
public class NotFoundException extends ResponseStatusException {
  private final String url;

  public NotFoundException(String message, String url) {
    super(HttpStatus.NOT_FOUND, message);
    this.url = url;
  }

  public NotFoundException(String message) {
    super(HttpStatus.NOT_FOUND, message);
    this.url = null;
  }
}
