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

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("loan_restructure_notification_tracking")
public class LoanRestructureNotificationTrackingEntity {

  @Id
  @Column("id")
  private Long id;

  @Column("restructure_details_id")
  private Long restructureDetailsId;

  @Column("loan_account_number")
  private Long loanAccountNumber;

  @Column("status")
  private String status;

  @Column("attempt_count")
  private Integer attemptCount;

  @Column("last_error")
  private String lastError;

  @Column("s3_file_path")
  private String s3FilePath;

  @Column("customer_name")
  private String customerName;

  @Column("mobile_number")
  private String mobileNumber;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
