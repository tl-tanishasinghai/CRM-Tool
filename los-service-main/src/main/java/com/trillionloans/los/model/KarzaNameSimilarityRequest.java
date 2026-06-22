package com.trillionloans.los.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KarzaNameSimilarityRequest {
  private String name1;
  private String name2;
  private Type type;
  private String preset;
  private boolean allowPartialMatch;
  private boolean suppressReorderPenalty;
  private ClientData clientData;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ClientData {
    private String caseId;
  }

  public enum Type {
    individual,
  }
}
