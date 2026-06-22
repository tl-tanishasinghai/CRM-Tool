package com.trillionloans.los.model.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "lead_acknowledgement")
public class LeadAcknowledgement {

  @Id
  @Column("id")
  private Long id;

  @Column("loan_application_id")
  private String loanId;

  @Column("acknowledgement_status")
  private String acknowledgementStatus;

  @Column("error_message")
  private String errorMessage;

  @CreatedDate
  @Column("acknowledgement_time")
  private LocalDateTime acknowledgementTime;
}
