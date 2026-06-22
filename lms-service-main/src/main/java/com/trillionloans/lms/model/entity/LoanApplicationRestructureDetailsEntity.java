package com.trillionloans.lms.model.entity;

import io.r2dbc.postgresql.codec.Json;
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
 * Entity representing the loan_application_restructure_details table.
 *
 * @author Pawan Kumar
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("loan_application_restructure_details")
public class LoanApplicationRestructureDetailsEntity {

  @Id
  @Column("id")
  private Long id;

  @Column("lan")
  private Long lan;

  @Column("lead")
  private Long lead;

  @Column("client")
  private Long client;

  @Column("eligibility")
  private Boolean eligibility;

  @Column("eligibility_data")
  private Json eligibilityData;

  @Column("restructure")
  private String restructure;

  @Column("restructure_id")
  private Long restructureId;

  @Column("customer_name")
  private String customerName;

  @Column("mobile_number")
  private String mobileNumber;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @Column("approved_on")
  private LocalDateTime approvedOn;

  @Column("signed_doc_id")
  private String signedDocId;

  @Column("signed_url")
  private String signedUrl;
}
