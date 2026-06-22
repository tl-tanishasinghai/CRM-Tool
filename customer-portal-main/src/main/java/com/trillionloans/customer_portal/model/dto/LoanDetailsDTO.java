package com.trillionloans.customer_portal.model.dto;

import java.math.BigInteger;
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
public class LoanDetailsDTO {

  private String loanAccountNumber;
  private String status;
  private double loanAmount;
  private String loanTenure;
  private double chargesPreDisbursal;
  private double emiAmount;
  private String disbursementDate;
  private double netDisbursementAmount;
  private String lendingServiceProvider;
  private Double chargesPostDisbursal;
  private String interestRate;
  private BigInteger loanApplicationId;
  private BigInteger productId;
  private String logo;

  private String lastBureauReportingDate;
  private LastPaymentDone lastPaymentDone;
  private NextPaymentDue nextPaymentDue;
  private double totalPrincipalOutstanding;
  private Integer dpdDays;
  private String productName;
  private CollectionDetailsResponse collectionDetails;
  private RepaymentDetails currentDueSplit;
  private RepaymentDetails nextDueSplit;
  private RepaymentDetails foreclosureSplit;

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class LastPaymentDone {
    private Double amount;
    private String date;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class NextPaymentDue {
    private Double amount;
    private String date;
  }
}
