package com.trillionloans.los.constant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(name = "relationship types", description = "Enum values for relationship types")
public enum RelationshipType {
  FATHER("Father"),
  SPOUSE("Spouse"),
  MOTHER("Mother"),
  HUSBAND("Husband"),
  SON("Son"),
  DAUGHTER("Daughter"),
  WIFE("Wife"),
  BROTHER("Brother");

  private final String displayName;

  RelationshipType(String displayName) {
    this.displayName = displayName;
  }
}
