package com.trillionloans.los.model.request;

import com.trillionloans.los.constant.DocumentTag;
import com.trillionloans.los.constant.LoanType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Multiple documents upload request body")
public class BulkDocumentsUploadRequest {
  @Valid private List<DocumentDetailsDTO> documents;

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class DocumentDetailsDTO {

    @NotNull(message = "Tag is required")
    private DocumentTag tag;

    @Valid
    @NotNull(message = "[documentUploadRequest] document is required")
    private DocumentInfoDTO document;

    @Schema(
        description =
            "Optional when tag is LOAN_AGREEMENT. If set, must be LoanType BUSINESS_LOAN or"
                + " MERCHANT_LOAN only. Stored as lsp_status; not sent to M2P. Must be omitted for"
                + " other tags.",
        allowableValues = {"BUSINESS_LOAN", "MERCHANT_LOAN"})
    private String loanStatus;

    /**
     * When {@code tag} is {@link DocumentTag#LOAN_AGREEMENT}, {@code loanStatus} must be unset or
     * one of {@link LoanType#BUSINESS_LOAN} / {@link LoanType#MERCHANT_LOAN}. For any other tag,
     * {@code loanStatus} must not be sent.
     */
    @AssertTrue(
        message =
            "loanStatus is only valid with tag LOAN_AGREEMENT and must be BUSINESS_LOAN or"
                + " MERCHANT_LOAN (LoanType enum names)")
    private boolean isLoanStatusValidForTag() {
      if (tag == DocumentTag.LOAN_AGREEMENT) {
        if (loanStatus == null || loanStatus.isBlank()) {
          return true;
        }
        String normalized = loanStatus.trim();
        return LoanType.BUSINESS_LOAN.name().equals(normalized)
            || LoanType.MERCHANT_LOAN.name().equals(normalized);
      }
      return loanStatus == null || loanStatus.isBlank();
    }
  }

  @Builder
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class DocumentInfoDTO {
    @NotBlank(message = "[document] fileName is required")
    @Size(max = 100, message = "[document] fileName should be under 100 characters")
    private String fileName;

    private String encodedFile;

    @NotBlank(message = "[document] filePath is required")
    @Size(max = 2000, message = "[document] filePath should be under 2000 characters")
    private String filePath;

    private String fileType;
    private String storageType;

    /** S3 path (key) of the uploaded document. Set internally after Digio/S3 upload. */
    private String s3Path;
  }
}
