package com.trillionloans.lms.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.codec.multipart.FilePart;

/** Represents a request body for uploading a document. */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Schema(description = "Document Upload request body")
public class DocumentUploadRequest {

  /** The name of the document. */
  private String name;

  /**
   * The tag identifier associated with the document.
   *
   * <p>This field is required and must not be blank.
   */
  @NotBlank(message = "tagIdentifier is required")
  private String tagIdentifier;

  /**
   * The file to be uploaded.
   *
   * <p>This field is required and must not be null.
   */
  @NotNull(message = "file is required")
  private FilePart file;
}
