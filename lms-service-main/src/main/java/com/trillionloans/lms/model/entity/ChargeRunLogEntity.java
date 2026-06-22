package com.trillionloans.lms.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("charge_run_log")
public class ChargeRunLogEntity {
  @Id private Long id;

  private LocalDate runDate;
  private LocalDateTime startedAt;
  private LocalDateTime completedAt;
  private String status;
  private Long totalEmisProcessed;
  private Long totalChargesAttempted;
  private Long totalPosted;
  private Long totalSkipped;
  private Long totalPostingDisabled;
  private Long totalFailed;
  private String errorMessage;
}
