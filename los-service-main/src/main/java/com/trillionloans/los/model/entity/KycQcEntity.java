package com.trillionloans.los.model.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("kyc_qc")
public class KycQcEntity {

  // indexing, primary, unique, non-nullable
  @Id
  @Column("id")
  private Long id;

  // indexing, nullable
  @Column("loan_application_id")
  private String loanId;

  // indexing, nullable
  @Column("client_id")
  private String clientId;

  // nullable
  @Column("xml_ts")
  private String xmlTs;

  // nullable
  @Column("final_name_match_score")
  private Double finalNameMatchScore;

  // nullable
  @Column("final_face_match_score")
  private Double finalFaceMatchScore;

  // nullable
  @Column("karza_name_match_score")
  private Double karzaNameMatchScore;

  // nullable
  @Column("karza_face_match_score")
  private Double karzaFaceMatchScore;

  // nullable
  @Column("analytic_name_match_score")
  private Double analyticNameMatchScore;

  // nullable
  @Column("analytic_face_match_score")
  private Double analyticFaceMatchScore;

  // indexing
  @Column("final_name_match_status")
  private String finalNameMatchStatus;

  // indexing
  @Column("final_face_match_status")
  private String finalFaceMatchStatus;

  // indexing
  @Column("xml_validity_status")
  private String xmlValidityStatus;

  @Column("kyc_type")
  private String kycType;

  // indexing
  @Column("product_code")
  private String productCode;

  // indexing
  @Column("created_at")
  private LocalDateTime createdAt;

  // indexing, nullable
  @Column("updated_at")
  private LocalDateTime updatedAt;

  // auto-incrementing version field
  // default value is 1
  @Version
  @Column("version")
  private Integer version;

  @Builder.Default
  @Column("is_deleted")
  private Boolean isDeleted = false;
}
