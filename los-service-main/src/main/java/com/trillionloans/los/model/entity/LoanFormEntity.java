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
@Table("loan_form")
public class LoanFormEntity {

  @Id private Integer id;

  @Column("first_name")
  private String firstName;

  @Column("last_name")
  private String lastName;

  @Column("email")
  private String email;

  @Column("phone")
  private String phone;

  @Column("loan_type")
  private String loanType;

  @Column("consent")
  private Boolean consent;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @Column("is_deleted")
  private Integer isDeleted;
}
