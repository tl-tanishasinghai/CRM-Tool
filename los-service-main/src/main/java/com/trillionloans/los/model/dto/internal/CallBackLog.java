package com.trillionloans.los.model.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a log entry for callback events. This class captures the details of a callback,
 * including its request, response, and any associated errors.
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CallBackLog {

  /** The code of the product associated with the callback. */
  private String productCode;

  /** The type of the callback (e.g., notification, update, etc.). */
  private String type;

  /** The request payload sent in the callback. */
  private Object request;

  /** A unique reference identifier for tracking the callback. */
  private String referenceId;

  /** The response payload received from the callback. */
  private Object response;

  /** Any exception that occurred during the callback process. */
  private String exception;

  /** The URI endpoint of the callback. */
  private String uri;

  /** The timestamp when the callback was created, typically in ISO 8601 format. */
  private String createdAt;
}
