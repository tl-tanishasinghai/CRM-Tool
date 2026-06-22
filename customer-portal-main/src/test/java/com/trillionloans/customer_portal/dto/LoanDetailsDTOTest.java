package com.trillionloans.customer_portal.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.trillionloans.customer_portal.model.dto.LoanDetailsDTO;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class LoanDetailsDTOTest {

  @Test
  void testAllArgsConstructorUsingBuilder() {
    LoanDetailsDTO.LastPaymentDone lastPayment =
        LoanDetailsDTO.LastPaymentDone.builder().amount(1500.00).date("2025-04-10").build();

    LoanDetailsDTO.NextPaymentDue nextPayment =
        LoanDetailsDTO.NextPaymentDue.builder().amount(1500.00).date("2025-05-10").build();

    LoanDetailsDTO loanDetails =
        LoanDetailsDTO.builder()
            .loanAccountNumber("LN123456")
            .status("Active")
            .loanAmount(50000.00)
            .loanTenure("36 Months")
            .chargesPreDisbursal(2000.00)
            .emiAmount(1500.00)
            .disbursementDate("2025-01-15")
            .netDisbursementAmount(48000.00)
            .lendingServiceProvider("ABC Bank")
            .chargesPostDisbursal(500.00)
            .interestRate("10%")
            .loanApplicationId(new BigInteger("100001"))
            .productId(new BigInteger("5001"))
            .logo("logoUrl")
            .lastBureauReportingDate("test")
            .lastPaymentDone(lastPayment)
            .nextPaymentDue(nextPayment)
            .totalPrincipalOutstanding(35000.00)
            .productName(null)
            .build();

    assertEquals("LN123456", loanDetails.getLoanAccountNumber());
    assertEquals("Active", loanDetails.getStatus());
    assertEquals(50000.00, loanDetails.getLoanAmount());
    assertEquals("36 Months", loanDetails.getLoanTenure());
    assertEquals(2000.00, loanDetails.getChargesPreDisbursal());
    assertEquals(1500.00, loanDetails.getEmiAmount());
    assertEquals("2025-01-15", loanDetails.getDisbursementDate());
    assertEquals(48000.00, loanDetails.getNetDisbursementAmount());
    assertEquals("ABC Bank", loanDetails.getLendingServiceProvider());
    assertEquals(500.00, loanDetails.getChargesPostDisbursal());
    assertEquals("10%", loanDetails.getInterestRate());
    assertEquals(new BigInteger("100001"), loanDetails.getLoanApplicationId());
    assertEquals(new BigInteger("5001"), loanDetails.getProductId());
    assertEquals("logoUrl", loanDetails.getLogo());

    assertEquals("test", loanDetails.getLastBureauReportingDate());

    assertNotNull(loanDetails.getLastPaymentDone());
    assertEquals(1500.00, loanDetails.getLastPaymentDone().getAmount());
    assertEquals("2025-04-10", loanDetails.getLastPaymentDone().getDate());

    assertNotNull(loanDetails.getNextPaymentDue());
    assertEquals(1500.00, loanDetails.getNextPaymentDue().getAmount());
    assertEquals("2025-05-10", loanDetails.getNextPaymentDue().getDate());

    assertEquals(35000.00, loanDetails.getTotalPrincipalOutstanding());
  }

  @Test
  void testNoArgsConstructor() {
    // Create a LoanDetailsDTO object using no-args constructor
    LoanDetailsDTO loanDetails = new LoanDetailsDTO();

    assertNull(loanDetails.getLoanAccountNumber());
    assertNull(loanDetails.getStatus());
    assertEquals(0.0, loanDetails.getLoanAmount());
    assertNull(loanDetails.getLoanTenure());
    assertEquals(0.0, loanDetails.getChargesPreDisbursal());
    assertEquals(0.0, loanDetails.getEmiAmount());
    assertNull(loanDetails.getDisbursementDate());
    assertEquals(0.0, loanDetails.getNetDisbursementAmount());
    assertNull(loanDetails.getLendingServiceProvider());
    assertNull(loanDetails.getChargesPostDisbursal());
    assertNull(loanDetails.getInterestRate());
    assertNull(loanDetails.getLoanApplicationId());
    assertNull(loanDetails.getProductId());
    assertNull(loanDetails.getLogo());
  }

  @Test
  void testGetterSetter() {
    LoanDetailsDTO loanDetails = new LoanDetailsDTO();

    // Set values using setters
    loanDetails.setLoanAccountNumber("LN987654");
    loanDetails.setStatus("Disbursed");
    loanDetails.setLoanAmount(100000.00);
    loanDetails.setLoanTenure("24 Months");
    loanDetails.setChargesPreDisbursal(1000.00);
    loanDetails.setEmiAmount(5000.00);
    loanDetails.setDisbursementDate("2025-06-01");
    loanDetails.setNetDisbursementAmount(95000.00);
    loanDetails.setLendingServiceProvider("XYZ Bank");
    loanDetails.setChargesPostDisbursal(300.00);
    loanDetails.setInterestRate("8%");
    loanDetails.setLoanApplicationId(new BigInteger("123456"));
    loanDetails.setProductId(new BigInteger("7890"));
    loanDetails.setLogo("logoUrl");

    // Assertions to verify getters return the correct values
    assertEquals("LN987654", loanDetails.getLoanAccountNumber());
    assertEquals("Disbursed", loanDetails.getStatus());
    assertEquals(100000.00, loanDetails.getLoanAmount());
    assertEquals("24 Months", loanDetails.getLoanTenure());
    assertEquals(1000.00, loanDetails.getChargesPreDisbursal());
    assertEquals(5000.00, loanDetails.getEmiAmount());
    assertEquals("2025-06-01", loanDetails.getDisbursementDate());
    assertEquals(95000.00, loanDetails.getNetDisbursementAmount());
    assertEquals("XYZ Bank", loanDetails.getLendingServiceProvider());
    assertEquals(300.00, loanDetails.getChargesPostDisbursal());
    assertEquals("8%", loanDetails.getInterestRate());
    assertEquals(new BigInteger("123456"), loanDetails.getLoanApplicationId());
    assertEquals(new BigInteger("7890"), loanDetails.getProductId());
    assertEquals("logoUrl", loanDetails.getLogo());
  }
}
