package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class KycUploadDocumentRequest {

  private String name;
  private String tagIdentifier;
  private String side;
  private FilePart file;
}
