package com.trillionloans.los.model.request.ckyc;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class SyntizenCkycSearchRequest {

  @JsonProperty("idnumber")
  private String idNumber;

  @JsonProperty("idtype")
  private String idType;

  private UUID rrn;
}
