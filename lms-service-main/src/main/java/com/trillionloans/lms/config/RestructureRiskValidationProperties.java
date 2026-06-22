package com.trillionloans.lms.config;

import java.time.LocalDate;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for loan restructure risk validations.
 *
 * @author Pawan Kumar
 */
@Component
@Getter
@Setter
@ConfigurationProperties("restructure.risk-validation")
public class RestructureRiskValidationProperties {

  /** Start date for actual disbursement date range (inclusive). Format: yyyy-MM-dd */
  private LocalDate disbursementStart = LocalDate.of(2025, 1, 1);

  /** End date for actual disbursement date range (inclusive). Format: yyyy-MM-dd */
  private LocalDate disbursementEnd = LocalDate.of(2025, 8, 31);

  /** Minimum DPD (Days Past Due) - dpdDays must be >= this value */
  private int dpdMin = 10;

  /** Maximum DPD (exclusive) - dpdDays must be < this value */
  private int dpdMaxExclusive = 60;

  /** DPD reject threshold - dpdDays must not be >= this value */
  private int dpdRejectThreshold = 90;

  /** Minimum number of paid repayments (EDI paid condition) */
  private int minPaidRepayments = 30;

  /**
   * Tenure grid for restructure (e.g. 120,180,270,360,450,540). Configured via
   * restructure.risk-validation.tenure-options in yml/env.
   */
  private List<Integer> tenureOptions;

  /** Reschedule reason ID for M2P API */
  private Long rescheduleReasonId = 331L;
}
