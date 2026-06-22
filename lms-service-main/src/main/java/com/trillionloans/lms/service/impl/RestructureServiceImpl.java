package com.trillionloans.lms.service.impl;

import static com.trillionloans.lms.constant.StringConstants.CHECK_ELIGIBILITY_LOG;
import static com.trillionloans.lms.constant.StringConstants.CHECK_ELIGIBILITY_WITH_DB_VALIDATION_LOG;
import static com.trillionloans.lms.constant.StringConstants.GET_ELIGIBILITY_LOG;
import static com.trillionloans.lms.constant.StringConstants.RESTRUCTURE_GET_DETAILS;
import static com.trillionloans.lms.constant.StringConstants.RESTRUCTURE_RISK_VALIDATION;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.lms.api.m2p.M2PApi;
import com.trillionloans.lms.config.RestructureRiskValidationProperties;
import com.trillionloans.lms.constant.StringConstants;
import com.trillionloans.lms.exception.ClientSideException;
import com.trillionloans.lms.model.dto.restructure.ApproveRestructureResponseDTO;
import com.trillionloans.lms.model.dto.restructure.EligibilityResponseDTO;
import com.trillionloans.lms.model.dto.restructure.PartialWaiveChargeDTO;
import com.trillionloans.lms.model.dto.restructure.RestructureApprovalDetailsDTO;
import com.trillionloans.lms.model.dto.restructure.RestructureStatus;
import com.trillionloans.lms.model.dto.restructure.RetrieveLoanResponseDTO;
import com.trillionloans.lms.model.dto.restructure.RpsResponseDTO;
import com.trillionloans.lms.model.dto.restructure.StatusResponseDTO;
import com.trillionloans.lms.model.dto.restructure.TentativeRpsResponseDTO;
import com.trillionloans.lms.model.entity.LoanApplicationRestructureDetailsEntity;
import com.trillionloans.lms.model.request.restructure.ApproveRescheduleRequest;
import com.trillionloans.lms.model.request.restructure.PartialWaiverRequest;
import com.trillionloans.lms.model.request.restructure.RescheduleInitiateRequest;
import com.trillionloans.lms.model.response.M2pLanDetails;
import com.trillionloans.lms.model.response.restructure.RescheduleInitiateResponse;
import com.trillionloans.lms.repository.LoanApplicationRestructureDetailsRepository;
import com.trillionloans.lms.repository.LoanRestructureEligibilityMasterRepository;
import com.trillionloans.lms.service.CollectionService;
import com.trillionloans.lms.service.NotificationService;
import com.trillionloans.lms.service.RestructureService;
import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Service implementation for handling loan restructure operations.
 *
 * @author Pawan Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestructureServiceImpl implements RestructureService {

  private static final DateTimeFormatter M2P_DATE_FORMATTER =
      DateTimeFormatter.ofPattern(StringConstants.M2P_DATE_FORMAT);

  private final M2PApi m2PApi;
  private final CollectionService collectionService;
  private final LoanRestructureEligibilityMasterRepository eligibilityMasterRepository;
  private final LoanApplicationRestructureDetailsRepository restructureDetailsRepository;
  private final NotificationService notificationService;
  private final ObjectMapper objectMapper;
  private final RestructureRiskValidationProperties riskValidationProperties;

  @Value("${notification-service.restructure.sms-trigger-enabled:false}")
  private boolean restructureSmsTriggerEnabled;

  private record EligibilityCheckResult(
      EligibilityResponseDTO eligibility, Long leadId, Long clientId) {}

  private record ClientIdLeadId(
      Long clientId, Long leadId, String customerName, String mobileNumber) {}

  private record EligibilityCheckResultWithDbInfo(
      EligibilityCheckResult checkResult, boolean dataChanged, Long existingRequestId) {}

  private record RiskValidationResult(boolean valid, String reason) {}

  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
  private static final DateTimeFormatter DATE_FORMAT_IST =
      DateTimeFormatter.ofPattern("dd MMMM yyyy");
  private static final DateTimeFormatter SMS_EFFECTIVE_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd-MM-yyyy");

  @Override
  public Mono<?> getRestructureDetails(String lan, String type, String requestId) {
    return switch (type.toLowerCase()) {
      case "eligibility" -> getEligibility(lan, type).map(ResponseEntity::ok);
      case "rps" -> getRps(lan, requestId, type).map(ResponseEntity::ok);
      case "status" -> getStatus(lan, requestId, type).map(ResponseEntity::ok);
      default ->
          Mono.error(
              new ResponseStatusException(
                  HttpStatus.BAD_REQUEST,
                  "Invalid type parameter. Allowed values: eligibility, rps, status"));
    };
  }

  /**
   * Returns early-exit message if latest restructure for LAN has INITIATED, FAIL, or SUCCESS.
   * Returns null for NOT_TRIGGERED, INVALIDATED, or null - in those cases continue with normal
   * flow.
   */
  private String getEarlyExitMessageForStatus(RestructureStatus status) {
    if (status == null) {
      return null;
    }
    if (status == RestructureStatus.INITIATED || status == RestructureStatus.FAIL) {
      return "LAN Restructure has already been INITIATED";
    }
    if (status == RestructureStatus.SUCCESS) {
      return "LAN Restructure has already been COMPLETED";
    }
    // NOT_TRIGGERED, INVALIDATED - no early exit, proceed with normal flow
    return null;
  }

  /**
   * Fetches RPS for restructure. Validates requestId and lan against
   * loan_application_restructure_details. Ensures requestId belongs to the specified LAN before
   * returning any response.
   */
  private Mono<RpsResponseDTO> getRps(String lan, String requestId, String type) {
    log.warn(RESTRUCTURE_GET_DETAILS, "rps", lan, "getting rps", "WIP", "");

    if (requestId == null || requestId.isBlank()) {
      return Mono.error(
          new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestId is required for RPS"));
    }

    Long requestIdLong = parseLan(requestId);
    if (requestIdLong == null) {
      return Mono.error(
          new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid requestId: " + requestId));
    }

    Long lanLong = parseLan(lan);
    if (lanLong == null) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lan: " + lan));
    }

    return restructureDetailsRepository
        .findById(requestIdLong)
        .switchIfEmpty(
            Mono.error(
                new ClientSideException(
                    "Restructure details not found", null, HttpStatus.BAD_REQUEST, "")))
        .flatMap(
            entity -> {
              if (!lanLong.equals(entity.getLan())) {
                return Mono.error(
                    new ClientSideException(
                        "RequestId does not belong to the specified LAN",
                        null,
                        HttpStatus.BAD_REQUEST,
                        ""));
              }

              log.info(
                  RESTRUCTURE_GET_DETAILS,
                  "eligibility",
                  lan,
                  "checking early exist applicable",
                  "WIP",
                  "");
              Mono<RpsResponseDTO> earlyExit =
                  restructureDetailsRepository
                      .findFirstByLanOrderByIdDesc(lanLong)
                      .flatMap(
                          latestEntity -> {
                            log.info(
                                RESTRUCTURE_GET_DETAILS,
                                "eligibility",
                                lan,
                                "checking early exist applicable",
                                "WIP",
                                "");

                            String msg =
                                getEarlyExitMessageForStatus(
                                    RestructureStatus.from(latestEntity.getRestructure()));
                            return msg != null
                                ? Mono.just(RpsResponseDTO.builder().message(msg).build())
                                : Mono.empty();
                          });

              return earlyExit.switchIfEmpty(
                  Mono.defer(
                      () -> {
                        if (RestructureStatus.INVALIDATED.equals(
                            RestructureStatus.from(entity.getRestructure()))) {
                          log.warn(
                              RESTRUCTURE_GET_DETAILS,
                              "rps",
                              lan,
                              "Data has been INVALIDATED to fetch RPS",
                              "fail",
                              "");

                          return Mono.just(
                              RpsResponseDTO.builder()
                                  .message("Data has been INVALIDATED to fetch RPS")
                                  .build());
                        }
                        if (!Boolean.TRUE.equals(entity.getEligibility())) {
                          log.warn(
                              RESTRUCTURE_GET_DETAILS,
                              "rps",
                              lan,
                              "requestId not eligible for RPS fetch",
                              "fail",
                              "");
                          return Mono.error(
                              new ClientSideException(
                                  "requestId not eligible for RPS fetch",
                                  null,
                                  HttpStatus.BAD_REQUEST,
                                  ""));
                        }
                        Long restructureId = entity.getRestructureId();
                        if (restructureId == null) {
                          log.warn(
                              RESTRUCTURE_GET_DETAILS,
                              "rps",
                              lan,
                              "Restructure details not found",
                              "fail",
                              "");
                          return Mono.error(
                              new ClientSideException(
                                  "Restructure details not found",
                                  null,
                                  HttpStatus.BAD_REQUEST,
                                  ""));
                        }
                        return m2PApi
                            .getTentativeRestructuredRps(restructureId.intValue())
                            .map(
                                tentativeRps ->
                                    RpsResponseDTO.builder().data(tentativeRps).build());
                      }));
            })
        .doOnSuccess(
            r -> log.warn(RESTRUCTURE_GET_DETAILS, "rps", lan, "getting rps", "SUCCESS", ""))
        .doOnError(
            e ->
                log.warn(
                    RESTRUCTURE_GET_DETAILS,
                    "rps",
                    lan,
                    "getting rps",
                    "FAIL",
                    "error:" + e.getMessage()));
  }

  /** Fetches restructure status. Returns from DB for SUCCESS/FAIL/NOT_TRIGGERED/INVALIDATED. */
  private Mono<StatusResponseDTO> getStatus(String lan, String requestId, String type) {
    log.warn(RESTRUCTURE_GET_DETAILS, "status", lan, "getting status", "WIP", "");

    if (requestId == null || requestId.isBlank()) {
      return Mono.error(
          new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestId is required for status"));
    }

    Long requestIdLong = parseLan(requestId);
    if (requestIdLong == null) {
      return Mono.error(
          new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid requestId: " + requestId));
    }

    Long lanLong = parseLan(lan);
    if (lanLong == null) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid lan: " + lan));
    }

    return restructureDetailsRepository
        .findById(requestIdLong)
        .switchIfEmpty(
            Mono.error(
                new ClientSideException(
                    "Restructure details not found", null, HttpStatus.BAD_REQUEST, "")))
        .flatMap(
            entity -> {
              if (!lanLong.equals(entity.getLan())) {
                log.warn(
                    RESTRUCTURE_GET_DETAILS,
                    "status",
                    lan,
                    "RequestId does not belong to the specified LAN",
                    "fail",
                    "");
                return Mono.error(
                    new ClientSideException(
                        "RequestId does not belong to the specified LAN",
                        null,
                        HttpStatus.BAD_REQUEST,
                        ""));
              }

              RestructureStatus dbStatus = RestructureStatus.from(entity.getRestructure());
              log.info(
                  RESTRUCTURE_GET_DETAILS,
                  "status",
                  lan,
                  "checking status for requestId",
                  "WIP",
                  "data found:" + entity);

              if (dbStatus == RestructureStatus.INITIATED) {
                Long restructureId = entity.getRestructureId();
                if (restructureId == null) {
                  log.warn(
                      RESTRUCTURE_GET_DETAILS,
                      "status",
                      lan,
                      "Restructure ID not found for status fetch",
                      "fail",
                      "");
                  return Mono.error(
                      new ClientSideException(
                          "Restructure ID not found for status fetch",
                          null,
                          HttpStatus.BAD_REQUEST,
                          ""));
                }
                return m2PApi
                    .getRestrcutureApprovalDetails(restructureId.intValue())
                    .map(
                        apiResponse ->
                            mapApiResponseToStatus(apiResponse, lanLong, entity.getId()));
              }

              return Mono.just(
                  buildStatusFromDb(
                      entity.getRestructure(), entity.getUpdatedAt(), lanLong, entity.getId()));
            })
        .doOnSuccess(
            r -> log.warn(RESTRUCTURE_GET_DETAILS, "status", lan, "getting status", "SUCCESS", ""))
        .doOnError(
            e ->
                log.warn(
                    RESTRUCTURE_GET_DETAILS,
                    "status",
                    lan,
                    "getting status",
                    "FAIL",
                    "error:" + e.getMessage()));
  }

  private StatusResponseDTO mapApiResponseToStatus(
      RestructureApprovalDetailsDTO apiResponse, Long lanId, Long requestId) {
    String statusValue =
        apiResponse.getStatusEnum() != null ? apiResponse.getStatusEnum().getValue() : null;
    String status;
    String approvedOnDate;

    if ("Approved".equals(statusValue)) {
      status = RestructureStatus.SUCCESS.name();
      approvedOnDate = formatApprovedOnDateFromApi(apiResponse.getTimeline());
    } else if ("Submitted and pending approval".equals(statusValue)) {
      status = RestructureStatus.FAIL.name();
      approvedOnDate = null;
    } else {
      status = RestructureStatus.NOT_TRIGGERED.name();
      approvedOnDate = null;
    }

    return StatusResponseDTO.builder()
        .status(status)
        .lanId(lanId)
        .approvedOnDate(approvedOnDate)
        .requestId(requestId)
        .build();
  }

  private String formatApprovedOnDateFromApi(RestructureApprovalDetailsDTO.TimelineDTO timeline) {
    if (timeline == null
        || timeline.getApprovedOnDate() == null
        || timeline.getApprovedOnDate().size() < 3) {
      return null;
    }
    List<Integer> parts = timeline.getApprovedOnDate();
    int year = parts.get(0);
    int month = parts.get(1);
    int day = parts.get(2);
    try {
      LocalDate date = LocalDate.of(year, month, day);
      return date.format(DATE_FORMAT_IST);
    } catch (Exception e) {
      log.info("Invalid approvedOnDate format: {}", parts);
      return null;
    }
  }

  private StatusResponseDTO buildStatusFromDb(
      String dbStatus, LocalDateTime updatedAt, Long lanId, Long requestId) {
    String approvedOnDate = null;
    if (RestructureStatus.SUCCESS.name().equals(dbStatus) && updatedAt != null) {
      approvedOnDate =
          updatedAt.atZone(ZoneOffset.UTC).withZoneSameInstant(IST).format(DATE_FORMAT_IST);
    }
    return StatusResponseDTO.builder()
        .status(dbStatus)
        .lanId(lanId)
        .approvedOnDate(approvedOnDate)
        .requestId(requestId)
        .build();
  }

  /** Fetches eligibility details for loan restructure based on lead ID. */
  private Mono<EligibilityResponseDTO> getEligibility(String lanId, String type) {
    log.info(GET_ELIGIBILITY_LOG, lanId, "data_get", "WIP", "");

    Long lan = parseLan(lanId);
    if (lan == null) {
      log.warn(GET_ELIGIBILITY_LOG, lanId, "data_get", "fail", "Invalid leadId/LAN");
      return Mono.error(
          new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid leadId/LAN: " + lanId));
    }

    Long leadIdLong = lan;
    Mono<EligibilityResponseDTO> earlyExit =
        restructureDetailsRepository
            .findFirstByLanOrderByIdDesc(lan)
            .flatMap(
                entity -> {
                  log.info(
                      RESTRUCTURE_GET_DETAILS,
                      "eligibility",
                      lanId,
                      "checking early exit applicable",
                      "WIP",
                      "");
                  String msg =
                      getEarlyExitMessageForStatus(RestructureStatus.from(entity.getRestructure()));
                  return msg != null
                      ? Mono.just(EligibilityResponseDTO.builder().message(msg).build())
                      : Mono.empty();
                });

    Mono<EligibilityResponseDTO> eligibilityMono =
        earlyExit.switchIfEmpty(
            Mono.defer(
                () ->
                    checkEligibilityWithDbValidation(lan, leadIdLong, type)
                        .flatMap(
                            dbInfo -> {
                              EligibilityCheckResult checkResult = dbInfo.checkResult();
                              EligibilityResponseDTO eligibilityResult = checkResult.eligibility();

                              if (!dbInfo.dataChanged() && dbInfo.existingRequestId() != null) {
                                return restructureDetailsRepository
                                    .findById(dbInfo.existingRequestId())
                                    .flatMap(
                                        existing ->
                                            existing.getRestructureId() != null
                                                    && Boolean.TRUE.equals(
                                                        eligibilityResult.getEligible())
                                                ? m2PApi
                                                    .getTentativeRestructuredRps(
                                                        existing.getRestructureId().intValue())
                                                    .map(
                                                        tentativeRps ->
                                                            eligibilityResult.toBuilder()
                                                                .requestId(
                                                                    dbInfo.existingRequestId())
                                                                .tentativeRps(
                                                                    (TentativeRpsResponseDTO)
                                                                        tentativeRps)
                                                                .build())
                                                : Mono.just(
                                                    eligibilityResult.toBuilder()
                                                        .requestId(dbInfo.existingRequestId())
                                                        .build()))
                                    .defaultIfEmpty(
                                        eligibilityResult.toBuilder()
                                            .requestId(dbInfo.existingRequestId())
                                            .build());
                              }

                              return eligibilityResult.getEligible()
                                  ? initiateRestructure(
                                          lan,
                                          eligibilityResult.getDpd(),
                                          eligibilityResult.getResidualTenure(),
                                          eligibilityResult.getPaidRepayments())
                                      .flatMap(
                                          restructureRequestId ->
                                              m2PApi
                                                  .getTentativeRestructuredRps(
                                                      restructureRequestId.intValue())
                                                  .map(
                                                      tentativeRps ->
                                                          eligibilityResult.toBuilder()
                                                              .tentativeRps(
                                                                  (TentativeRpsResponseDTO)
                                                                      tentativeRps)
                                                              .build())
                                                  .flatMap(
                                                      finalResponse ->
                                                          getClientIdAndLeadIdFromLanDetails(lan)
                                                              .flatMap(
                                                                  ids ->
                                                                      saveRestructureDetails(
                                                                              lan,
                                                                              ids.leadId(),
                                                                              ids.clientId(),
                                                                              ids.customerName(),
                                                                              ids.mobileNumber(),
                                                                              finalResponse,
                                                                              restructureRequestId
                                                                                  .intValue())
                                                                          .map(
                                                                              savedId ->
                                                                                  finalResponse
                                                                                      .toBuilder()
                                                                                      .requestId(
                                                                                          savedId)
                                                                                      .build()))))
                                  : getClientIdAndLeadIdFromLanDetails(lan)
                                      .flatMap(
                                          ids ->
                                              saveRestructureDetails(
                                                      lan,
                                                      ids.leadId(),
                                                      ids.clientId(),
                                                      ids.customerName(),
                                                      ids.mobileNumber(),
                                                      eligibilityResult,
                                                      null)
                                                  .map(
                                                      savedId ->
                                                          eligibilityResult.toBuilder()
                                                              .requestId(savedId)
                                                              .build()));
                            })
                        .onErrorResume(
                            e ->
                                Mono.just(
                                    EligibilityResponseDTO.builder()
                                        .eligible(false)
                                        .dpd(null)
                                        .residualTenure(null)
                                        .pos(null)
                                        .tos(null)
                                        .tentativeRps(null)
                                        .requestId(null)
                                        .reason("Failed to check eligibility: " + e.getMessage())
                                        .build()))));

    return eligibilityMono
        .doOnSuccess(
            r -> {
              String outcome =
                  (r.getEligible() == null && r.getMessage() != null)
                      ? "EARLY_EXIT"
                      : (Boolean.TRUE.equals(r.getEligible()) ? "ELIGIBLE" : "INELIGIBLE");
              String reason = r.getReason();
              if (r.getMessage() != null && !r.getMessage().isBlank()) {
                reason = r.getMessage();
              }
              log.info(
                  RESTRUCTURE_GET_DETAILS,
                  "eligibility",
                  lanId,
                  "checking eligibility",
                  "SUCCESS",
                  outcome);
              if ("ELIGIBLE".equals(outcome) || "EARLY_EXIT".equals(outcome)) {
                log.info(GET_ELIGIBILITY_LOG, lanId, "get_eligibility", "success", outcome);
              } else {
                log.warn(
                    GET_ELIGIBILITY_LOG,
                    lanId,
                    "get_eligibility",
                    "fail",
                    reason != null ? reason : outcome);
              }
            })
        .doOnError(
            e -> {
              log.warn(
                  RESTRUCTURE_GET_DETAILS,
                  "eligibility",
                  lanId,
                  "checking eligibility",
                  "FAIL",
                  e.getMessage() != null ? e.getMessage() : "");
              log.warn(
                  GET_ELIGIBILITY_LOG,
                  lanId,
                  "data_get",
                  "fail",
                  e.getMessage() != null ? e.getMessage() : "");
            });
  }

  /**
   * Checks eligibility with DB validation: compares with existing eligibility_data. If any of
   * eligible, dpd, residualTenure, pos, tos differs: invalidates old record. Always returns
   * Mono&lt;EligibilityCheckResult&gt;.
   */
  private Mono<EligibilityCheckResultWithDbInfo> checkEligibilityWithDbValidation(
      Long lan, Long leadId, String type) {
    log.info(CHECK_ELIGIBILITY_WITH_DB_VALIDATION_LOG, lan, "data_get", "WIP", "");
    return checkEligibility(lan, leadId, type)
        .flatMap(
            checkResult ->
                restructureDetailsRepository
                    .findFirstByLanOrderByIdDesc(lan)
                    .flatMap(
                        existing ->
                            Mono.fromCallable(
                                    () ->
                                        isEligibilityDataChanged(
                                            existing.getEligibilityData(),
                                            existing.getEligibility(),
                                            checkResult.eligibility()))
                                .flatMap(
                                    dataChanged -> {
                                      if (dataChanged) {
                                        log.warn(
                                            CHECK_ELIGIBILITY_WITH_DB_VALIDATION_LOG,
                                            lan,
                                            "db_validation",
                                            "fail",
                                            "eligibility data changed, record invalidated");
                                        existing.setRestructure(
                                            RestructureStatus.INVALIDATED.name());
                                        existing.setUpdatedAt(LocalDateTime.now());
                                        return restructureDetailsRepository
                                            .save(existing)
                                            .then(
                                                Mono.just(
                                                    new EligibilityCheckResultWithDbInfo(
                                                        checkResult, true, null)));
                                      }
                                      log.info(
                                          CHECK_ELIGIBILITY_WITH_DB_VALIDATION_LOG,
                                          lan,
                                          "db_validation",
                                          "success",
                                          "existing record used, requestId=" + existing.getId());
                                      return Mono.just(
                                          new EligibilityCheckResultWithDbInfo(
                                              checkResult, false, existing.getId()));
                                    }))
                    .switchIfEmpty(
                        Mono.fromCallable(
                            () -> {
                              log.info(
                                  CHECK_ELIGIBILITY_WITH_DB_VALIDATION_LOG,
                                  lan,
                                  "db_validation",
                                  "success",
                                  "no existing record");
                              return new EligibilityCheckResultWithDbInfo(checkResult, true, null);
                            })));
  }

  private boolean isEligibilityDataChanged(
      Json eligibilityDataJson, Boolean existingEligible, EligibilityResponseDTO newEligibility) {
    if (eligibilityDataJson == null) {
      return true;
    }
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> stored =
          objectMapper.readValue(eligibilityDataJson.asString(), Map.class);
      Boolean storedEligible =
          stored.containsKey("eligible") ? (Boolean) stored.get("eligible") : existingEligible;
      if (!Objects.equals(storedEligible, newEligibility.getEligible())) {
        return true;
      }
      if (!Objects.equals(toInt(stored.get("dpd")), newEligibility.getDpd())) {
        return true;
      }
      if (!Objects.equals(
          toInt(stored.get("residualTenure")), newEligibility.getResidualTenure())) {
        return true;
      }
      if (!Objects.equals(toDouble(stored.get("pos")), newEligibility.getPos())) {
        return true;
      }
      if (!Objects.equals(toDouble(stored.get("tos")), newEligibility.getTos())) {
        return true;
      }
      return false;
    } catch (Exception e) {
      log.info("Failed to parse eligibility_data for comparison: {}", e.getMessage());
      return true;
    }
  }

  private Integer toInt(Object o) {
    if (o == null) return null;
    if (o instanceof Number n) return n.intValue();
    return null;
  }

  private Double toDouble(Object o) {
    if (o == null) return null;
    if (o instanceof Number n) return n.doubleValue();
    return null;
  }

  /** Checks eligibility: retrieveLoanForRestructure + eligibility master + risk validations. */
  private Mono<EligibilityCheckResult> checkEligibility(Long lan, Long leadId, String type) {
    log.info(CHECK_ELIGIBILITY_LOG, lan, "data_get", "WIP", "");
    return m2PApi
        .retrieveLoanForRestructure(lan)
        .flatMap(
            loanResponse -> {
              RetrieveLoanResponseDTO loan = (RetrieveLoanResponseDTO) loanResponse;
              log.info(CHECK_ELIGIBILITY_LOG, lan, "data_get", "success", "loan retrieved");
              return eligibilityMasterRepository
                  .findFirstByLan(lan)
                  .flatMap(
                      entity ->
                          loanRestructureRiskValidations(loan)
                              .map(
                                  riskResult -> {
                                    boolean eligible =
                                        Boolean.TRUE.equals(entity.getEligible())
                                            && riskResult.valid();
                                    String reason =
                                        !eligible
                                            ? (Boolean.TRUE.equals(entity.getEligible())
                                                ? riskResult.reason()
                                                : "Not eligible in master record")
                                            : null;
                                    if (eligible) {
                                      log.info(
                                          CHECK_ELIGIBILITY_LOG,
                                          lan,
                                          "eligibility_check",
                                          "success",
                                          "eligible");
                                    } else {
                                      log.warn(
                                          CHECK_ELIGIBILITY_LOG,
                                          lan,
                                          "eligibility_check",
                                          "fail",
                                          reason != null ? reason : "not eligible");
                                    }
                                    return new EligibilityCheckResult(
                                        buildEligibilityCheckResponse(loan, eligible, reason),
                                        entity.getLeadId(),
                                        entity.getClientId());
                                  }))
                  .switchIfEmpty(
                      Mono.fromCallable(
                          () -> {
                            log.warn(
                                CHECK_ELIGIBILITY_LOG,
                                lan,
                                "eligibility_check",
                                "fail",
                                "No eligibility record found for LAN");
                            return new EligibilityCheckResult(
                                buildEligibilityCheckResponse(
                                    loan, false, "No eligibility record found for LAN"),
                                leadId,
                                null);
                          }));
            })
        .onErrorResume(
            e -> {
              log.warn(
                  CHECK_ELIGIBILITY_LOG,
                  lan,
                  "data_get",
                  "fail",
                  e.getMessage() != null ? e.getMessage() : "");
              return Mono.just(
                  new EligibilityCheckResult(
                      EligibilityResponseDTO.builder()
                          .eligible(false)
                          .dpd(null)
                          .residualTenure(null)
                          .pos(null)
                          .tos(null)
                          .tentativeRps(null)
                          .requestId(null)
                          .reason("Failed to check eligibility: " + e.getMessage())
                          .build(),
                      leadId,
                      null));
            });
  }

  private EligibilityResponseDTO buildEligibilityCheckResponse(
      RetrieveLoanResponseDTO loan, boolean eligible, String reason) {
    Integer dpd = loan.getDpdDays();
    Integer residualTenure = loan.getNumberOfDueRepayments();
    Integer paidRepayments = loan.getNumberOfPaidRepayments();
    Double pos =
        loan.getSummary() != null && loan.getSummary().getPrincipalOutstanding() != null
            ? loan.getSummary().getPrincipalOutstanding()
            : null;
    Double tos =
        loan.getSummary() != null && loan.getSummary().getTotalOutstanding() != null
            ? loan.getSummary().getTotalOutstanding()
            : null;

    return EligibilityResponseDTO.builder()
        .eligible(eligible)
        .dpd(dpd)
        .residualTenure(residualTenure)
        .paidRepayments(paidRepayments)
        .pos(pos)
        .tos(tos)
        .tentativeRps(null)
        .requestId(null)
        .reason(eligible ? null : reason)
        .build();
  }

  private Mono<RiskValidationResult> loanRestructureRiskValidations(RetrieveLoanResponseDTO lan) {
    return Mono.fromCallable(
        () -> {
          if (lan == null) {
            log.info(
                RESTRUCTURE_RISK_VALIDATION,
                "Loan restructure risk validation failed: loan is null");
            return new RiskValidationResult(false, "Loan data is null");
          }
          if (riskValidationProperties == null) {
            log.info(
                RESTRUCTURE_RISK_VALIDATION,
                "Loan restructure risk validation failed: risk validation config is null");
            return new RiskValidationResult(false, "Risk validation config not available");
          }

          if (!isActualDisbursementDateInRange(lan)) {
            String reason =
                String.format(
                    "Actual disbursement date not in allowed range (%s to %s)",
                    getDisbursementStart(), getDisbursementEnd());
            log.info(
                RESTRUCTURE_RISK_VALIDATION, "Loan restructure risk validation failed: " + reason);
            return new RiskValidationResult(false, reason);
          }

          if (Boolean.TRUE.equals(lan.getIsNPA())) {
            String reason = "Loan is NPA (Non-Performing Asset)";
            log.info(
                RESTRUCTURE_RISK_VALIDATION, "Loan restructure risk validation failed: " + reason);
            return new RiskValidationResult(false, reason);
          }

          Integer dpd = lan.getDpdDays();
          if (dpd == null) {
            log.warn(
                RESTRUCTURE_RISK_VALIDATION,
                "Loan restructure risk validation failed: dpdDays is null");
            return new RiskValidationResult(false, "DPD (Days Past Due) is not available");
          }

          int dpdMin = getDpdMin();
          int dpdMaxExclusive = getDpdMaxExclusive();
          log.warn(
              RESTRUCTURE_RISK_VALIDATION,
              "DPD min and max values: " + dpdMin + "," + dpdMaxExclusive);

          if (dpd < dpdMin || dpd >= dpdMaxExclusive) {
            String reason =
                String.format("DPD %d not in allowed range [%d, %d)", dpd, dpdMin, dpdMaxExclusive);
            log.info("Loan restructure risk validation failed: {}", reason);
            return new RiskValidationResult(false, reason);
          }

          int dpdRejectThreshold = getDpdRejectThreshold();
          if (dpd >= dpdRejectThreshold) {
            String reason =
                String.format("DPD %d exceeds reject threshold (%d)", dpd, dpdRejectThreshold);
            log.info("Loan restructure risk validation failed: {}", reason);
            return new RiskValidationResult(false, reason);
          }

          Integer noOfPaidRepayment = lan.getNumberOfPaidRepayments();
          int minPaidRepayments = getMinPaidRepayments();
          if (noOfPaidRepayment == null || noOfPaidRepayment < minPaidRepayments) {
            String reason =
                String.format(
                    "Paid repayments (%s) less than required minimum (%d)",
                    noOfPaidRepayment, minPaidRepayments);
            log.info(
                RESTRUCTURE_RISK_VALIDATION, "Loan restructure risk validation failed: " + reason);
            return new RiskValidationResult(false, reason);
          }

          return new RiskValidationResult(true, null);
        });
  }

  private LocalDate getDisbursementStart() {
    return riskValidationProperties.getDisbursementStart() != null
        ? riskValidationProperties.getDisbursementStart()
        : LocalDate.of(2025, 1, 1);
  }

  private LocalDate getDisbursementEnd() {
    return riskValidationProperties.getDisbursementEnd() != null
        ? riskValidationProperties.getDisbursementEnd()
        : LocalDate.of(2025, 8, 31);
  }

  private int getDpdMin() {
    return riskValidationProperties.getDpdMin();
  }

  private int getDpdMaxExclusive() {
    return riskValidationProperties.getDpdMaxExclusive();
  }

  private int getDpdRejectThreshold() {
    return riskValidationProperties.getDpdRejectThreshold();
  }

  private int getMinPaidRepayments() {
    return riskValidationProperties.getMinPaidRepayments();
  }

  private boolean isActualDisbursementDateInRange(RetrieveLoanResponseDTO loan) {
    if (loan.getTimeline() == null || loan.getTimeline().getActualDisbursementDate() == null) {
      return false;
    }
    List<Integer> dateParts = loan.getTimeline().getActualDisbursementDate();
    if (dateParts.size() < 3) {
      return false;
    }
    try {
      int year = dateParts.get(0);
      int month = dateParts.get(1);
      int day = dateParts.get(2);
      LocalDate disbursementDate = LocalDate.of(year, month, day);
      LocalDate start = getDisbursementStart();
      LocalDate end = getDisbursementEnd();
      return !disbursementDate.isBefore(start) && !disbursementDate.isAfter(end);
    } catch (Exception e) {
      log.info("Invalid actualDisbursementDate format: {}", dateParts);
      return false;
    }
  }

  /**
   * Initiates a restructure request with M2P.
   *
   * @param lan The loan account number.
   * @param dpd Days past due (used for graceOnPrincipal and interestFreePeriod).
   * @param residualTenure The residual tenure in days.
   * @return Mono containing the M2P resourceId (reschedule request ID).
   */
  private Mono<Long> initiateRestructure(
      Long lan, Integer dpd, Integer residualTenure, Integer paidRepayments) {
    log.info(
        "Initiating restructure for LAN: {}, DPD: {}, residualTenure: {}, paidRepayments: {}",
        lan,
        dpd,
        residualTenure,
        paidRepayments);
    return Mono.defer(
            () -> {
              int currentResidualTenure = residualTenure != null ? residualTenure : 0;
              int currentPaidRepayments = paidRepayments != null ? paidRepayments : 0;
              int originalTotalTenure = currentResidualTenure + currentPaidRepayments;
              int dpdValue = dpd != null ? dpd : 0;
              int extendedDpdIncludingToday = computeExtendedDpdIncludingToday(dpdValue);
              LocalDate rescheduleFrom =
                  LocalDate.now(ZoneId.of("Asia/Kolkata")).minusDays(dpdValue);
              String rescheduleFromDate = rescheduleFrom.format(M2P_DATE_FORMATTER);
              String submittedOnDate =
                  LocalDate.now(ZoneId.of("Asia/Kolkata")).format(M2P_DATE_FORMATTER);
              int tenureForRestructuring = currentResidualTenure + extendedDpdIncludingToday;
              int selectedTenure = getNextClosestTenureFromGrid(tenureForRestructuring);
              int extraTerms = selectedTenure - extendedDpdIncludingToday;
              int newTotalTenure = originalTotalTenure + extraTerms + extendedDpdIncludingToday;
              int finalTenure = currentResidualTenure + selectedTenure;
              int payableTenure =
                  newTotalTenure - extendedDpdIncludingToday - currentPaidRepayments;

              log.info(
                  "[RESTRUCTURE] LAN: {} | ORIGINAL LOAN: "
                      + "paidEMIs: {}, remainingEMIs: {}, totalEMIs: {}",
                  lan,
                  currentPaidRepayments,
                  currentResidualTenure,
                  originalTotalTenure);

              log.info(
                  "[RESTRUCTURE] LAN: {} | RESCHEDULE DATE: IST today - DPD: {} - {} = {}",
                  lan,
                  LocalDate.now(ZoneId.of("Asia/Kolkata")),
                  dpdValue,
                  rescheduleFromDate);

              log.info(
                  "[RESTRUCTURE] LAN: {} | TENURE CALCULATION: tenureForRestructuring: {} (unpaid"
                      + " {} + ExtendedDPDIncludingToday {}), selectedTenure: {} (next closest in"
                      + " grid), extraTerms: {} (selected {} - ExtendedDPDIncludingToday {}),"
                      + " finalTenure: {} (remaining {} + selected {})",
                  lan,
                  tenureForRestructuring,
                  currentResidualTenure,
                  extendedDpdIncludingToday,
                  selectedTenure,
                  extraTerms,
                  selectedTenure,
                  extendedDpdIncludingToday,
                  finalTenure,
                  currentResidualTenure,
                  selectedTenure);

              log.info(
                  "[RESTRUCTURE] LAN: {} | AFTER RESTRUCTURE: "
                      + "newTotalEMIs: {} (original {} + extraTerms {} + grace {}), "
                      + "graceEMIs: {} (₹0 payment), payableEMIs: {}",
                  lan,
                  newTotalTenure,
                  originalTotalTenure,
                  extraTerms,
                  extendedDpdIncludingToday,
                  extendedDpdIncludingToday,
                  payableTenure);

              RescheduleInitiateRequest request =
                  RescheduleInitiateRequest.builder()
                      .loanId(lan)
                      .rescheduleFromDate(rescheduleFromDate)
                      .rescheduleReasonId(riskValidationProperties.getRescheduleReasonId())
                      .submittedOnDate(submittedOnDate)
                      .specificToInstallment(false)
                      .extraTerms(String.valueOf(extraTerms))
                      .graceOnPrincipal(String.valueOf(extendedDpdIncludingToday))
                      .interestFreePeriod(String.valueOf(extendedDpdIncludingToday))
                      .compoundGraceInterest(false)
                      .build();

              log.info(
                  "[RESTRUCTURE] LAN: {} | M2P REQUEST: rescheduleFromDate: {}, submittedOnDate:"
                      + " {}, extraTerms: {}, graceOnPrincipal: {}, interestFreePeriod: {}",
                  lan,
                  rescheduleFromDate,
                  submittedOnDate,
                  extraTerms,
                  extendedDpdIncludingToday,
                  extendedDpdIncludingToday);

              return m2PApi.initiateRescheduleRequest(request);
            })
        .map(RescheduleInitiateResponse::getResourceId);
  }

  private int computeExtendedDpdIncludingToday(Integer dpd) {
    int base = dpd != null ? dpd : 0;
    return base + 1;
  }

  /**
   * Returns the next closest tenure from the grid: smallest option >= tenureForRestructuring.
   * Formula: tenureForRestructuring = unpaid tenure + DPD; selectedTenure = next in grid;
   * extraTerms = selectedTenure - DPD.
   */
  private int getNextClosestTenureFromGrid(int tenureForRestructuring) {
    List<Integer> tenureOptions = riskValidationProperties.getTenureOptions();
    for (int option : tenureOptions) {
      if (option >= tenureForRestructuring) {
        return option;
      }
    }
    return tenureOptions.get(tenureOptions.size() - 1);
  }

  private Long parseLan(String leadId) {
    try {
      return Long.parseLong(leadId);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /**
   * Fetches clientId and leadId from getLanDetails (CollectionService). Extracts client ->
   * clientId, lead -> loanId. Returns nulls if not found.
   */
  private Mono<ClientIdLeadId> getClientIdAndLeadIdFromLanDetails(Long lan) {
    return collectionService
        .getLanDetails(String.valueOf(lan))
        .map(
            lanDetailsList -> {
              if (lanDetailsList == null || lanDetailsList.isEmpty()) {
                return new ClientIdLeadId(null, null, null, null);
              }
              M2pLanDetails first = lanDetailsList.get(0);
              return new ClientIdLeadId(
                  first.getClientId(), first.getLeadId(), first.getName(), first.getMobileNumber());
            })
        .onErrorResume(
            e -> {
              log.info("Failed to get LAN details for lan {}: {}", lan, e.getMessage());
              return Mono.just(new ClientIdLeadId(null, null, null, null));
            });
  }

  private Mono<Long> saveRestructureDetails(
      Long lan,
      Long lead,
      Long client,
      String customerName,
      String mobileNumber,
      EligibilityResponseDTO eligibility,
      Integer restructureRequestId) {
    Map<String, Object> eligibilityData =
        Map.of(
            "eligible", eligibility.getEligible() != null ? eligibility.getEligible() : false,
            "dpd", eligibility.getDpd() != null ? eligibility.getDpd() : 0,
            "residualTenure",
                eligibility.getResidualTenure() != null ? eligibility.getResidualTenure() : 0,
            "pos", eligibility.getPos() != null ? eligibility.getPos() : 0.0,
            "tos", eligibility.getTos() != null ? eligibility.getTos() : 0.0);
    return Mono.fromCallable(
            () -> {
              try {
                return objectMapper.writeValueAsString(eligibilityData);
              } catch (JsonProcessingException e) {
                log.info("Failed to serialize eligibility data: {}", e.getMessage());
                throw new IllegalStateException("Failed to serialize eligibility data", e);
              }
            })
        .map(
            json ->
                LoanApplicationRestructureDetailsEntity.builder()
                    .lan(lan)
                    .lead(lead)
                    .client(client)
                    .customerName(customerName)
                    .mobileNumber(mobileNumber)
                    .eligibility(eligibility.getEligible())
                    .eligibilityData(Json.of(json))
                    .restructure(
                        Boolean.TRUE.equals(eligibility.getEligible())
                            ? RestructureStatus.NOT_TRIGGERED.name()
                            : null)
                    .restructureId(
                        restructureRequestId != null ? restructureRequestId.longValue() : null)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build())
        .flatMap(restructureDetailsRepository::save)
        .map(LoanApplicationRestructureDetailsEntity::getId);
  }

  @Override
  public Mono<ApproveRestructureResponseDTO> approveRestructure(String lan, String requestId) {
    log.info("[APPROVE_RESTRUCTURE] LAN: {}, requestId: {}", lan, requestId);

    Long lanId = parseLan(lan);
    Long dbRequestId = parseLan(requestId);

    if (lanId == null) {
      return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid LAN: " + lan));
    }
    if (dbRequestId == null) {
      return Mono.error(
          new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid requestId: " + requestId));
    }

    return restructureDetailsRepository
        .findById(dbRequestId)
        .switchIfEmpty(
            Mono.error(
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Restructure request not found for requestId: " + requestId)))
        .flatMap(
            existingRecord -> {
              log.info(
                  "[APPROVE_RESTRUCTURE] Found record: id: {}, lan: {}, restructureId: {},"
                      + " eligibility: {}, status: {}",
                  existingRecord.getId(),
                  existingRecord.getLan(),
                  existingRecord.getRestructureId(),
                  existingRecord.getEligibility(),
                  existingRecord.getRestructure());

              if (!lanId.equals(existingRecord.getLan())) {
                return Mono.just(
                    ApproveRestructureResponseDTO.builder()
                        .status("FAIL")
                        .lanId(lanId)
                        .requestId(dbRequestId)
                        .message(
                            String.format(
                                "LAN mismatch: expected %d, found %d in record",
                                lanId, existingRecord.getLan()))
                        .build());
              }

              if (!Boolean.TRUE.equals(existingRecord.getEligibility())) {
                return Mono.just(
                    ApproveRestructureResponseDTO.builder()
                        .status("FAIL")
                        .lanId(lanId)
                        .requestId(dbRequestId)
                        .message("Loan is not eligible for restructure")
                        .build());
              }

              if (!RestructureStatus.NOT_TRIGGERED.name().equals(existingRecord.getRestructure())) {
                if (RestructureStatus.SUCCESS.name().equals(existingRecord.getRestructure())) {
                  return Mono.just(
                      ApproveRestructureResponseDTO.builder()
                          .status("SUCCESS")
                          .lanId(lanId)
                          .approvedOnDate(formatApprovedDate(existingRecord.getApprovedOn()))
                          .requestId(dbRequestId)
                          .message("Already approved")
                          .build());
                }
                return Mono.just(
                    ApproveRestructureResponseDTO.builder()
                        .status("FAIL")
                        .lanId(lanId)
                        .requestId(dbRequestId)
                        .message(
                            String.format(
                                "Invalid status for approval: %s (expected NOT_TRIGGERED)",
                                existingRecord.getRestructure()))
                        .build());
              }

              Long m2pRequestId = existingRecord.getRestructureId();
              if (m2pRequestId == null) {
                return Mono.just(
                    ApproveRestructureResponseDTO.builder()
                        .status("FAIL")
                        .lanId(lanId)
                        .requestId(dbRequestId)
                        .message("No M2P restructure request ID found")
                        .build());
              }

              return processApproval(lanId, m2pRequestId, existingRecord);
            });
  }

  private Mono<ApproveRestructureResponseDTO> processApproval(
      Long lanId, Long m2pRequestId, LoanApplicationRestructureDetailsEntity existingRecord) {

    if (RestructureStatus.SUCCESS.name().equals(existingRecord.getRestructure())) {
      log.info(
          "[APPROVE_RESTRUCTURE] Already approved for LAN: {}, requestId: {}",
          lanId,
          existingRecord.getId());
      return Mono.just(
          ApproveRestructureResponseDTO.builder()
              .status("SUCCESS")
              .lanId(lanId)
              .approvedOnDate(formatApprovedDate(existingRecord.getApprovedOn()))
              .requestId(existingRecord.getId())
              .build());
    }

    return m2PApi
        .retrieveLoanForRestructure(lanId)
        .flatMap(
            loanResponse -> {
              RetrieveLoanResponseDTO loan = (RetrieveLoanResponseDTO) loanResponse;

              String validationError =
                  validateEligibilityDataNotChanged(existingRecord.getEligibilityData(), loan);
              if (validationError != null) {
                log.info(
                    "[APPROVE_RESTRUCTURE] Eligibility data changed for LAN: {}: {}",
                    lanId,
                    validationError);
                return Mono.just(
                    ApproveRestructureResponseDTO.builder()
                        .status("FAIL")
                        .lanId(lanId)
                        .requestId(existingRecord.getId())
                        .message(validationError)
                        .build());
              }

              log.info(
                  "[APPROVE_RESTRUCTURE] Validation passed, setting status to INITIATED for LAN:"
                      + " {}",
                  lanId);
              existingRecord.setRestructure(RestructureStatus.INITIATED.name());
              existingRecord.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

              return restructureDetailsRepository
                  .save(existingRecord)
                  .flatMap(
                      savedRecord -> waiveChargesBeforeApproval(lanId, m2pRequestId, savedRecord));
            })
        .onErrorResume(
            ex -> {
              log.error(
                  "[APPROVE_RESTRUCTURE] Error during validation for LAN: {}: {}",
                  lanId,
                  ex.getMessage());
              return Mono.just(
                  ApproveRestructureResponseDTO.builder()
                      .status("FAIL")
                      .lanId(lanId)
                      .requestId(existingRecord.getId())
                      .message("Validation failed: " + ex.getMessage())
                      .build());
            });
  }

  private Mono<ApproveRestructureResponseDTO> callM2PApproveAfterWaiver(
      Long lanId, Long m2pRequestId, LoanApplicationRestructureDetailsEntity record) {

    String approvedOnDateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
    ApproveRescheduleRequest approveRequest =
        ApproveRescheduleRequest.builder().approvedOnDate(approvedOnDateStr).build();

    log.info(
        "[APPROVE_RESTRUCTURE] Calling M2P approve API for LAN: {}, requestId: {}",
        lanId,
        m2pRequestId);

    return m2PApi
        .approveRescheduleRequest(m2pRequestId.intValue(), approveRequest)
        .flatMap(
            m2pResponse -> {
              log.info(
                  "[APPROVE_RESTRUCTURE] M2P approve success for LAN: {}, resourceId: {}",
                  lanId,
                  m2pResponse.getResourceId());
              return markApprovalSuccess(lanId, m2pRequestId, record);
            })
        .onErrorResume(
            ex -> {
              log.error(
                  "[APPROVE_RESTRUCTURE] M2P approve failed for LAN: {}: {}",
                  lanId,
                  ex.getMessage());
              record.setRestructure(RestructureStatus.FAIL.name());
              record.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
              return restructureDetailsRepository
                  .save(record)
                  .map(
                      saved ->
                          ApproveRestructureResponseDTO.builder()
                              .status("FAIL")
                              .lanId(lanId)
                              .requestId(record.getId())
                              .message("M2P approval failed: " + ex.getMessage())
                              .build());
            });
  }

  private Mono<ApproveRestructureResponseDTO> waiveChargesBeforeApproval(
      Long lanId, Long m2pRequestId, LoanApplicationRestructureDetailsEntity record) {

    log.info("[APPROVE_RESTRUCTURE] Fetching waivable charges for LAN: {}", lanId);

    return m2PApi
        .getPartialWaiveTemplate(lanId)
        .flatMap(
            charges -> {
              if (charges == null || charges.isEmpty()) {
                log.info("[APPROVE_RESTRUCTURE] No charges to waive for LAN: {}", lanId);
                return callM2PApproveAfterWaiver(lanId, m2pRequestId, record);
              }

              List<PartialWaiveChargeDTO> waivableCharges =
                  charges.stream()
                      .filter(c -> c.getAmountOutstanding() != null && c.getAmountOutstanding() > 0)
                      .toList();

              if (waivableCharges.isEmpty()) {
                log.info(
                    "[APPROVE_RESTRUCTURE] No outstanding charges to waive for LAN: {}", lanId);
                return callM2PApproveAfterWaiver(lanId, m2pRequestId, record);
              }

              log.info(
                  "[APPROVE_RESTRUCTURE] Waiving {} charges for LAN: {}",
                  waivableCharges.size(),
                  lanId);

              List<PartialWaiverRequest.ChargeWaiver> chargeWaivers =
                  waivableCharges.stream()
                      .map(
                          c ->
                              PartialWaiverRequest.ChargeWaiver.builder()
                                  .chargeId(c.getChargeId())
                                  .waiverAmount(c.getAmountOutstanding())
                                  .build())
                      .collect(Collectors.toList());

              PartialWaiverRequest waiverRequest =
                  PartialWaiverRequest.builder().charges(chargeWaivers).build();

              return m2PApi
                  .waiveCharges(lanId, waiverRequest)
                  .flatMap(
                      waiverResponse -> {
                        log.info(
                            "[APPROVE_RESTRUCTURE] Charges waived successfully for LAN: {}", lanId);
                        return callM2PApproveAfterWaiver(lanId, m2pRequestId, record);
                      })
                  .onErrorResume(
                      ex -> {
                        log.error(
                            "[APPROVE_RESTRUCTURE] Charge waiver failed for LAN: {}: {}",
                            lanId,
                            ex.getMessage());
                        record.setRestructure(RestructureStatus.FAIL.name());
                        record.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
                        return restructureDetailsRepository
                            .save(record)
                            .map(
                                saved ->
                                    ApproveRestructureResponseDTO.builder()
                                        .status("FAIL")
                                        .lanId(lanId)
                                        .requestId(record.getId())
                                        .message("Charge waiver failed: " + ex.getMessage())
                                        .build());
                      });
            })
        .onErrorResume(
            ex -> {
              log.error(
                  "[APPROVE_RESTRUCTURE] Failed to fetch waivable charges for LAN: {}, approval"
                      + " will not proceed: {}",
                  lanId,
                  ex.getMessage());
              record.setRestructure(RestructureStatus.FAIL.name());
              record.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
              return restructureDetailsRepository
                  .save(record)
                  .map(
                      saved ->
                          ApproveRestructureResponseDTO.builder()
                              .status("FAIL")
                              .lanId(lanId)
                              .requestId(record.getId())
                              .message("Failed to fetch waivable charges: " + ex.getMessage())
                              .build());
            });
  }

  private Mono<ApproveRestructureResponseDTO> markApprovalSuccess(
      Long lanId, Long m2pRequestId, LoanApplicationRestructureDetailsEntity record) {
    LocalDateTime approvedOn = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
    record.setRestructure(RestructureStatus.SUCCESS.name());
    record.setApprovedOn(approvedOn);
    record.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));

    return restructureDetailsRepository
        .save(record)
        .flatMap(
            saved ->
                Mono.deferContextual(
                    ctx -> {
                      log.info(
                          "[APPROVE_RESTRUCTURE] SUCCESS: LAN: {}, requestId: {}, approvedOn: {}",
                          lanId,
                          saved.getId(),
                          approvedOn);
                      String approvedOnDate = formatApprovedDate(approvedOn);
                      String smsEffectiveDate = approvedOn.format(SMS_EFFECTIVE_DATE_FORMAT);
                      if (restructureSmsTriggerEnabled) {
                        notificationService
                            .triggerRestructureApprovalSmsAsync(
                                saved.getId(), lanId, smsEffectiveDate)
                            .contextWrite(Context.of(ctx))
                            .subscribe();
                      } else {
                        log.info(
                            "[RESTRUCTURE_SMS] trigger disabled by config for"
                                + " restructureDetailsId: {}, lan: {}",
                            saved.getId(),
                            lanId);
                      }
                      return Mono.just(
                          ApproveRestructureResponseDTO.builder()
                              .status("SUCCESS")
                              .lanId(lanId)
                              .approvedOnDate(approvedOnDate)
                              .requestId(saved.getId())
                              .build());
                    }));
  }

  private String validateEligibilityDataNotChanged(
      Json eligibilityDataJson, RetrieveLoanResponseDTO currentLoan) {
    if (eligibilityDataJson == null) {
      return "No stored eligibility data found";
    }

    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> stored =
          objectMapper.readValue(eligibilityDataJson.asString(), Map.class);

      Integer storedDpd = toInt(stored.get("dpd"));
      Integer storedResidualTenure = toInt(stored.get("residualTenure"));
      Double storedPos = toDouble(stored.get("pos"));
      Double storedTos = toDouble(stored.get("tos"));

      Integer currentDpd = currentLoan.getDpdDays();
      Integer currentResidualTenure = currentLoan.getNumberOfDueRepayments();
      Double currentPos =
          currentLoan.getSummary() != null
              ? currentLoan.getSummary().getPrincipalOutstanding()
              : null;
      Double currentTos =
          currentLoan.getSummary() != null ? currentLoan.getSummary().getTotalOutstanding() : null;

      if (!Objects.equals(storedDpd, currentDpd)) {
        return String.format(
            "DPD changed from %d to %d since eligibility check", storedDpd, currentDpd);
      }
      if (!Objects.equals(storedResidualTenure, currentResidualTenure)) {
        return String.format(
            "Residual tenure changed from %d to %d since eligibility check",
            storedResidualTenure, currentResidualTenure);
      }
      if (!Objects.equals(storedPos, currentPos)) {
        return String.format(
            "Principal outstanding changed from %.2f to %.2f since eligibility check",
            storedPos != null ? storedPos : 0.0, currentPos != null ? currentPos : 0.0);
      }
      if (!Objects.equals(storedTos, currentTos)) {
        return String.format(
            "Total outstanding changed from %.2f to %.2f since eligibility check",
            storedTos != null ? storedTos : 0.0, currentTos != null ? currentTos : 0.0);
      }

      return null;
    } catch (Exception e) {
      log.info("Failed to parse eligibility_data for validation: {}", e.getMessage());
      return "Failed to validate eligibility data: " + e.getMessage();
    }
  }

  private String formatApprovedDate(LocalDateTime approvedOn) {
    if (approvedOn == null) {
      return LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
    }
    return approvedOn.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
  }
}
