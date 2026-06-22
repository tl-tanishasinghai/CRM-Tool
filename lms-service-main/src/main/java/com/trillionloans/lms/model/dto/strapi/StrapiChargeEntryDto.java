package com.trillionloans.lms.model.dto.strapi;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StrapiChargeEntryDto {

  private String name;

  private BigDecimal value;

  private String trigger;

  private String frequency;

  private String description;

  @SerializedName("offset_days")
  private Integer offsetDays;

  @SerializedName("gst_applicable")
  private Boolean gstApplicable;

  @SerializedName("posting_enabled")
  private Boolean postingEnabled;

  @SerializedName("calculation_type")
  private String calculationType;

  @SerializedName("charge_short_code")
  private String chargeShortCode;

  @SerializedName("m2p_charge_type_id")
  private Long m2pChargeTypeId;

  @SerializedName("posting_date_mode")
  private String postingDateMode;

  @SerializedName("payment_status_allowed")
  private List<String> paymentStatusAllowed;

  @SerializedName("daily_rates")
  private List<StrapiDailyRateDto> dailyRates;
}
