package com.trillionloans.los.model.dto;

import com.trillionloans.los.constant.AddressType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Address details DTO")
public class AddressDetailsDTO {
  @NotNull(message = "[addressDetails] addressType is required")
  private List<AddressType> addressType;

  @Size(max = 200, message = "[addressDetails] addressLineOne should be under 200 characters")
  private String addressLineOne;

  @Size(max = 200, message = "[addressDetails] addressLineTwo should be under 200 characters")
  private String addressLineTwo;

  private String landmark;

  @NotEmpty(message = "[addressDetails] postalCode is required")
  private String postalCode;

  private String ownershipType;
}
