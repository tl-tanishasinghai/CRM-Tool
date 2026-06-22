package com.trillionloans.lms.model.request.restructure;

import static com.trillionloans.lms.constant.StringConstants.M2P_DATE_FORMAT;
import static com.trillionloans.lms.constant.StringConstants.M2P_LOCALE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for initiating loan reschedule with M2P.
 *
 * @author Amar Bhosale
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Reschedule Initiate Request for M2P")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RescheduleInitiateRequest {

  @Schema(description = "Loan ID (LAN)", example = "15736")
  private Long loanId;

  @Schema(description = "Date from which reschedule should start", example = "07-01-2026")
  private String rescheduleFromDate;

  @Schema(description = "Reschedule reason code ID", example = "331")
  private Long rescheduleReasonId;

  @Schema(description = "Date when request was submitted", example = "10-02-2026")
  private String submittedOnDate;

  @Schema(description = "Whether reschedule is specific to an installment", example = "false")
  @Builder.Default
  private Boolean specificToInstallment = false;

  @Schema(description = "Number of extra terms/days to add", example = "80")
  private String extraTerms;

  @Schema(description = "Grace period on principal (in EMIs)", example = "10")
  private String graceOnPrincipal;

  @Schema(description = "Interest-free period (in EMIs)", example = "10")
  private String interestFreePeriod;

  @Schema(description = "Whether to compound grace interest", example = "false")
  @Builder.Default
  private Boolean compoundGraceInterest = false;

  @Schema(description = "Date format pattern", example = "dd-MM-yyyy")
  @Builder.Default
  private String dateFormat = M2P_DATE_FORMAT;

  @Schema(description = "Locale for the request", example = "en")
  @Builder.Default
  private String locale = M2P_LOCALE;
}
