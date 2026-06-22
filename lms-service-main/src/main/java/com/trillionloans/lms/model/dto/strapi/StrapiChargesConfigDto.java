package com.trillionloans.lms.model.dto.strapi;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Strapi v5 flat response object for the charges-configs collection. */
@Getter
@NoArgsConstructor
public class StrapiChargesConfigDto {

  @SerializedName("product_code")
  private String productCode;

  @SerializedName("partner_code")
  private String partnerCode;

  @SerializedName("active_from_date")
  private String activeFromDate;

  @SerializedName("flag_to_enable_charges")
  private Boolean flagToEnableCharges;

  private List<StrapiChargeEntryDto> charges;
}
