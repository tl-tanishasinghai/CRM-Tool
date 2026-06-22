package com.trillionloans.customer_portal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.trillionloans.customer_portal.constant.OfficeLogoMapping;
import com.trillionloans.customer_portal.constant.ProductOfficeMapping;
import com.trillionloans.customer_portal.model.dto.LoanDetailsDTO;
import com.trillionloans.customer_portal.model.dto.LoanDetailsResponse;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

class LoanDetailsUtilTest {

  @Test
  void testGetRepaymentPeriodString() {
    // Test case 1: Valid tenure with frequency
    BigInteger tenure = new BigInteger("12");
    int repaymentPeriodFrequencyValue = 1;
    String result = LoanDetailsUtil.getRepaymentPeriodString(tenure, repaymentPeriodFrequencyValue);
    assertEquals("12 Weeks", result);

    // Test case 2: Tenure = 1
    tenure = new BigInteger("1");
    result = LoanDetailsUtil.getRepaymentPeriodString(tenure, repaymentPeriodFrequencyValue);
    assertEquals("1 Week", result);

    // Test case 3: Tenure is null
    result = LoanDetailsUtil.getRepaymentPeriodString(null, repaymentPeriodFrequencyValue);
    assertEquals("Invalid input", result);
  }

  @Test
  void testMapLoanToDto() {
    // Create a mock LoanDetailsResponse
    LoanDetailsResponse mockLoan = mock(LoanDetailsResponse.class);

    // Mocking the LoanDetailsResponse fields
    when(mockLoan.getLoanAccountNumber()).thenReturn("LN12345");
    when(mockLoan.getStatus()).thenReturn(new BigInteger("1"));
    when(mockLoan.getLoanAmount()).thenReturn(10000.00);
    when(mockLoan.getEmiAmount()).thenReturn(1000.00);
    when(mockLoan.getDisbursementDate()).thenReturn("Jan 1, 1990");
    when(mockLoan.getNetDisbursementAmount()).thenReturn(9000.00);
    when(mockLoan.getProductId()).thenReturn(new BigInteger("101"));
    when(mockLoan.getRateOfInterest()).thenReturn(7.5);
    when(mockLoan.getChargesPostDisbursal()).thenReturn(50.00);
    when(mockLoan.getLoanApplicationId()).thenReturn(BigInteger.valueOf(12345));
    when(mockLoan.getRepaymentPeriodFrequencyEnum()).thenReturn(new BigInteger("1"));
    when(mockLoan.getTenure()).thenReturn(new BigInteger("24"));

    // Mock ProductOfficeMapping and OfficeLogoMapping methods
    mockStatic(ProductOfficeMapping.class);
    when(ProductOfficeMapping.getOfficeNameByProductId(101)).thenReturn("ABC Bank");

    mockStatic(OfficeLogoMapping.class);
    when(OfficeLogoMapping.getUrlByOfficeName("ABC Bank")).thenReturn("http://abc.com/logo.png");

    // Call the method
    LoanDetailsDTO loanDetailsDTO = LoanDetailsUtil.mapLoanToDto(mockLoan);

    // Assert the values
    assertEquals("LN12345", loanDetailsDTO.getLoanAccountNumber());
    assertEquals("UNKNOWN_STATUS", loanDetailsDTO.getStatus());
    assertEquals(10000.00, loanDetailsDTO.getLoanAmount());
    assertEquals(1000.00, loanDetailsDTO.getEmiAmount());
    assertEquals("01/01/1990", loanDetailsDTO.getDisbursementDate());
    assertEquals("24 Weeks", loanDetailsDTO.getLoanTenure());
    assertEquals("ABC Bank", loanDetailsDTO.getLendingServiceProvider());
    assertEquals("http://abc.com/logo.png", loanDetailsDTO.getLogo());
  }
}
