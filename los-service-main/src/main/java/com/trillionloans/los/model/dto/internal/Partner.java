package com.trillionloans.los.model.dto.internal;

import com.trillionloans.los.constant.IsActiveStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "partner details")
public class Partner {

  @NotEmpty(message = "[partnerDetails] partnerId is required")
  @Size(max = 10, message = "[partnerDetails] partnerId should be under 10 characters")
  private String partnerId;

  @NotEmpty(message = "[partnerDetails] partnerName is required")
  @Size(max = 100, message = "[partnerDetails] partnerName should be under 100 characters")
  private String partnerName;

  @NotEmpty(message = "[partnerDetails] productCode is required")
  @Size(max = 100, message = "[partnerDetails] productCode should be under 100 characters")
  private String productCode;

  @NotEmpty(message = "[partnerDetails] productName is required")
  @Size(max = 100, message = "[partnerDetails] productName should be under 100 characters")
  private String productName;

  @NotEmpty(message = "[partnerDetails] productType is required")
  @Size(max = 100, message = "[partnerDetails] productType should be under 100 characters")
  private String productType;

  @NotNull(message = "[partnerDetails] status is required")
  private IsActiveStatus status;
}
