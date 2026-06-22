package com.trillionloans.los.model.response.ckyc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyntizenAuthResponse {

  private String respcode;

  private String authkey;

  private String authkeyexpriry;

  private String respdesc;
}
