package com.trillionloans.customer_portal.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.trillionloans.customer_portal.model.dto.LoanDetailsResponse;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class LoanDetailsResponseTest {

  @Test
  void testNoArgsConstructor() {
    // Create a LoanDetailsResponse object using no-args constructor
    LoanDetailsResponse loanDetailsResponse = new LoanDetailsResponse();

    // Assertions to verify default values (all fields should be null or 0.0)
    assertNull(loanDetailsResponse.getLoanAccountNumber());
    assertNull(loanDetailsResponse.getStatus());
    assertEquals(0.0, loanDetailsResponse.getLoanAmount());
    assertNull(loanDetailsResponse.getTenure());
    assertEquals(0.0, loanDetailsResponse.getChargesPreDisbursal());
    assertEquals(0.0, loanDetailsResponse.getEmiAmount());
    assertNull(loanDetailsResponse.getDisbursementDate());
    assertEquals(0.0, loanDetailsResponse.getNetDisbursementAmount());
    assertNull(loanDetailsResponse.getProductId());
    assertEquals(0.0, loanDetailsResponse.getRateOfInterest());
    assertNull(loanDetailsResponse.getRepaymentPeriodFrequencyEnum());
    assertNull(loanDetailsResponse.getOfficeName());
    assertEquals(0.0, loanDetailsResponse.getChargesPostDisbursal());
    assertNull(loanDetailsResponse.getLoanApplicationId());
  }

  @Test
  void testGetterSetter() {
    // Create a LoanDetailsResponse object using no-args constructor
    LoanDetailsResponse loanDetailsResponse = new LoanDetailsResponse();

    // Set values using setters
    loanDetailsResponse.setLoanAccountNumber("LN987654");
    loanDetailsResponse.setStatus(new BigInteger("2"));
    loanDetailsResponse.setLoanAmount(100000.00);
    loanDetailsResponse.setTenure(new BigInteger("24"));
    loanDetailsResponse.setChargesPreDisbursal(1000.00);
    loanDetailsResponse.setEmiAmount(5000.00);
    loanDetailsResponse.setDisbursementDate("2025-06-01");
    loanDetailsResponse.setNetDisbursementAmount(95000.00);
    loanDetailsResponse.setProductId(new BigInteger("2002"));
    loanDetailsResponse.setRateOfInterest(8.0);
    loanDetailsResponse.setRepaymentPeriodFrequencyEnum(new BigInteger("6"));
    loanDetailsResponse.setOfficeName("XYZ Bank");
    loanDetailsResponse.setChargesPostDisbursal(300.00);
    loanDetailsResponse.setLoanApplicationId(new BigInteger("123456"));

    // Assertions to verify getters return the correct values
    assertEquals("LN987654", loanDetailsResponse.getLoanAccountNumber());
    assertEquals(new BigInteger("2"), loanDetailsResponse.getStatus());
    assertEquals(100000.00, loanDetailsResponse.getLoanAmount());
    assertEquals(new BigInteger("24"), loanDetailsResponse.getTenure());
    assertEquals(1000.00, loanDetailsResponse.getChargesPreDisbursal());
    assertEquals(5000.00, loanDetailsResponse.getEmiAmount());
    assertEquals("2025-06-01", loanDetailsResponse.getDisbursementDate());
    assertEquals(95000.00, loanDetailsResponse.getNetDisbursementAmount());
    assertEquals(new BigInteger("2002"), loanDetailsResponse.getProductId());
    assertEquals(8.0, loanDetailsResponse.getRateOfInterest());
    assertEquals(new BigInteger("6"), loanDetailsResponse.getRepaymentPeriodFrequencyEnum());
    assertEquals("XYZ Bank", loanDetailsResponse.getOfficeName());
    assertEquals(300.00, loanDetailsResponse.getChargesPostDisbursal());
    assertEquals(new BigInteger("123456"), loanDetailsResponse.getLoanApplicationId());
  }
}
