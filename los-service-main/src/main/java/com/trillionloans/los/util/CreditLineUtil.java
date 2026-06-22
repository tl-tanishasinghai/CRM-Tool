package com.trillionloans.los.util;

import com.trillionloans.los.constant.CreditLineStatus;
import com.trillionloans.los.model.entity.CreditLineEntity;
import com.trillionloans.los.model.request.CreditLineLoanApplication;
import com.trillionloans.los.model.request.LoanApplication;
import com.trillionloans.los.model.response.CreditLineCallbackToPartnerDTO;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Utility class for Credit Line operations.
 *
 * <p>Contains stateless helper methods for credit line status management, date conversions, and DTO
 * building.
 */
public final class CreditLineUtil {

  private CreditLineUtil() {}

  /**
   * Returns the priority of a credit line status in the lifecycle. Higher value means further along
   * in the process.
   *
   * @param status the credit line status
   * @return priority value (0-3)
   */
  public static int getStatusPriority(String status) {
    if (status == null) {
      return 0;
    }
    if (CreditLineStatus.OPS_APPROVED.getValue().equals(status)) {
      return 0;
    }
    if (CreditLineStatus.CREATED.getValue().equals(status)) {
      return 1;
    }
    if (CreditLineStatus.APPROVED.getValue().equals(status)) {
      return 2;
    }
    if (CreditLineStatus.ACTIVE.getValue().equals(status)) {
      return 3;
    }
    return 0;
  }

  /**
   * Checks if M2P status is ahead of local status in the credit line lifecycle.
   *
   * @param localStatus the local status
   * @param m2pStatus the M2P status
   * @return true if M2P status is ahead
   */
  public static boolean isM2PAhead(String localStatus, String m2pStatus) {
    return getStatusPriority(m2pStatus) > getStatusPriority(localStatus);
  }

  /**
   * Converts epoch milliseconds to LocalDateTime.
   *
   * @param epochMs epoch milliseconds
   * @return LocalDateTime or null if epochMs is null
   */
  public static LocalDateTime epochMsToLocalDateTime(Long epochMs) {
    if (epochMs == null) {
      return null;
    }
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
  }

  /**
   * Checks if the globalisation code indicates credit line not found.
   *
   * @param code the globalisation code
   * @return true if code contains "creditline.not.found"
   */
  public static boolean containsCreditLineNotFound(String code) {
    return code != null && code.contains("creditline.not.found");
  }

  /**
   * Builds a CreditLineCallbackToPartnerDTO from a CreditLineEntity.
   *
   * @param creditLineEntity the credit line entity
   * @return the callback DTO
   */
  public static CreditLineCallbackToPartnerDTO buildCreditLineStatusDto(
      CreditLineEntity creditLineEntity) {
    return CreditLineCallbackToPartnerDTO.builder()
        .limitId(creditLineEntity.getM2pCreditLineId())
        .limit(
            creditLineEntity.getCreditLimit() != null
                ? creditLineEntity.getCreditLimit().intValue()
                : null)
        .tenureDetails(
            CreditLineCallbackToPartnerDTO.TenureDetails.builder()
                .value(creditLineEntity.getTenureValue())
                .type(creditLineEntity.getTenureType())
                .build())
        .status(creditLineEntity.getStatus())
        .leadId(creditLineEntity.getLeadId())
        .build();
  }

  /**
   * Maps CreditLineLoanApplication to LoanApplication using productCode from request header.
   *
   * @param creditLineRequest the credit line loan application request
   * @param productCode the product code from request header
   * @return LoanApplication with productCode as losProductKey
   */
  public static LoanApplication mapToLoanApplication(
      CreditLineLoanApplication creditLineRequest, String productCode) {
    return LoanApplication.builder()
        .externalId(creditLineRequest.getExternalId())
        .losProductKey(productCode)
        .leadApplicationTerms(null)
        .amount(null)
        .associations(null)
        .charges(null)
        .isTopup(null)
        .loanPurposeId(null)
        .loanOfficerId(null)
        .sourcingChannelId(null)
        .loanIdToClose(null)
        .build();
  }
}
