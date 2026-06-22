package com.trillionloans.lms.model.dto.internal;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class ProductControl {
  private List<Flow> flows;
  private ChargesConfig chargesConfig;

  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ChargesConfig {
    private Boolean flagToEnableCharges;
    private String activeFromDate;
    private List<ChargeEntry> charges;
  }

  @Getter
  @AllArgsConstructor
  public static class Flow {
    private String identifier;
    private String functionName;
    private String partnerUri;
    private String callMethod;
    private Integer retryCount;
    private String loggerHeader;

    // cta call configurations, SPEL used for parsing rules
    private boolean ctaCallFlag;
    private String ctaName;

    // re-kyc
    private Integer writtenOffDpdDays;

    // Condition configurations, Used for migration or any other product based configuration
    private HashMap<String, Object> conditions;
  }

  @Getter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class ChargeEntry {
    private String name; // charge name
    private String chargeShortCode; // e.g., BC, PC1, PC2
    private String calculationType; // FLAT | PCT_PI_REMAINING | PCT_ON_PRINCIPAL
    private BigDecimal value; // amount or rate (e.g., 0.02)
    private boolean gstApplicable; // true/false
    private long m2pChargeTypeId; // downstream posting type
    private Integer offsetDays; // exact T+N, e.g., 2,4,8 (nullable)
    private String trigger; // NEW: "DPD_EQUALS" | "MONTH_END_OVERDUE" | null
    private List<String> paymentStatusAllowed; // ["NOT_PAID"] or ["NOT_PAID","PARTIAL"]
    private List<DailyRate> dailyRates; // for daily penal slabs
    private String frequency; // e.g., "daily", "one_time" (optional)
    private String postingDateMode; // "RUN_DATE" (default) | "EMI_DUE_DATE"
    private Boolean postingEnabled;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyRate {
      private int dpdFrom;
      private int dpdTo;
      private BigDecimal dailyRate;
      private String description;
    }
  }
}
