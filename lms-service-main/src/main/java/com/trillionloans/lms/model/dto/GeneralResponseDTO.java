package com.trillionloans.lms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing a general response for the collection module.
 *
 * @author sofiyan
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "General Response DTO for collection module")
public class GeneralResponseDTO {

  private Long id;
  private String code;
  private String value;
}
