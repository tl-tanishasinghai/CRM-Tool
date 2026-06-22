package com.trillionloans.los.model.dto;

import com.trillionloans.los.constant.Gender;
import com.trillionloans.los.constant.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Schema(description = "Family details for a lead")
public class FamilyDetailsDTO {
  private String firstName;
  private String lastName;
  private String dateOfBirth;
  private String documentType;
  private String documentKey;
  private RelationshipType relationship;
  private Gender gender;
}
