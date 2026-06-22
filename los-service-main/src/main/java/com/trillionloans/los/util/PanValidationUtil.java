package com.trillionloans.los.util;

import com.trillionloans.los.exception.PanValidationExceptions.BuildOpvRequestException;
import com.trillionloans.los.exception.PanValidationExceptions.PanResultsPersistenceException;
import com.trillionloans.los.model.ClientCacheDTO;
import com.trillionloans.los.model.PanVerificationResult;
import com.trillionloans.los.model.dto.NsdlRejectionType;
import com.trillionloans.los.model.dto.PanVerificationLog;
import com.trillionloans.los.model.dto.internal.LoanLevelClientDetailsCacheDTO;
import com.trillionloans.los.model.request.NsdlPanVerificationRequest;
import com.trillionloans.los.model.response.m2p.M2pLoanCreationResponseDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

@Slf4j
public class PanValidationUtil {
  private PanValidationUtil() {}

  public static Mono<NsdlPanVerificationRequest> buildPanVerificationRequest(
      LoanLevelClientDetailsCacheDTO clientCacheDTO) {
    return Mono.defer(
        () -> {
          if (clientCacheDTO == null) {
            return Mono.error(new BuildOpvRequestException("Cannot build OPV request."));
          }

          try {
            NsdlPanVerificationRequest request =
                NsdlPanVerificationRequest.builder()
                    .pan(clientCacheDTO.getPanNumber())
                    .name(
                        buildFullName(
                            clientCacheDTO.getFirstName(),
                            clientCacheDTO.getMiddleName(),
                            clientCacheDTO.getLastName()))
                    .dob(sanitizeDob(clientCacheDTO.getDateOfBirth()))
                    .build();

            return Mono.just(request);
          } catch (Exception e) {
            return Mono.error(new BuildOpvRequestException("Failed to build OPV request", e));
          }
        });
  }

  public static Mono<NsdlPanVerificationRequest> buildPanVerificationRequestPhase1(
      ClientCacheDTO clientCacheDTO) {
    return Mono.defer(
        () -> {
          if (clientCacheDTO == null) {
            return Mono.error(new BuildOpvRequestException("Cannot build OPV request."));
          }

          try {
            NsdlPanVerificationRequest request =
                NsdlPanVerificationRequest.builder()
                    .pan(clientCacheDTO.getPanNumber())
                    .name(
                        buildFullName(
                            clientCacheDTO.getFirstName(),
                            clientCacheDTO.getMiddleName(),
                            clientCacheDTO.getLastName()))
                    .dob(sanitizeDob(clientCacheDTO.getDateOfBirth()))
                    .build();

            return Mono.just(request);
          } catch (Exception e) {
            return Mono.error(new BuildOpvRequestException("Failed to build OPV request", e));
          }
        });
  }

