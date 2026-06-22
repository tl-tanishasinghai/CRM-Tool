package com.trillionloans.los.model.request;

import com.trillionloans.los.constant.LeadDocumentTag;
import com.trillionloans.los.model.dto.DocumentInfoDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
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
@Schema(description = "Multiple documents upload request body")
public class LeadBulkDocumentsUploadRequest {
  @Valid private List<DocumentDetailsDTO> documents;

  @Builder
  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class DocumentDetailsDTO {

    @NotNull(message = "Tag is required")
    private LeadDocumentTag tag;

    @Valid
    @NotNull(message = "[documentUploadRequest] document is required")
    private DocumentInfoDTO document;
  }
}
