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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Credit line mark repayment request body")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreditLineMarkRepaymentRequest {

  /**
   * Total repayment amount (sum of all emiDetails amounts). Optional from client; computed and set
   * before sending to M2P when not provided.
   */
  private Double amount;

  @NotEmpty(message = "[MarkRepayment] emiDetails list cannot be empty")
  @Valid
  @JsonAlias("emiDetail")
  private List<EmiDetail> emiDetails;

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
  @Schema(description = "EMI level repayment details")
  public static class EmiDetail {

    @NotBlank(message = "[MarkRepayment] emiTransactionId is required")
    @JsonAlias("transactionId")
    private String emiTransactionId;

    @NotNull(message = "[MarkRepayment] amount is required")
    private Double amount;
  }

  @Builder
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Schema(description = "Payment related details")
  public static class PaymentDetails {

    @NotNull(message = "[MarkRepayment] paymentType is required")
    private Integer paymentTypeId;

    @NotBlank(message = "[MarkRepayment] referenceNumber is required")
    private String referenceNumber;
  }
}
