package com.trillionloans.los.model.request.creditline;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
public class DrawdownBreRequest {
  private Values values;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Values {
    private Input input;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Input {
    private String externalId;
    private String pancardDistributor;
    private String limitId;

    private List<InvoiceData> invoiceData;

    private BigDecimal requestedAmount;
    private String distributorCode;
    private Boolean invoiceNoRepeatFlag;
    private String loanType;

    private BigDecimal roi;
    private Integer tenure;

    private BigDecimal processingFee; // todo

    private String pincode;
    private Integer businessVintage;
    private String retailerName;
    private String distributorName;
    private String typeOfGoods;
    private String invoiceAttachment;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class InvoiceData {
    private String invoiceNumber;
    private LocalDate invoiceDate;
    private BigDecimal invoiceAmount;
    private String gstNumberInvoice;
  }
}
