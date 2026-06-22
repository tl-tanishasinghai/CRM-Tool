package com.trillionloans.lms.model.response;

import com.trillionloans.lms.constant.ResponseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseDTO<T> {
  private ResponseStatus status;
  private String message;
  private String traceId;
  private T data;
}
