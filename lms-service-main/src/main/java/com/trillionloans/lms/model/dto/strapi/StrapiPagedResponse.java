package com.trillionloans.lms.model.dto.strapi;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StrapiPagedResponse<T> {
  private List<T> data;
}
