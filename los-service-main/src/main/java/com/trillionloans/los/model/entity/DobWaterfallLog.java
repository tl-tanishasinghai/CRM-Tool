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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("dob_waterfall_result")
public class DobWaterfallLog {
  @Id
  @Column("id")
  Long id;

  @Column("client_id")
  String clientId;

  @Column("product_code")
  String productCode;

  @Column("result")
  String result;

  @Column("pan_dob")
  String panDob;

  @Column("aadhar_dob")
  String aadharDob;

  @Column("rejection_reason")
  String rejectionReason;

  @Column("created_at")
  LocalDateTime createdAt;
}
