package com.trillionloans.los.model.dto;

import jakarta.validation.constraints.NotBlank;
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
public class DocumentInfoDTO {
  @NotBlank(message = "[document] fileName is required")
  @Size(max = 100, message = "[document] fileName should be under 100 characters")
  private String fileName;

  @NotBlank(message = "[document] filePath is required")
  @Size(max = 500, message = "[document] fileName should be under 500 characters")
  private String filePath;

  private String fileType;
  private String storageType;
}
