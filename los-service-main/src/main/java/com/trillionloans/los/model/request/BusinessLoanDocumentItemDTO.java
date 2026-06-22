package com.trillionloans.los.model.request;

import com.trillionloans.los.constant.BusinessLoanAllDocumentTag;
import com.trillionloans.los.validation.EnumValidator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Business name, address, and M2P document reference for a tagged document")
public class BusinessLoanDocumentItemDTO {
  private String businessName;
  private String businessAddress;

  @Size(max = 20, message = "[businessLoanDocument] documentId must be at most 20 characters")
  private String documentId;

  @Schema(
      description =
          "Document tag; must be one of BUSINESS_LOAN_CONFIG allDocumentList "
              + "(UDYAM_CERTIFICATE, CPV, GST_REGISTRATION_CERTIFICATE)")
  @EnumValidator(
      enumClass = BusinessLoanAllDocumentTag.class,
      message =
          "[businessLoanDocument] tag must be one of UDYAM_CERTIFICATE, CPV,"
              + " GST_REGISTRATION_CERTIFICATE")
  private String tag;
}
