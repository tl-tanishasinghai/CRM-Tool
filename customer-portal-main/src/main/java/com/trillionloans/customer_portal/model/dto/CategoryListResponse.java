package com.trillionloans.customer_portal.model.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryListResponse {
  private List<String> categories;
}
