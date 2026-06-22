package com.trillionloans.los.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SelfieUpload {
  @NotBlank(message = "fileName is required")
  private String fileName;

  @NotBlank(message = "fileContent is required")
  private String fileContent;

  private Boolean doLiveliness;
  private String fileType;
  private String storageType;
  private String score;
  private String ip;

  @DecimalMin(value = "-180.0", message = "Longitude must be >= -180.0")
  @DecimalMax(value = "180.0", message = "Longitude must be <= 180.0")
  private Double longitude;

  @DecimalMin(value = "-90.0", message = "Latitude must be >= -90.0")
  @DecimalMax(value = "90.0", message = "Latitude must be <= 90.0")
  private Double latitude;
}
