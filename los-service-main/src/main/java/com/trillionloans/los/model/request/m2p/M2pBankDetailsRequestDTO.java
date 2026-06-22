package com.trillionloans.los.model.request.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for bank details in the M2P (Money to People) context. This class
 * encapsulates the necessary information regarding a bank account related to a loan application
 * process.
 *
 * <p>This DTO is used to capture the details required for bank transactions such as repayment and
 * disbursement.
 *
 * <p>The following fields are included:
 *
 * <ul>
 *   <li><strong>name</strong>: The name of the account holder.
 *   <li><strong>accountNumber</strong>: The bank account number.
 *   <li><strong>ifscCode</strong>: The Indian Financial System Code for the bank branch.
 *   <li><strong>accountTypeId</strong>: Identifier for the type of bank account (e.g., savings,
 *       current).
 *   <li><strong>supportedForRepayment</strong>: Indicates if the account supports repayment
 *       transactions.
 *   <li><strong>supportedForDisbursement</strong>: Indicates if the account supports disbursement
 *       transactions.
 * </ul>
 *
 * <p>This class utilizes Lombok annotations to automatically generate boilerplate code such as
 * getters, setters, and constructors.
 *
 * <p>Example usage:
 *
 * <pre>
 * M2pBankDetailsRequestDTO bankDetails = M2pBankDetailsRequestDTO.builder()
 *     .name("John Doe")
 *     .accountNumber("1234567890")
 *     .ifscCode("ABC123456")
 *     .accountTypeId("SAVINGS")
 *     .supportedForRepayment(true)
 *     .supportedForDisbursement(false)
 *     .build();
 * </pre>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class M2pBankDetailsRequestDTO {
  private String name;
  private String accountNumber;
  private String ifscCode;
  private String accountTypeId;
  private boolean supportedForRepayment;
  private boolean supportedForDisbursement;
}
