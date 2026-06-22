package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class PanDetails {

  @JsonProperty("id_number")
  private String idNumber;

  @JsonProperty("document_type")
  private String documentType;

  @JsonProperty("id_proof_type")
  private String idProofType;

  private String gender;
  private String name;
  private String dob;

  @JsonProperty("loan_application_id")
  private String loanApplicationId;

  private final String locale = "en";
  private final String dateFormat = "dd MMMM yyyy";
}
