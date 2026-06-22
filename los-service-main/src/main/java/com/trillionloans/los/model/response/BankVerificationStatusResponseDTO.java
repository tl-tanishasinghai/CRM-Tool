package com.trillionloans.los.model.response;

import com.trillionloans.los.constant.BankVerificationStatusEnum;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankVerificationStatusResponseDTO {

  private String leadId;
  private String bankAccountId;
  private BankVerificationStatusEnum bankVerificationStatus;
  private Boolean pennyDropStatus;
  private Double nameMatchPercentage;
  private Boolean nameIsValid;
  private List<String> failureReasons;
}
