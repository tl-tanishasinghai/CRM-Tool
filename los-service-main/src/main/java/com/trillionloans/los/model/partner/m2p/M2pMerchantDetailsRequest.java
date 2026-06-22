package com.trillionloans.los.model.partner.m2p;

import com.trillionloans.los.model.request.MerchantDetailsRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO class for the merchant details that are fetched from LSP. */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Schema(description = "Merchant/anchor details request body")
public class M2pMerchantDetailsRequest {

  /** The name of the merchant. */
  private String name;

  /** The short name or abbreviation for the merchant. */
  private String shortName;

  /** The limit amount associated with the merchant. */
  private String limitAmount;

  /** The end date of the relationship with the merchant. */
  private String relationshipEndDate;

  /** The category under which the merchant falls (e.g., retail, services). */
  private String category;

  /** The date format used for the merchant's records. */
  private String dateFormat;

  /** The locale setting for the merchant, which may affect language and regional settings. */
  private String locale;

  /** The office ID associated with the merchant. */
  private Integer officeId;

  /** Details of the contact person associated with the merchant. */
  private MerchantDetailsRequest.ContactPersonDetails contactPersondetails;
}
