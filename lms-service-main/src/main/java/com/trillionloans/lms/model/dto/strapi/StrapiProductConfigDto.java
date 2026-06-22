package com.trillionloans.lms.model.dto.strapi;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Strapi v5 flat response object for the product-configurations collection. */
@Getter
@NoArgsConstructor
public class StrapiProductConfigDto {

  @SerializedName("product_code")
  private String productCode;

  @SerializedName("partner_code")
  private String partnerCode;

  private List<StrapiCallbackDto> callbacks;
}
