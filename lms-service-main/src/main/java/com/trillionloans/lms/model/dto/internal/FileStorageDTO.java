package com.trillionloans.lms.model.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Data Transfer Object representing file storage details in the system. */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "file details")
public class FileStorageDTO {
  private String fileName;
  private String s3Url;
  private String contentType;
}
