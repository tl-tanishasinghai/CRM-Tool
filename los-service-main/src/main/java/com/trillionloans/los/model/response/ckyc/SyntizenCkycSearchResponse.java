package com.trillionloans.los.model.response.ckyc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SyntizenCkycSearchResponse {

  private String respcode;
  private String respdesc;
  private String rrn;
  private String name;
  private String father_name;
}
