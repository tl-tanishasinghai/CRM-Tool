package com.trillionloans.los.model.dto;

import com.trillionloans.los.constant.QcCheckStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QcCheckResult {
  private QcCheckStatus status;
  private String reason;

  public static QcCheckResult approved() {
    return QcCheckResult.builder().status(QcCheckStatus.APPROVED).build();
  }

  public static QcCheckResult rejected(String reason) {
    return QcCheckResult.builder().status(QcCheckStatus.REJECTED).reason(reason).build();
  }

  public static QcCheckResult pending(String reason) {
    return QcCheckResult.builder().status(QcCheckStatus.PENDING).reason(reason).build();
  }
}
