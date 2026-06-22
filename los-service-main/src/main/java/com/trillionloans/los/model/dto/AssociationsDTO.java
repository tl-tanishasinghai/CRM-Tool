package com.trillionloans.los.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Associations for a loan application")
public class AssociationsDTO {
  private String anchor;
  private String merchant;
  private String thirdParty;
  private String self;
}
