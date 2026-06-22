package com.trillionloans.lms.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class M2pLoanForeClosureDTO {
  private String dateFormat;
  private String locale;
  private String note;

  @JsonProperty("preclosureReasonId")
  @SerializedName("preclosureReasonId")
  private int preClosureReasonId;

  private String transactionAmount;
  private String transactionDate;
  private int paymentTypeId;
  private String interestWaiverAmount;
  private String receiptNumber;
  private List<Object> chargeDiscountDetails;
  private List<Object> waiveCharges;
}
