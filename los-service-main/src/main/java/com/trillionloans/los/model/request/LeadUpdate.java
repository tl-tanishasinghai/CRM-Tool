package com.trillionloans.los.model.request;

import com.trillionloans.los.model.dto.AdditionalDetailsDTO;
import com.trillionloans.los.model.dto.AddressDetailsDTO;
import com.trillionloans.los.model.dto.BankDetailsDTO;
import com.trillionloans.los.model.dto.ClientDetailsUpdateDTO;
import com.trillionloans.los.model.dto.EmploymentDetailsUpdateDTO;
import com.trillionloans.los.model.dto.FamilyDetailsDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Lead update request body")
public class LeadUpdate {
  @Valid
  @Schema(description = "Client personal information")
  private ClientDetailsUpdateDTO clientDetails;

  @Valid
  @Schema(description = "Address information")
  private List<AddressDetailsDTO> addressDetails;

  @Valid
  @Schema(description = "Family details of a client")
  private List<FamilyDetailsDTO> familyDetails;

  @Valid
  @Schema(description = "Bank details of a client")
  private List<BankDetailsDTO> bankDetails;

  @Valid
  @Schema(description = "Employment details of a client")
  private EmploymentDetailsUpdateDTO employmentDetails;

  @Valid
  @Schema(description = "Additional details for a client/lead")
  private List<AdditionalDetailsDTO> additionalDetails;
}
