package com.trillionloans.lms.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class M2pPartRepaymentRequestDTO {
  private Double amount;
  private String partPaymentDate;
  private String dateFormat;
  private String locale;
  private String notes;
  private Double amountToBeRetainedInExcess;
  private PaymentDetails paymentDetails;

  @NoArgsConstructor
  @AllArgsConstructor
  @Getter
  @Setter
  @Builder
  private static class PaymentDetails {
    private int paymentTypeId;
  }

  public static M2pPartRepaymentRequestDTO getM2pRequestFormatFromDTO(
      PartRepaymentRequestDTO partRepaymentRequestDTO) {
    return M2pPartRepaymentRequestDTO.builder()
        .amount(partRepaymentRequestDTO.getAmount())
        .partPaymentDate(partRepaymentRequestDTO.getPartPaymentDate())
        .dateFormat("dd-MM-yyyy")
        .locale("en")
        .notes(partRepaymentRequestDTO.getNotes())
        .amountToBeRetainedInExcess(0.0)
        .paymentDetails(
            PaymentDetails.builder()
                .paymentTypeId(partRepaymentRequestDTO.getPaymentDetails().getPaymentTypeId())
                .build())
        .build();
  }
}
