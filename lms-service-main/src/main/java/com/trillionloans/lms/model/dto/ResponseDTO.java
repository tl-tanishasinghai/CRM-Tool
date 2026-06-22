package com.trillionloans.lms.model.dto;

import com.trillionloans.lms.constant.ResponseStatus;
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
  private String traceId;
  private String message;
  private T data;
}
