package com.trillionloans.los.model.dto;

import com.trillionloans.los.model.request.m2p.M2pBankDetailsRequestDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "bank details for a lead")
public class BankDetailsDTO {
  private String accountType;

  @Size(max = 150, message = "[bankDetails] name should be under 150 characters")
  //  @Pattern(
  //      regexp = "^[a-zA-Z .&()'-]+$",
  //      message = "[bankDetails] accountName contains invalid characters")
  private String name;

  @Pattern(
      regexp = "^[0-9a-zA-Z ]+$",
      message = "[bankDetails] accountNumber contains invalid characters")
  private String accountNumber;

  @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "[bankDetails] invalid ifsc code")
  private String ifscCode;

  private Boolean supportedForRepayment;
  private Boolean supportedForDisbursement;

  public M2pBankDetailsRequestDTO getM2pRequestDTO() {
    return M2pBankDetailsRequestDTO.builder()
        .accountTypeId(accountType)
        .name(this.name)
        .accountNumber(this.accountNumber)
        .ifscCode(this.ifscCode)
        .supportedForDisbursement(this.supportedForDisbursement)
        .supportedForRepayment(this.supportedForRepayment)
        .build();
  }
}
