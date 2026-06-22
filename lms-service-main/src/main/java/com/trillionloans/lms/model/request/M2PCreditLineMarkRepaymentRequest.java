package com.trillionloans.lms.model.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound M2P credit-line mark repayment body: uses {@code paymentType} (string) per M2P contract,
 * mapped from {@link CreditLineMarkRepaymentRequest}'s {@code paymentTypeId}.
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "M2P credit line mark repayment request body")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class M2PCreditLineMarkRepaymentRequest {

  private Double amount;

  @NotEmpty(message = "[MarkRepayment] emiDetails list cannot be empty")
  @Valid
  @JsonAlias("emiDetail")
  private List<CreditLineMarkRepaymentRequest.EmiDetail> emiDetails;

  @NotNull(message = "[MarkRepayment] paymentDetails is required")
  @Valid
  private PaymentDetails paymentDetails;

  @NotNull(message = "[MarkRepayment] transactionTime is required")
  @Schema(description = "Transaction time in epoch milliseconds", example = "1723262261000")
  private Long transactionTime;

  @Builder
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Schema(description = "Payment related details for M2P")
  public static class PaymentDetails {

    @NotBlank(message = "[MarkRepayment] paymentType is required")
    private String paymentType;

    @NotBlank(message = "[MarkRepayment] referenceNumber is required")
    private String referenceNumber;
  }

  private static final Map<Integer, String> PAYMENT_TYPE_ID_TO_M2P_LABEL =
      Map.of(1, "Online transfer");

  /**
   * Builds the M2P payload from the API request, mapping {@code paymentTypeId} to M2P {@code
   * paymentType} strings.
   *
   * @throws IllegalArgumentException if {@code paymentTypeId} is not mapped for M2P
   */
  public static M2PCreditLineMarkRepaymentRequest from(CreditLineMarkRepaymentRequest source) {
    if (source.getPaymentDetails() == null) {
      throw new IllegalArgumentException("paymentDetails is required");
    }
    Integer paymentTypeId = source.getPaymentDetails().getPaymentTypeId();
    String paymentType = PAYMENT_TYPE_ID_TO_M2P_LABEL.get(paymentTypeId);
    if (paymentType == null) {
      throw new IllegalArgumentException(
          "Unsupported paymentTypeId for M2P credit line mark repayment: " + paymentTypeId);
    }
    return M2PCreditLineMarkRepaymentRequest.builder()
        .amount(source.getAmount())
        .emiDetails(source.getEmiDetails())
        .paymentDetails(
            PaymentDetails.builder()
                .paymentType(paymentType)
                .referenceNumber(source.getPaymentDetails().getReferenceNumber())
                .build())
        .transactionTime(source.getTransactionTime())
        .build();
  }
}