  private static String sanitizeDob(String dob) {
    if (dob == null || dob.isBlank()) {
      return dob;
    }

    List<DateTimeFormatter> inputFormatters =
        List.of(
            DateTimeFormatter.ofPattern("dd-MM-yyyy"), // e.g. 01-01-1989
            DateTimeFormatter.ofPattern("d-M-yyyy"), // e.g. 1-1-1989
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH), // e.g. Jan 1, 1989
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH) // e.g. January 1, 1989
            );

    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    for (DateTimeFormatter inputFormatter : inputFormatters) {
      try {
        return LocalDate.parse(dob.trim(), inputFormatter).format(outputFormatter);
      } catch (DateTimeParseException ignored) {
        // try next format
      }
    }

    throw new BuildOpvRequestException("Unsupported date format for DOB: " + dob);
  }

  public static PanVerificationLog buildVerificationLogForNullResponse(
      String leadId,
      M2pLoanCreationResponseDTO loanApplicationResp,
      ClientCacheDTO clientDetails,
      PanVerificationLog.FinalVerificationResult status) {

    try {

      String loanApplicationId =
          Objects.nonNull(loanApplicationResp)
                  && Objects.nonNull(loanApplicationResp.getResourceId())
              ? loanApplicationResp.getResourceId().toString()
              : StringUtils.EMPTY;

      String panNumber =
          Objects.nonNull(clientDetails) && Objects.nonNull(clientDetails.getPanNumber())
              ? clientDetails.getPanNumber()
              : StringUtils.EMPTY;

      String firstName =
          Objects.nonNull(clientDetails) && Objects.nonNull(clientDetails.getFirstName())
              ? clientDetails.getFirstName()
              : StringUtils.EMPTY;
      String middleName =
          Objects.nonNull(clientDetails) && Objects.nonNull(clientDetails.getMiddleName())
              ? clientDetails.getMiddleName()
              : StringUtils.EMPTY;
      String lastName =
          Objects.nonNull(clientDetails) && Objects.nonNull(clientDetails.getLastName())
              ? clientDetails.getLastName()
              : StringUtils.EMPTY;

      String dob =
          Objects.nonNull(clientDetails) && Objects.nonNull(clientDetails.getDateOfBirth())
              ? clientDetails.getDateOfBirth()
              : StringUtils.EMPTY;

      return PanVerificationLog.builder()
          .customerId(Objects.nonNull(leadId) ? leadId : StringUtils.EMPTY)
          .loanApplicationId(loanApplicationId)
          .panNumber(panNumber)
          .nameEntered(buildFullName(firstName, middleName, lastName))
          .dobEntered(dob)
          .finalVerificationResult(status)
          .verificationTimestamp(
              LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                  .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
          .rejectionReason(StringUtils.EMPTY)
          .build();

    } catch (Exception e) {
      throw new PanResultsPersistenceException(
          String.format("Failed to build verification log for leadId=%s", leadId), e);
    }
  }

  public static PanVerificationLog buildVerificationLogForVendorResponse(
      String leadId,
      M2pLoanCreationResponseDTO loanApplicationResp,
      ClientCacheDTO clientDetails,
      PanVerificationResult.VerificationResult result,
      PanVerificationLog.FinalVerificationResult status) {

    try {
      String loanApplicationId =
          Objects.nonNull(loanApplicationResp)
                  && Objects.nonNull(loanApplicationResp.getResourceId())
              ? loanApplicationResp.getResourceId().toString()
              : StringUtils.EMPTY;

      PanVerificationResult.Record vendorResponse =
          Objects.nonNull(result) && Objects.nonNull(result.getVendorResponse())
              ? result.getVendorResponse()
              : new PanVerificationResult.Record();

      PanVerificationResult.EvaluationResult evaluationResult =
          Objects.nonNull(result) && Objects.nonNull(result.getEvaluationResult())
              ? result.getEvaluationResult()
              : new PanVerificationResult.EvaluationResult();

      String panNumber =
          Objects.nonNull(clientDetails) && Objects.nonNull(clientDetails.getPanNumber())
              ? clientDetails.getPanNumber()
              : StringUtils.EMPTY;

      String firstName =
          Objects.nonNull(clientDetails) && Objects.nonNull(clientDetails.getFirstName())
              ? clientDetails.getFirstName()
              : StringUtils.EMPTY;
      String middleName =
          Objects.nonNull(clientDetails) && Objects.nonNull(clientDetails.getMiddleName())
              ? clientDetails.getMiddleName()
              : StringUtils.EMPTY;
      String lastName =
          Objects.nonNull(clientDetails) && Objects.nonNull(clientDetails.getLastName())
              ? clientDetails.getLastName()
              : StringUtils.EMPTY;

      String dob =
          Objects.nonNull(clientDetails) && Objects.nonNull(clientDetails.getDateOfBirth())
              ? clientDetails.getDateOfBirth()
              : StringUtils.EMPTY;

      return PanVerificationLog.builder()
          .customerId(Objects.nonNull(leadId) ? leadId : StringUtils.EMPTY)
          .loanApplicationId(loanApplicationId)
          .panNumber(panNumber)
          .panStatus(
              Objects.nonNull(vendorResponse.getPanStatus())
                  ? vendorResponse.getPanStatus()
                  : StringUtils.EMPTY)
          .seedingStatus(
              Objects.nonNull(vendorResponse.getSeedingStatus())
                  ? vendorResponse.getSeedingStatus()
                  : StringUtils.EMPTY)
          .nameEntered(buildFullName(firstName, middleName, lastName))
          .nameMatchResult(
              Objects.nonNull(vendorResponse.getNameMatch())
                  ? vendorResponse.getNameMatch()
                  : StringUtils.EMPTY)
          .dobEntered(dob)
          .dobMatchResult(
              Objects.nonNull(vendorResponse.getDobMatch())
                  ? vendorResponse.getDobMatch()
                  : StringUtils.EMPTY)
          .finalVerificationResult(status)
          .verificationTimestamp(
              LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                  .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
          .rejectionReason(
              Objects.nonNull(evaluationResult.getRejectionReasons())
                  ? String.join(", ", evaluationResult.getRejectionReasons())
                  : StringUtils.EMPTY)
          .build();

    } catch (Exception e) {
      throw new PanResultsPersistenceException(
          String.format("Failed to build verification log for leadId=%s", leadId), e);
    }
  }

  public static String getRejectionMessage(List<String> rejectionReasons) {
    if (rejectionReasons == null || rejectionReasons.isEmpty()) {
      return StringUtils.EMPTY;
    }

    // Check if rejection is based on pan status
    if (containsReasonForType(rejectionReasons, NsdlRejectionType.PAN_STATUS)) {
      return "Pan Verification Rejected.";
    }
    // next check if the rejection is for SEEDING_STATUS
    else if (containsReasonForType(rejectionReasons, NsdlRejectionType.SEEDING_STATUS)) {
      return "PAN Inoperative - Aadhaar Linkage Required.";
    }
    // in all other cases,
    else {
      return "Pan Verification Rejected.";
    }
  }

  private static boolean containsReasonForType(
      List<String> rejectionReasons, NsdlRejectionType type) {
    String typePrefix = type.getFieldName();
    return rejectionReasons.stream()
        .anyMatch(reason -> reason != null && reason.startsWith(typePrefix));
  }

  private static String buildFullName(String... parts) {
    return Arrays.stream(parts)
        .filter(org.apache.commons.lang3.StringUtils::isNotBlank) // skip null/empty parts
        .collect(Collectors.joining(org.apache.commons.lang3.StringUtils.SPACE));
  }
}
