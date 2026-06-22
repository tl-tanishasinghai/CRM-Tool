package com.trillionloans.los.model.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "rules details")
public class RuleDTO {
  @NotNull private String name;

  private String description;

  @NotNull private String productCode;

  @NotNull @NotEmpty private String type;

  @NotNull
  @Min(0)
  private Integer priority;

  @NotNull @NotEmpty private String condition;

  @NotNull @NotEmpty private String action;

  private Boolean active = true;
}
