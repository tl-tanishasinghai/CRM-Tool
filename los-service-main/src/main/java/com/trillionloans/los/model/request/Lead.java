package com.trillionloans.los.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.trillionloans.los.model.dto.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Lead creation request body")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Lead {

  @Valid
  @NotNull(message = "[CreateLeadRequest] clientDetails object is required")
  @Schema(description = "Client personal information")
  private ClientDetailsDTO clientDetails;

  @Valid
  @Schema(description = "Address information")
  private List<AddressDetailsDTO> addressDetails;

  @Valid
  @Schema(description = "Family details of a client")
  private List<FamilyDetailsDTO> familyDetails;

  @Valid
  @Schema(description = "Identifier documents of a client")
  private List<ClientIdentifierDetailsDTO> clientIdentifierDetails;

  @Valid
  @Schema(description = "Bank details of a client")
  private List<BankDetailsDTO> bankDetails;

  @Valid
  @Schema(description = "Employment details of a client")
  private EmploymentDetailsDTO employmentDetails;

  @Schema(description = "Additional details for a client/lead")
  //  @Valid
  private List<AdditionalDetailsDTO> additionalDetails;

  @Schema(
      description = "Miscellaneous details as key-value pairs",
      example = "{\"detail1\": \"value1\", \"detail2\": \"value2\"}")
  private Map<String, String> miscellaneousDetails;
}
