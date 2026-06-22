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
@Table("loan_level_client_details")
public class LoanLevelClientDetail {
  @Id private Long id;

  @Column("client_creation_reference_id")
  private String clientCreationReferenceId;

  @Column("loan_application_id")
  private String loanApplicationId;

  @Column("client_id")
  private String clientId;

  @Column("product_code")
  private String productCode;

  @Column("loan_application_reference_no")
  private String loanApplicationReferenceNo;

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

  @Column("aadhaar_name")
  private String aadhaarName;

  @Column("aadhaar_dob")
  private String aadhaarDob;

  @Column("aadhaar_house")
  private String aadhaarHouse;

  @Column("aadhaar_street")
  private String aadhaarStreet;

  @Column("aadhaar_landmark")
  private String aadhaarLandmark;

  @Column("aadhaar_locality")
  private String aadhaarLocality;

  @Column("aadhaar_vtc")
  private String aadhaarVtc;

  @Column("aadhaar_sub_district")
  private String aadhaarSubDistrict;

  @Column("aadhaar_district")
  private String aadhaarDistrict;

  @Column("aadhaar_state")
  private String aadhaarState;

  @Column("aadhaar_pincode")
  private String aadhaarPincode;

  @Column("aadhaar_country")
  private String aadhaarCountry;

  @Column("aadhaar_dependent")
  private String aadhaarDependent;

  @Column("aadhaar_care_of")
  private String aadhaarCareOf;

  @CreatedDate
  @Column("created_at")
  private LocalDateTime createdAt;

  @LastModifiedDate
  @Column("updated_at")
  private LocalDateTime updatedAt;
}
