package com.trillionloans.los.model.response.m2p;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class M2pErrorResponseDTO {
  private String developerMessage;
  private String httpStatusCode;
  private String defaultUserMessage;
  private String userMessageGlobalisationCode;
  private List<ErrorDetailDTO> errors;
  private String errorCode;

  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  public static class ErrorDetailDTO {
    private String developerMessage;
    private String defaultUserMessage;
    private String userMessageGlobalisationCode;
    private String parameterName;
    private List<ArgumentDTO> args;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Data
  @Builder
  public static class ArgumentDTO {
    private Object value;
  }
}
