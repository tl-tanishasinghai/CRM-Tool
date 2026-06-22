package com.trillionloans.lms.model.dto.strapi;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StrapiDailyRateDto {

  @SerializedName("dpd_from")
  private int dpdFrom;

  @SerializedName("dpd_to")
  private int dpdTo;

  @SerializedName("daily_rate")
  private BigDecimal dailyRate;

  private String description;
}
