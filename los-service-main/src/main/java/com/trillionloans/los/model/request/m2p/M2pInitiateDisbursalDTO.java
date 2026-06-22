package com.trillionloans.los.model.request.m2p;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) for initiating a disbursal in the M2P (Money to Payment) process. This
 * class encapsulates the necessary information required to process a disbursal request.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class M2pInitiateDisbursalDTO {

  /**
   * The identifier for the payment type. This could represent various payment methods supported by
   * the system.
   */
  private int paymentTypeId;

  /**
   * The identifier for the bank account detail associated with the disbursal. This should reference
   * a valid bank account in the system.
   */
  private String bankAccountDetailId;

  /**
   * The actual date when the disbursement is to be made. This should be in a valid date format as
   * specified by the dateFormat field.
   */
  private String expectedDisbursementDate;

  /**
   * The format of the date specified in actualDisbursementDate. This defines how the date should be
   * interpreted (e.g., "yyyy-MM-dd").
   */
  private String dateFormat;
}
