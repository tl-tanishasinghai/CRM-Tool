package com.trillionloans.los.model.dto.internal;

import com.trillionloans.los.config.AmlPepConfig;
import com.trillionloans.los.config.InsuranceConfig;
import com.trillionloans.los.config.PanValidationConfig;
import com.trillionloans.los.config.ValidationFunnelConfiguration;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class ProductControl {
  private List<Flow> flows;

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Flow {
    private String identifier;

    /**
     * Config schema version for this flow (e.g. {@code v1} for BUSINESS_LOAN_CONFIG upload rules).
     */
    private String version;

    private String functionName;
    private String partnerUri;
    private String callMethod;
    private Integer retryCount;
    private String loggerHeader;
    private boolean pepCheckEnabled;
    private boolean amlCheckEnabled;
    private Double amlRejectionThreshold;
    private Double amlManualVerificationThreshold;

    // cta call configurations, SpEL used for parsing rules
    private boolean ctaCallFlag;
    private boolean newDisbursementFlow;
    private boolean aadhaarPanLinkCheck;
    private boolean aadhaarPanLinkEnforce;
    private boolean disableFunnel;

    private String ctaName;
    private boolean parseDisbursementDate;
    private boolean leadAcknowledgement;

    // AML PEP CONFIG
    private AmlPepConfig amlPepConfig;

    private InsuranceConfig insuranceConfig;

    // condition configurations, used for migration or any other product based configuration
    private HashMap<String, Object> conditions;

    // disbursal checks condition
    private HashMap<String, Object> disbValidationCondition;

    // preactivation checks condition
    private HashMap<String, Object> activationValidationCondition;

    // documents array for no cta calling
    private List<CtaConfiguration> ctaConfigurations;
    private Boolean vcipSmsEnabled;

    // pan validation feature configurations
    private ValidationFunnelConfiguration validationFunnelConfiguration;

    // NSDL phase1 config
    private PanValidationConfig panValidationConfig;

    // bre offer expiry configurations
    private Boolean rejectOnExpiry;
    private Integer offerEligibilityTenureDays;

    private Boolean isRiskParameterCalculationEnabled;
    private String metricValueCalculationFunction;
    private String metricPercentageCalculationFunction;

    private Boolean isBusinessLoan;

    /**
     * Document tags that trigger business loan evaluation on upload (e.g. CPV, UDYAM_CERTIFICATE).
     */
    private List<String> uploadDocumentList;

    /**
     * All document tags for business loan (e.g. CPV, UDYAM_CERTIFICATE,
     * GST_REGISTRATION_CERTIFICATE).
     */
    private List<String> allDocumentList;

    /** Document tags that require documentId (e.g. GST_REGISTRATION_CERTIFICATE). */
    private List<String> documentIdDocumentList;

    /** When true, documents in ocrDocumentList are sent to SQS for async OCR processing. */
    private Boolean ocrProductFlag;

    /** Document tags that go through OCR queue (e.g. UDYAM_CERTIFICATE, CPV). */
    private List<String> ocrDocumentList;

    // loan level client details config - m-client update flag
    private boolean mClientUpdate;

    @Getter
    @AllArgsConstructor
    public static class CtaConfiguration {
      private String ctaName;
      private List<String> documents;
    }
  }
}
