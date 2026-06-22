package com.trillionloans.customer_portal.model.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FreshdeskCategoryFieldResponse {
  private List<Choice> choices;

  @Getter
  @Setter
  public static class Choice {
    private String label;
  }
}
