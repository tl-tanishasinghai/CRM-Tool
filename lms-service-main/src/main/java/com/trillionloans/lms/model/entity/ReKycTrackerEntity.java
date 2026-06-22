package com.trillionloans.lms.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("re_kyc_tracker")
public class ReKycTrackerEntity {

  @Id
  @Column("id")
  private Long id;

  @Column("client_id")
  private String clientId;

  @Column("lan_id")
  private String lanId;

  @Column("product_id")
  private String productId;

  @Column("client_name")
  private String clientName;

  @Column("mobile_no")
  private String mobileNo;

  @Column("disbursal_date")
  private LocalDate disbursalDate;

  @Column("re_kyc_due_date")
  private LocalDate reKycDueDate;

  @Column("eligible_sms_code")
  private String eligibleSmsCode;

  @Column("last_trigger_code")
  private String lastTriggerCode;

  @Column("last_sent_at")
  private LocalDateTime lastSentAt;

  @Builder.Default
  @Column("is_active")
  private Boolean isActive = true;

  @Builder.Default
  @Column("is_written_off")
  private Boolean isWrittenOff = false;

  @Column("dpd_days")
  private Integer dpdDays;

  @ReadOnlyProperty
  @Column("created_at")
  private LocalDateTime createdAt;

  @ReadOnlyProperty
  @Column("updated_at")
  private LocalDateTime updatedAt;
}
