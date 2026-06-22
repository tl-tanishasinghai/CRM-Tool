package com.trillionloans.los.model.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.trillionloans.los.util.R2dbcJsonJacksonConfig;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
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
@Builder
@Table("drawdowns")
@NoArgsConstructor
@AllArgsConstructor
public class Drawdown {
  @Id private Long id;
  private String partnerId;
  private String anchorId;

  private BigDecimal amount;

  private DrawdownStatus status;

  private String transactionId;
  private String lineId;

  /** Client-provided idempotency key. Unique per drawdown. */
  @Column("external_id")
  private String externalId;

  @JsonSerialize(using = R2dbcJsonJacksonConfig.Serializer.class)
  @JsonDeserialize(using = R2dbcJsonJacksonConfig.Deserializer.class)
  private Json metadata;

  /** Timestamp when drawdown reached OPS_APPROVAL_PENDING status */
  @Column("ops_approval_pending_at")
  private LocalDateTime opsApprovalPendingAt;

  /**
   * Timestamp when drawdown reached a final status (SUCCESS, FAILED, BRE_REJECTED, OPS_REJECTED)
   */
  @Column("final_status_at")
  private LocalDateTime finalStatusAt;

  @Getter
  public enum DrawdownStatus {
    INIT("INIT"),
    BRE_INIT("BRE_INIT"),
    BRE_APPROVED("BRE_APPROVED"),
    BRE_REJECTED("BRE_REJECTED"),
    RISK_REJECTED("RISK_REJECTED"),
    OPS_APPROVAL_PENDING("OPS_APPROVAL_PENDING"),
    OPS_REJECTED("OPS_REJECTED"),
    FAILED("FAILED"),
    SUCCESS("SUCCESS");

    private final String description;

    DrawdownStatus(String description) {
      this.description = description;
    }

    /**
     * Checks if this status is a final status. Final statuses are: SUCCESS, FAILED, BRE_REJECTED,
     * RISK_REJECTED, OPS_REJECTED
     */
    public boolean isFinalStatus() {
      return this == SUCCESS
          || this == FAILED
          || this == BRE_REJECTED
          || this == RISK_REJECTED
          || this == OPS_REJECTED;
    }
  }
}
