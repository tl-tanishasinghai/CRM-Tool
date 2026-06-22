package com.trillionloans.los.model.dto;

import com.trillionloans.los.model.request.DrawdownRequest;
import java.util.Collections;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DrawdownInternalRequest extends DrawdownRequest {
  //  private MerchantDetails merchantDetails;
  private DrawdownInternalRequest.PaymentDetails paymentDetails;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MerchantDetails {
    private String merchantId;
    private String merchantName;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaymentDetails {
    private String paymentType;
    private Integer paymentTypeId;
  }

  public static DrawdownInternalRequest from(
      DrawdownRequest parent, MerchantDetails merchantDetails, PaymentDetails paymentDetails) {

    if (parent == null) {
      log.error("Parent DrawdownRequest cannot be null.");
      throw new IllegalArgumentException("Parent DrawdownRequest cannot be null");
    }

    return DrawdownInternalRequest.builder()
        .anchorId(parent.getAnchorId())
        .partnerId(parent.getPartnerId())
        .externalId(parent.getExternalId())
        .invoiceData(Optional.ofNullable(parent.getInvoiceData()).orElse(Collections.emptyList()))
        .drawdownData(parent.getDrawdownData())
        // Child field
        //        .merchantDetails(merchantDetails)
        .paymentDetails(paymentDetails)
        .build();
  }
}
