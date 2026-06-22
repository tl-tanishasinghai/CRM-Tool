package com.trillionloans.los.model.request.m2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One row of business-loan classification detail posted to M2P (matches reporting SQL shape). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanClassificationDetailsM2pLine {

  @JsonProperty("application_business_name")
  private String applicationBusinessName;

  @JsonProperty("application_business_address")
  private String applicationBusinessAddress;

  @JsonProperty("document_name")
  private String documentName;

  @JsonProperty("document_business_name")
  private String documentBusinessName;

  @JsonProperty("document_business_address")
  private String documentBusinessAddress;

  @JsonProperty("document_number")
  private String documentNumber;

  @JsonProperty("name_match_score")
  private BigDecimal nameMatchScore;

  @JsonProperty("address_match_score")
  private BigDecimal addressMatchScore;

  @JsonProperty("is_uploaded")
  private Boolean isUploaded;

  /** evaluation_status from LOS; serialized as is_eligible for M2P payload compatibility. */
  @JsonProperty("is_eligible")
  private String isEligible;
}
