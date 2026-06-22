package com.trillionloans.los.model.entity;

import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("client_creation_request_details")
public class ClientCreationRequestDetail {
  @Id private Long id;

  @Column("client_id")
  private String clientId;

  @Column("product_code")
  private String productCode;

  @Column("first_name")
  private String firstName;

  @Column("middle_name")
  private String middleName;

  @Column("last_name")
  private String lastName;

  private String gender;

  @Column("date_of_birth")
  private String dateOfBirth;

  private String email;

  @Column("mobile_no")
  private String mobileNo;

  @Column("alternate_mobile_no")
  private String alternateMobileNo;

  private String education;

  @Column("external_id")
  private String externalId;

  @Column("address_details")
  private Json addressDetails;

  @Column("family_details")
  private Json familyDetails;

  @Column("client_identifier_details")
  private Json clientIdentifierDetails;

  @Column("bank_details")
  private Json bankDetails;

  @Column("employment_details")
  private Json employmentDetails;

  @Column("additional_details")
  private Json additionalDetails;

  @CreatedDate
  @Column("created_at")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column("updated_at")
  private LocalDateTime updatedAt;
}
