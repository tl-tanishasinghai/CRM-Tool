package com.trillionloans.lms.model.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Entity representing the loan_restructure_eligibility_master table.
 *
 * @author Pawan Kumar
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("loan_restructure_eligibility_master")
public class LoanRestructureEligibilityMasterEntity {

  @Id
  @Column("id")
  private Long id;

  @Column("lan")
  private Long lan;

  @Column("lead_id")
  private Long leadId;

  @Column("client_id")
  private Long clientId;

  @Column("eligible")
  private Boolean eligible;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("last_updated_at")
  private LocalDateTime lastUpdatedAt;
}
