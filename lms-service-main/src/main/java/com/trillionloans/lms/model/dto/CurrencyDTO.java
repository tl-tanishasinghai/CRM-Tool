package com.trillionloans.lms.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object (DTO) representing currency details for the collection module.
 *
 * @author sofiyan
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Currency DTO for collection module")
public class CurrencyDTO {

  private String code;
  private String name;
  private int decimalPlaces;
  private int inMultiplesOf;
  private String nameCode;
  private String displayLabel;
}
