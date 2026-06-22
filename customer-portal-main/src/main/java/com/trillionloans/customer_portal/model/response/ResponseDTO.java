package com.trillionloans.customer_portal.model.response;

import com.trillionloans.customer_portal.constant.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO<T> {
  private ResponseStatus status;
  private String message;
  private String traceId;
  private T data;
}
