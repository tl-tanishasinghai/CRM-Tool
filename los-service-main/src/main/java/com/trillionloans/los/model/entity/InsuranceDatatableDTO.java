package com.trillionloans.los.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Insurance Datatable DTO")
public class InsuranceDatatableDTO {

  @JsonProperty("id")
  @Schema(description = "Primary Key")
  private Integer id;

  @JsonProperty("loan_application_reference_id")
  @Schema(description = "Loan Application Identifier (Unique)")
  private Integer loanApplicationId;

  @JsonProperty("client_id")
  @Schema(description = "Client Identifier")
  private Integer clientId;

  @JsonProperty("is_charge_added")
  @Schema(description = "Whether charge is added")
  private Boolean isChargeAdded;

  @JsonProperty("is_opted")
  @Schema(description = "Whether user opted for insurance")
  private Boolean isOpted;

  @JsonProperty("premium_amount")
  @Schema(description = "Insurance premium amount")
  private Double premiumAmount;

  @JsonProperty("policy_no")
  @Schema(description = "Policy number received from Assurekit/M2P")
  private String policyNo;

  @JsonProperty("doc_url")
  @Schema(description = "URL of the insurance document")
  private String docUrl;

  @JsonProperty("mp_doc_id")
  @Schema(description = "Document ID provided by M2P")
  private Integer m2pDocId;

  @JsonProperty("status")
  @Schema(description = "Insurance status")
  private String status;

  @JsonProperty("created_at")
  @Schema(description = "Timestamp when record was created")
  private String createdAt;

  private String locale;

  @JsonProperty("dateFormat")
  private String dateFormat;
}
