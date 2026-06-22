package com.trillionloans.los.model.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object representing log event details. This class encapsulates the data related to
 * a specific log event, including metadata and request/response information.
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "log details")
public class LogEventsDTO {

  private String requestId; // Maps to "request_id"
  private String traceId; // Maps to "trace_id"
  private String serviceName; // Maps to "service_name"
  private String partnerId; // Maps to "partner_id"
  private String method; // Maps to "method"
  private String uri; // Maps to "uri"
  private String loggerHeader; // Maps to "loggerHeader"
  private Object requestBody; // Maps to "request_body"
  private Object responseBody; // Maps to "response_body"
  private String statusCode; // Maps to "status_code"
  private Long processingTimeMs; // Maps to "processing_time_ms"
  private LocalDateTime createdAt; // Maps to "created_at"
  private String sourceApplication; // Maps to "sourceApplication"
  private String destinationApplication; // Maps to "destinationApplication"
}
