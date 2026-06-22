package com.trillionloans.los.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClientCacheDTO {
  private Integer clientId;
  private String productCode;

  private String firstName;
  private String middleName;
  private String lastName;

  private String fatherFirstName;
  private String fatherLastName;

  private String dateOfBirth;
  private String panNumber;
}
