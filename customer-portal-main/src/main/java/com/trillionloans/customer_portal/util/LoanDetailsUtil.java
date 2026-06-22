package com.trillionloans.customer_portal.util;

import static com.trillionloans.customer_portal.constant.LoanStatus.getStatusNameByCode;
import static com.trillionloans.customer_portal.constant.StringConstants.DD_MM_YYYY;
import static com.trillionloans.customer_portal.constant.StringConstants.INVALID_INPUT;
import static com.trillionloans.customer_portal.util.DateTimeUtil.convertDate;

import com.trillionloans.customer_portal.constant.*;
import com.trillionloans.customer_portal.model.dto.LoanDetailsDTO;
import com.trillionloans.customer_portal.model.dto.LoanDetailsResponse;
import com.trillionloans.customer_portal.model.dto.SimplifiedTransactionResponse;
import com.trillionloans.customer_portal.model.dto.TransactionDetailResponse;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import reactor.core.publisher.Mono;

public class LoanDetailsUtil {

  private LoanDetailsUtil() {}

  public static Mono<SimplifiedTransactionResponse.Transaction> processTransactionAsync(
      TransactionDetailResponse.Transaction transaction) {
    // Simulate async processing for each transaction and return a Mono
    if (transaction.getManuallyReversed()
        || !Set.of(2L, 9L, 17L).contains(transaction.getType().getId())) {
      return Mono.empty(); // Skip the transaction by returning an empty Mono
    }

    return Mono.just(
        SimplifiedTransactionResponse.Transaction.builder()
            .amount(transaction.getAmount())
            .date(
                LocalDate.of(
                        transaction.getDate().get(0),
                        transaction.getDate().get(1),
                        transaction.getDate().get(2))
                    .format(DateTimeFormatter.ofPattern(DD_MM_YYYY)))
            .value(transaction.getTxnValueDateStatus().getValue())
            .build());
  }

  public static String getRepaymentPeriodString(
      BigInteger tenure, int repaymentPeriodFrequencyValue) {
    if (tenure == null) {
      return INVALID_INPUT;
    }
    RepaymentPeriodFrequency repaymentPeriodFrequency =
        RepaymentPeriodFrequency.fromValue(repaymentPeriodFrequencyValue);
    String periodString = tenure + " " + repaymentPeriodFrequency.getPeriodName();
    if (tenure.intValue() > 1) {
      periodString += "s";
    }
    return periodString;
  }

  public static LoanDetailsDTO mapLoanToDto(LoanDetailsResponse loan) {
    LoanDetailsDTO loanDetailsDto = new LoanDetailsDTO();
    loanDetailsDto.setLoanAccountNumber(loan.getLoanAccountNumber());
    loanDetailsDto.setStatus(getStatusNameByCode(loan.getStatus().intValue()));
    loanDetailsDto.setLoanAmount(loan.getLoanAmount());
    loanDetailsDto.setChargesPreDisbursal(loan.getChargesPreDisbursal());

    loanDetailsDto.setEmiAmount(loan.getEmiAmount());
    loanDetailsDto.setDisbursementDate(convertDate(loan.getDisbursementDate()));
    loanDetailsDto.setNetDisbursementAmount(loan.getNetDisbursementAmount());
    loanDetailsDto.setProductId(loan.getProductId());
    loanDetailsDto.setInterestRate(loan.getRateOfInterest() + "%");
    loanDetailsDto.setChargesPostDisbursal(loan.getChargesPostDisbursal());
    loanDetailsDto.setLoanApplicationId(loan.getLoanApplicationId());
    BigInteger repaymentFrequencyEnum = loan.getRepaymentPeriodFrequencyEnum();
    BigInteger tenure = loan.getTenure();
    loanDetailsDto.setLoanTenure(
        getRepaymentPeriodString(tenure, repaymentFrequencyEnum.intValue()));
    BigInteger productId = loan.getProductId();
    String lendingServiceProvider =
        ProductOfficeMapping.getOfficeNameByProductId(productId.intValue());
    loanDetailsDto.setLendingServiceProvider(lendingServiceProvider);
    String logo = OfficeLogoMapping.getUrlByOfficeName(lendingServiceProvider);
    loanDetailsDto.setLogo(logo);
    loanDetailsDto.setProductName(
        ProductNameMapping.getProductNameByProductId(productId.toString()));

    loanDetailsDto.setTotalPrincipalOutstanding(loan.getTotalPrincipalOutstanding());

    loanDetailsDto.setLastBureauReportingDate(
        populateLastBureauReportingDateBasedOnLoginDate(LocalDate.now()));
    return loanDetailsDto;
  }

  private static String populateLastBureauReportingDateBasedOnLoginDate(LocalDate loginDate) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    if (loginDate.getDayOfMonth() <= 15) {
      LocalDate previousMonthDate = loginDate.minusMonths(1).withDayOfMonth(22);
      return previousMonthDate.format(formatter);
    } else {
      LocalDate currentMonthDate = loginDate.withDayOfMonth(7);
      return currentMonthDate.format(formatter);
    }
  }
}
