package com.trillionloans.lms.util;

import com.trillionloans.lms.model.entity.CreditLineMarkRepaymentRecord;
import com.trillionloans.lms.model.request.CreditLineMarkRepaymentRequest;
import com.trillionloans.lms.model.response.CreditLineMarkRepaymentResponse;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class CreditLineUtil {

  private CreditLineUtil() {}

  public record RepaymentGroupResponse(
      String drawdownTransactionId,
      Object drawdownDetails, // Populated from M2P for the drawdown
      BigDecimal totalRepaidAmount,
      Integer repaymentCount,
      List<RepaymentDetailDTO> repaymentRecords) {}

  public record RepaymentDetailDTO(
      String transactionId,
      BigDecimal amount,
      Long transactionTime,
      String referenceNumber,
      Object repaymentDetails // Populated from M2P for each repayment
      ) {}

  /** Set of product codes that are classified as Credit Line products. */
  private static final Set<String> CREDIT_LINE_PRODUCT_CODES = Set.of("FUND", "KCL");

  /**
   * Checks if the given product code is a Credit Line product.
   *
   * @param productCode the product code to check
   * @return true if the product is a Credit Line product, false otherwise
   */
  public static boolean isCreditLineProduct(String productCode) {
    if (productCode == null) {
      return false;
    }
    return CREDIT_LINE_PRODUCT_CODES.contains(productCode.trim().toUpperCase());
  }

  public static List<CreditLineMarkRepaymentRecord> createRepaymentEntities(
      String lineId,
      CreditLineMarkRepaymentRequest request,
      CreditLineMarkRepaymentResponse response) {

    if (Objects.isNull(request) || Objects.isNull(request.getEmiDetails())) {
      return Collections.emptyList();
    }

    var paymentDetails = Optional.ofNullable(request.getPaymentDetails());

    Integer paymentTypeId =
        paymentDetails
            .map(CreditLineMarkRepaymentRequest.PaymentDetails::getPaymentTypeId)
            .orElse(0);

    String refNumber =
        paymentDetails
            .map(CreditLineMarkRepaymentRequest.PaymentDetails::getReferenceNumber)
            .orElse(StringUtils.EMPTY);

    Long txnTime = Optional.ofNullable(request.getTransactionTime()).orElse(0L);
    String safeLineId = Optional.ofNullable(lineId).orElse(StringUtils.EMPTY);
    String transactionId = (response != null) ? response.getTransactionId() : StringUtils.EMPTY;

    return request.getEmiDetails().stream()
        .map(
            emi ->
                CreditLineMarkRepaymentRecord.builder()
                    .lineId(safeLineId)
                    .emiTransactionId(
                        Optional.ofNullable(emi.getEmiTransactionId()).orElse(StringUtils.EMPTY))
                    .amount(BigDecimal.valueOf(Optional.ofNullable(emi.getAmount()).orElse(0.0)))
                    .transactionId(transactionId)
                    .paymentTypeId(paymentTypeId)
                    .referenceNumber(refNumber)
                    .transactionTime(txnTime)
                    .build())
        .toList();
  }

  public static boolean applyResultFilters(
      CreditLineUtil.RepaymentGroupResponse group, String drawdownId, String transactionId) {
    if (transactionId != null && !transactionId.isBlank()) {
      return group.repaymentRecords().stream()
          .anyMatch(r -> r.transactionId().equals(transactionId));
    }
    if (drawdownId != null && !drawdownId.isBlank()) {
      return group.drawdownTransactionId().equals(drawdownId);
    }
    return true;
  }

  public static String extractIdentifier(Object apiTxn) {
    if (apiTxn instanceof Map<?, ?> map) {
      Object id = map.get("transactionIdentifier");
      return id != null ? id.toString() : null;
    }
    return null;
  }
}
