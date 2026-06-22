package com.trillionloans.lms.model.dto.strapi;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Strapi v5 callback component — maps 1:1 to ProductControl.Flow. */
@Getter
@NoArgsConstructor
public class StrapiCallbackDto {

  private String identifier;

  @SerializedName("function_name")
  private String functionName;

  @SerializedName("partner_uri")
  private String partnerUri;

  @SerializedName("call_method")
  private String callMethod;

  @SerializedName("retry_count")
  private Integer retryCount;

  @SerializedName("logger_header")
  private String loggerHeader;

  @SerializedName("cta_call_flag")
  private boolean ctaCallFlag;

  @SerializedName("cta_name")
  private String ctaName;

  @SerializedName("written_off_dpd_days")
  private Integer writtenOffDpdDays;

  private HashMap<String, Object> conditions;
}
