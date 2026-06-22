package com.trillionloans.lms.repository;

import com.trillionloans.lms.model.entity.ReKycTrackerEntity;
import java.time.LocalDate;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReKycTrackerRepository extends R2dbcRepository<ReKycTrackerEntity, Long> {

  Flux<ReKycTrackerEntity> findAllByIsActiveTrue();

  /** Handles if lan_id or kyc_due_date changes. */
  @Modifying
  @Query(
      """
INSERT INTO re_kyc_tracker (
    client_id,
    lan_id,
    product_id,
    client_name,
    mobile_no,
    disbursal_date,
    re_kyc_due_date,
    eligible_sms_code,
    is_active,
    is_written_off,
    dpd_days
)
VALUES (
    :clientId,
    :lanId,
    :productId,
    :clientName,
    :mobileNo,
    :disbursalDate,
    :dueDate,
    :eligibleSmsCode,
    :isActive,
    :isWrittenOff,
    :dpdDays
)
ON CONFLICT (client_id)
DO UPDATE SET
    lan_id            = EXCLUDED.lan_id,
    product_id      = EXCLUDED.product_id,
    client_name       = EXCLUDED.client_name,
    mobile_no         = EXCLUDED.mobile_no,
    disbursal_date    = EXCLUDED.disbursal_date,
    re_kyc_due_date   = EXCLUDED.re_kyc_due_date,
    eligible_sms_code = EXCLUDED.eligible_sms_code,
    is_active         = EXCLUDED.is_active,
    is_written_off    = EXCLUDED.is_written_off,
    dpd_days          = EXCLUDED.dpd_days,
    updated_at        = CURRENT_TIMESTAMP,

    last_trigger_code = CASE
        WHEN re_kyc_tracker.lan_id IS DISTINCT FROM EXCLUDED.lan_id
        THEN NULL
        ELSE re_kyc_tracker.last_trigger_code
    END,

    last_sent_at = CASE
        WHEN re_kyc_tracker.lan_id IS DISTINCT FROM EXCLUDED.lan_id
        THEN NULL
        ELSE re_kyc_tracker.last_sent_at
    END

WHERE
       re_kyc_tracker.lan_id           IS DISTINCT FROM EXCLUDED.lan_id
    OR re_kyc_tracker.product_id    IS DISTINCT FROM EXCLUDED.product_id
    OR re_kyc_tracker.client_name     IS DISTINCT FROM EXCLUDED.client_name
    OR re_kyc_tracker.mobile_no       IS DISTINCT FROM EXCLUDED.mobile_no
    OR re_kyc_tracker.disbursal_date  IS DISTINCT FROM EXCLUDED.disbursal_date
    OR re_kyc_tracker.re_kyc_due_date IS DISTINCT FROM EXCLUDED.re_kyc_due_date
    OR re_kyc_tracker.eligible_sms_code        IS DISTINCT FROM EXCLUDED.eligible_sms_code
    OR re_kyc_tracker.is_active       IS DISTINCT FROM EXCLUDED.is_active
    OR re_kyc_tracker.is_written_off  IS DISTINCT FROM EXCLUDED.is_written_off
    OR re_kyc_tracker.dpd_days        IS DISTINCT FROM EXCLUDED.dpd_days
""")
  Mono<Integer> upsertTracker(
      String clientId,
      String lanId,
      Integer productId,
      String clientName,
      String mobileNo,
      LocalDate disbursalDate,
      LocalDate dueDate,
      String eligibleSmsCode,
      Boolean isActive,
      Boolean isWrittenOff,
      Integer dpdDays);

  /** Updates milestone after Kafka event is published. */
  @Modifying
  @Query(
      """
          UPDATE re_kyc_tracker
          SET last_trigger_code = :code,
              last_sent_at = CURRENT_TIMESTAMP,
              updated_at = CURRENT_TIMESTAMP
          WHERE client_id = :clientId
      """)
  Mono<Integer> updateTriggerStatus(String clientId, String code);

  @Modifying
  @Query(
      """
          UPDATE re_kyc_tracker
          SET is_active = FALSE,
              updated_at = CURRENT_TIMESTAMP
          WHERE is_active = TRUE
            AND updated_at < CURRENT_DATE
      """)
  Mono<Integer> deactivateOldRecords();
}
