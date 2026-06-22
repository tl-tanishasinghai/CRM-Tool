package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Business loan details for update loan request")
public class BusinessLoanDetailsDTO {
  private String businessName;
  private String businessAddress;

  @Schema(
      description = "Per-tag document lines (business name/address from doc, M2P document id, tag)")
  @Valid
  private List<BusinessLoanDocumentItemDTO> businessLoanDocuments;
}
