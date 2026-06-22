package com.trillionloans.los.model.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("partnership_form")
public class PartnershipFormEntity {

  @Id private Integer id;

  @Column("partnership_type")
  private String partnershipType;

  @Column("first_name")
  private String firstName;

  @Column("last_name")
  private String lastName;

  @Column("email")
  private String email;

  @Column("phone")
  private String phone;

  @Column("organization_name")
  private String organizationName;

  @Column("designation_name")
  private String designationName;

  @Column("consent")
  private Boolean consent;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @Column("is_deleted")
  private Integer isDeleted;
}
