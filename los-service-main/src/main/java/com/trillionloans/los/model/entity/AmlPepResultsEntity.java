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
@Table("aml_pep_results")
public class AmlPepResultsEntity {

  @Id
  @Column("id")
  private Long id;

  @Column("client_id")
  private Integer clientId;

  @Column("product_id")
  private String productId;

  @Column("lead_id")
  private String leadId;

  /** Raw request sent to AML/PEP service */
  @Column("request")
  private String request;

  /** Raw response received from AML/PEP service */
  @Column("response")
  private String response;

  @Column("service_status")
  private String serviceStatus;

  @Column("pep_decision")
  private String pepDecision;

  @Column("aml_decision")
  private String amlDecision;

  @Column("aml_fuzzy_match_score")
  private Double amlFuzzyMatchScore;

  @Column("final_status")
  private String finalStatus;

  @Column("reason_description")
  private String reasonDescription;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
