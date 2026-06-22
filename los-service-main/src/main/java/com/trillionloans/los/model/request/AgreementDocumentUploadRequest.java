package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Agreement Document Upload request body")
public class AgreementDocumentUploadRequest {
  private String name;

  @NotBlank(message = "tagIdentifier is required")
  private String tagIdentifier;

  @NotNull(message = "file is required")
  private FilePart file;
}
