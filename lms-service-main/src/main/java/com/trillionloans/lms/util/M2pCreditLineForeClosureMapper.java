package com.trillionloans.lms.util;

import com.trillionloans.lms.model.dto.internal.M2pCreditLineForeClosureDTO;
import com.trillionloans.lms.model.dto.internal.M2pCreditLineForeClosurePayload;

public final class M2pCreditLineForeClosureMapper {

  private static final String M2P_PAYMENT_TYPE_ONLINE_TRANSFER = "Online transfer";

  private M2pCreditLineForeClosureMapper() {}

  public static M2pCreditLineForeClosurePayload toM2pPayload(M2pCreditLineForeClosureDTO dto) {
    if (dto == null) {
      return null;
    }
    return M2pCreditLineForeClosurePayload.builder()
        .transactionTime(dto.getTransactionTime())
        .paymentDetails(
            M2pCreditLineForeClosurePayload.PaymentDetails.builder()
                .paymentType(M2P_PAYMENT_TYPE_ONLINE_TRANSFER)
                .referenceNumber(dto.getNotes())
                .build())
        .build();
  }
}
