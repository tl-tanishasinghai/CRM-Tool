package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.ADDRESS_MATCH_THRESHOLD;
import static com.trillionloans.los.constant.StringConstants.BUSINESS_LOAN_CONFIG_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.NAME_MATCH_THRESHOLD;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;

import com.trillionloans.los.api.partner.AnalyticApi;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.BusinessLoanAllDocumentTag;
import com.trillionloans.los.constant.DocumentEvaluationStatus;
import com.trillionloans.los.constant.LoanType;
import com.trillionloans.los.model.dto.BusinessLoanOcrMessage;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.BusinessLoanDetails;
import com.trillionloans.los.model.entity.BusinessLoanDocument;
import com.trillionloans.los.model.entity.BusinessLoanDocumentEvaluation;
import com.trillionloans.los.model.entity.LoanTypeClassification;
import com.trillionloans.los.model.request.AnalyticAddressSimilarityRequest;
import com.trillionloans.los.model.request.AnalyticNameSimilarityRequest;
import com.trillionloans.los.model.request.BusinessLoanDetailsDTO;
import com.trillionloans.los.model.request.BusinessLoanDocumentItemDTO;
import com.trillionloans.los.model.request.m2p.M2pDisbursementCallBackRequest;
import com.trillionloans.los.model.response.AnalyticAddressSimilarityResponse;
import com.trillionloans.los.model.response.AnalyticNameSimilarityResponse;
import com.trillionloans.los.model.response.m2p.M2pDocumentsUploadResponseDTO;
import com.trillionloans.los.repository.BusinessLoanDetailsRepository;
import com.trillionloans.los.repository.BusinessLoanDocumentEvaluationRepository;
import com.trillionloans.los.repository.BusinessLoanDocumentRepository;
import com.trillionloans.los.repository.LoanTypeClassificationRepository;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

@Service
@AllArgsConstructor
@Slf4j
public class BusinessLoanEvaluationService {

  private static final String BUSINESS_LOAN_EVALUATION_LOG = "[BUSINESS_LOAN_EVALUATION]";
  private static final int REQUIRED_QUALIFIED_DOCS = 2;
  private static final int REQUIRED_EVALUATED_DOCS = 2;
  private static final String APPLICATION_DISBURSE = "APPLICATION_DISBURSE";
  private static final String DISBURSE_INITIATED = "DISBURSE_INITIATED";
  private static final String LOAN_CLOSED = "LOAN_CLOSED";
  private static final String UPDATE_BUSINESS_LOAN_ASYNC = "[UPDATE_BUSINESS_LOAN]";

  /**
   * Tags that participate in analytics + global trillion_status (BUSINESS_LOAN_CONFIG
   * allDocumentList).
   */
  private static final Set<String> ANALYTICS_SCORING_TAGS =
      Arrays.stream(BusinessLoanAllDocumentTag.values())
          .map(Enum::name)
          .collect(Collectors.toUnmodifiableSet());

  /** Used only when BUSINESS_LOAN_CONFIG conditions or product code are missing */
  private static final double DEFAULT_NAME_MATCH_THRESHOLD_PERCENT = 20.0;

  private static final double DEFAULT_ADDRESS_MATCH_THRESHOLD_PERCENT = 20.0;

  /**
   * When {@link ProductControl.Flow#getVersion()} equals this (case-insensitive), upload-list
   * prerequisite applies after analytics for tags in {@code uploadDocumentList}.
   */
  private static final String BUSINESS_LOAN_CONFIG_VERSION_V1 = "v1";

  private final BusinessLoanDetailsRepository businessLoanDetailsRepository;
  private final BusinessLoanDocumentRepository businessLoanDocumentRepository;
  private final BusinessLoanDocumentEvaluationRepository evaluationRepository;
  private final LoanTypeClassificationRepository classificationRepository;
  private final NameAddressMatchService nameAddressMatchService;
  private final ProductConfigMasterService productConfigMasterService;
  private final M2PWrapperApi m2PWrapperApi;
  private final BusinessLoanOcrPublisherService businessLoanOcrPublisherService;
  private final LoanClientLookupService loanClientLookupService;
  private final AnalyticApi analyticApi;

  /**
   * For business loan documents: when ocrProductFlag is true and tag is in ocrDocumentList,
   * publishes to SQS for async OCR processing. Otherwise, triggers direct evaluation.
   */
  public void publishBusinessLoanOcrMessagesAsync(
      M2pDocumentsUploadResponseDTO uploadResponse,
      String loanId,
      String productCode,
      ProductControl.Flow businessLoanConfig) {
    if (uploadResponse == null || uploadResponse.getDocuments() == null) {
      return;
    }
    List<String> uploadDocumentTags = businessLoanConfig.getUploadDocumentList();
    if (uploadDocumentTags == null || uploadDocumentTags.isEmpty()) {
      return;
    }

    boolean ocrEnabled =
        Boolean.TRUE.equals(businessLoanConfig.getOcrProductFlag())
            && businessLoanConfig.getOcrDocumentList() != null
            && !businessLoanConfig.getOcrDocumentList().isEmpty();
    List<String> ocrDocumentList = ocrEnabled ? businessLoanConfig.getOcrDocumentList() : List.of();

    for (M2pDocumentsUploadResponseDTO.Doc docResponse : uploadResponse.getDocuments()) {
      if (docResponse == null || docResponse.getDocumentDetails() == null) {
        continue;
      }
      String tag = docResponse.getDocumentDetails().getTag();
      if (tag == null || !uploadDocumentTags.contains(tag)) {
        continue;
      }

      boolean shouldPublishToOcr = ocrEnabled && ocrDocumentList.contains(tag);

      if (shouldPublishToOcr) {
        int documentId = docResponse.getDocumentId();
        BusinessLoanOcrMessage message =
            BusinessLoanOcrMessage.builder()
                .loanApplicationId(loanId)
                .documentId(String.valueOf(documentId))
                .tag(tag)
                .productCode(productCode)
                .build();

        businessLoanOcrPublisherService
            .publishOcrMessage(message)
            .subscribeOn(Schedulers.boundedElastic())
            .subscribe(
                published -> {
                  if (!Boolean.TRUE.equals(published)) {
                    markDocumentUploadedAndEvaluateAsync(loanId, tag);
                  }
                },
                error -> {
                  log.warn(
                      "{} Failed to publish OCR message for loanId: {}, tag: {}, falling back to"
                          + " direct evaluation",
                      BUSINESS_LOAN_EVALUATION_LOG,
                      loanId,
                      tag,
                      error);
                  markDocumentUploadedAndEvaluateAsync(loanId, tag);
                });
      } else {
        markDocumentUploadedAndEvaluateAsync(loanId, tag);
      }
    }
  }

  /**
   * From LOAN_AGREEMENT document upload: persists {@code loanStatus} as {@code lsp_status} when
   * business loan product is enabled (caller must gate). No-op if {@code loanStatus} is blank or
   * not BUSINESS_LOAN/MERCHANT_LOAN.
   */
  public void applyLspStatusFromLoanAgreementUploadAsync(
      String loanApplicationId, String productCode, String loanStatus) {
    if (loanStatus == null || loanStatus.isBlank()) {
      return;
    }
    String normalized = loanStatus.trim();
    if (!LoanType.BUSINESS_LOAN.name().equals(normalized)
        && !LoanType.MERCHANT_LOAN.name().equals(normalized)) {
      log.warn(
          "{} Ignoring invalid loanStatus for lsp_status (expected BUSINESS_LOAN or MERCHANT_LOAN):"
              + " {}",
          BUSINESS_LOAN_EVALUATION_LOG,
          loanStatus);
      return;
    }

    Map<String, String> mdcContext = MDC.getCopyOfContextMap();
    String lspValue = normalized;

    classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            classification -> {
              classification.setLspStatus(lspValue);
              if (productCode != null) {
                classification.setProductCode(productCode);
              }
              classification.setUpdatedAt(LocalDateTime.now());
              return classificationRepository.save(classification);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  LocalDateTime now = LocalDateTime.now();
                  LoanTypeClassification created =
                      LoanTypeClassification.builder()
                          .loanApplicationId(loanApplicationId)
                          .productCode(productCode)
                          .lspStatus(lspValue)
                          .trillionStatus(LoanType.DATA_PENDING.name())
                          .createdAt(now)
                          .updatedAt(now)
                          .build();
                  return classificationRepository.save(created);
                }))
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(
            saved -> {
              setMdcContext(mdcContext);
              log.info(
                  "{} lsp_status from LOAN_AGREEMENT upload for loanApplicationId: {} -> {}",
                  BUSINESS_LOAN_EVALUATION_LOG,
                  loanApplicationId,
                  lspValue);
            })
        .doOnError(
            error -> {
              setMdcContext(mdcContext);
              log.error(
                  "{} Failed lsp_status from LOAN_AGREEMENT upload for loanApplicationId: {}",
                  BUSINESS_LOAN_EVALUATION_LOG,
                  loanApplicationId,
                  error);
            })
        .doFinally(signalType -> MDC.clear())
        .subscribe();
  }

  // This method save Business name and address of users business :business_loan_details
  // along with specific name and address mentioned in business documents:business_loan_documents
  public Mono<Void> saveBusinessLoanDetailsAsync(
      String loanApplicationId, BusinessLoanDetailsDTO dto) {
    if (dto == null
        || (dto.getBusinessName() == null || dto.getBusinessName().isBlank())
        || (dto.getBusinessAddress() == null || dto.getBusinessAddress().isBlank())) {
      log.warn(
          "{} Skipping save - businessLoanDetails incomplete for loanApplicationId: {}",
          BUSINESS_LOAN_EVALUATION_LOG,
          loanApplicationId);
      return Mono.empty();
    }
    // Checking for business details
    return businessLoanDetailsRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            existing -> {
              existing.setBusinessName(dto.getBusinessName());
              existing.setBusinessAddress(dto.getBusinessAddress());
              existing.setUpdatedAt(LocalDateTime.now());
              return businessLoanDetailsRepository.save(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  BusinessLoanDetails newDetails =
                      BusinessLoanDetails.builder()
                          .loanApplicationId(loanApplicationId)
                          .businessName(dto.getBusinessName())
                          .businessAddress(dto.getBusinessAddress())
                          .build();
                  return businessLoanDetailsRepository.save(newDetails);
                }))
        .flatMap(
            saved ->
                // Adding Business Name and Address Against Tag and Loan Id
                addBusinessLoanDocumentsDetails(loanApplicationId, dto.getBusinessLoanDocuments())
                    .then(Mono.just(saved)))
        .doOnSuccess(
            ignored ->
                log.info(
                    "{} Saved business loan details for loanApplicationId: {}",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    loanApplicationId))
        .doOnError(
            error ->
                log.error(
                    "{} Failed to save business loan details for loanApplicationId: {}, error: {}",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    loanApplicationId,
                    error.getMessage()))
        .then();
  }

  /**
   * After {@link #saveBusinessLoanDetailsAsync(String, BusinessLoanDetailsDTO)} succeeds: sets
   * classification to evaluation, upserts evaluation rows, runs analytics per scoring tag, updates
   * per-tag status and final {@code trillion_status}. Fire-and-forget; does not block the HTTP
   * response.
   */
  public void afterBusinessLoanDetailsUpdateAsync(
      String loanApplicationId, BusinessLoanDetailsDTO dto) {
    if (dto == null
        || dto.getBusinessLoanDocuments() == null
        || dto.getBusinessLoanDocuments().isEmpty()) {
      return;
    }
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();
    log.info(
        "{} Starting async post-update pipeline for loanApplicationId: {}",
        UPDATE_BUSINESS_LOAN_ASYNC,
        loanApplicationId);
    presistBusinessDocumentDetailsTriggerEvaluation(loanApplicationId, dto)
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(
            ignored -> {
              setMdcContext(mdcContext);
              log.info(
                  "{} Completed async post-update pipeline for loanApplicationId: {}",
                  UPDATE_BUSINESS_LOAN_ASYNC,
                  loanApplicationId);
            })
        .doOnError(
            error -> {
              setMdcContext(mdcContext);
              log.error(
                  "{} Async post-update pipeline failed for loanApplicationId: {}",
                  UPDATE_BUSINESS_LOAN_ASYNC,
                  loanApplicationId,
                  error);
            })
        .doFinally(signalType -> MDC.clear())
        .subscribe();
  }

  /** Loads client id, marks trillion_status evaluating, upserts evaluation rows, runs analytics. */
  private Mono<Void> presistBusinessDocumentDetailsTriggerEvaluation(
      String loanApplicationId, BusinessLoanDetailsDTO dto) {
    return loanClientLookupService
        .getClientIdForLoan(loanApplicationId, "BUSINESS_LOAN_UPDATE")
        .flatMap(
            clientId ->
                // Updating Evaluation status
                setClassificationTrillionStatusEvaluating(loanApplicationId).thenReturn(clientId))
        .flatMap(
            clientId ->
                // adding or updating business details to business details table
                upsertEvaluationRowsFromBusinessLoanUpdate(loanApplicationId, dto)
                    .thenReturn(clientId))
        .flatMap(
            clientId ->
                // running evaluation to get name and address similarity results
                evaluateScoringTagsAndFinalizeClassification(loanApplicationId, dto, clientId));
  }

  /** Sets {@link LoanType#EVALUATION_IN_PROGRESS} on {@code loan_type_classification}. */
  private Mono<Void> setClassificationTrillionStatusEvaluating(String loanApplicationId) {
    return classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            classification -> {
              classification.setTrillionStatus(LoanType.EVALUATION_IN_PROGRESS.name());
              classification.setUpdatedAt(LocalDateTime.now());
              return classificationRepository.save(classification);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  LocalDateTime now = LocalDateTime.now();
                  return classificationRepository.save(
                      LoanTypeClassification.builder()
                          .loanApplicationId(loanApplicationId)
                          .trillionStatus(LoanType.EVALUATION_IN_PROGRESS.name())
                          .createdAt(now)
                          .updatedAt(now)
                          .build());
                }))
        .then();
  }

  /**
   * For each document line: upsert {@code business_loan_document_evaluation} rows per tag (match
   * flags and scores unchanged here).
   */
  private Mono<Void> upsertEvaluationRowsFromBusinessLoanUpdate(
      String loanApplicationId, BusinessLoanDetailsDTO dto) {
    return Flux.fromIterable(dto.getBusinessLoanDocuments())
        .filter(item -> item != null && item.getTag() != null && !item.getTag().isBlank())
        .concatMap(
            item ->
                evaluationRepository
                    .findByLoanApplicationIdAndTag(loanApplicationId, item.getTag())
                    .flatMap(
                        existing -> {
                          existing.setUpdatedAt(LocalDateTime.now());
                          return evaluationRepository.save(existing);
                        })
                    .switchIfEmpty(
                        Mono.defer(
                            () ->
                                evaluationRepository.save(
                                    BusinessLoanDocumentEvaluation.builder()
                                        .loanApplicationId(loanApplicationId)
                                        .tag(item.getTag())
                                        .isDocumentUploaded(false)
                                        .evaluationStatus(DocumentEvaluationStatus.NOT_READY.name())
                                        .build()))))
        .then();
  }

  /**
   * For CPV / UDYAM / GST tags: call analytics, update per-tag evaluation; then set {@code
   * trillion_status} to {@link LoanType#BUSINESS_LOAN} if at least two tags qualify, else {@link
   * LoanType#MERCHANT_LOAN}.
   */
  private Mono<Void> evaluateScoringTagsAndFinalizeClassification(
      String loanApplicationId, BusinessLoanDetailsDTO dto, Integer clientId) {
    String clientIdStr = String.valueOf(clientId);
    String refName = dto.getBusinessName();
    String refAddress = dto.getBusinessAddress();

    List<BusinessLoanDocumentItemDTO> scoringDocs =
        dto.getBusinessLoanDocuments().stream()
            .filter(
                d -> d != null && d.getTag() != null && ANALYTICS_SCORING_TAGS.contains(d.getTag()))
            .toList();
    // No document found for evaluation according to tags
    if (scoringDocs.isEmpty()) {
      log.info(
          "{} No scoring-tag documents in request for loanApplicationId: {}, skipping analytics",
          UPDATE_BUSINESS_LOAN_ASYNC,
          loanApplicationId);
      return setClassificationTrillionMerchantLoan(loanApplicationId);
    }

    // Thresholds + v1 upload rules from BUSINESS_LOAN_CONFIG
    return resolveBusinessLoanAnalyticsContext(loanApplicationId)
        .flatMap(
            ctx -> {
              log.info(
                  "{} loanApplicationId: {} nameMatchThreshold={} addressMatchThreshold={}"
                      + " configVersionV1UploadRule={} uploadDocumentListSize={}",
                  UPDATE_BUSINESS_LOAN_ASYNC,
                  loanApplicationId,
                  ctx.nameMatchThresholdPercent,
                  ctx.addressMatchThresholdPercent,
                  ctx.v1UploadPrerequisite,
                  ctx.uploadDocumentList.size());
              // Persist each tag outcome, then count QUALIFIED at runtime (no second DB scan).
              return Flux.fromIterable(scoringDocs)
                  .concatMap(
                      item ->
                          executeNameAddressSimilarityAgainstDocumentDetails(
                                  loanApplicationId,
                                  refName,
                                  refAddress,
                                  item,
                                  clientIdStr,
                                  ctx.nameMatchThresholdPercent,
                                  ctx.addressMatchThresholdPercent)
                              .flatMap(
                                  (TagAnalyticsResult result) ->
                                      applyV1UploadDocumentPrerequisite(
                                              loanApplicationId, item.getTag(), result, ctx)
                                          .flatMap(
                                              (TagAnalyticsResult adjusted) ->
                                                  persistEvaluationAfterAnalytics(
                                                          loanApplicationId,
                                                          item.getTag(),
                                                          adjusted)
                                                      .then(Mono.just(adjusted)))))
                  // Docker/javac inferred Flux<Object> on this concatMap; cast matches IDEA/ECJ.
                  .cast(TagAnalyticsResult.class)
                  .filter(TagAnalyticsResult::isQualified)
                  .count()
                  .flatMap(
                      qualifiedCount -> {
                        boolean businessLoan = qualifiedCount >= REQUIRED_QUALIFIED_DOCS;
                        log.info(
                            "{} loanApplicationId: {} qualified scoring docs (this request): {},"
                                + " trillion_status -> {}",
                            UPDATE_BUSINESS_LOAN_ASYNC,
                            loanApplicationId,
                            qualifiedCount,
                            businessLoan
                                ? LoanType.BUSINESS_LOAN.name()
                                : LoanType.MERCHANT_LOAN.name());
                        return setClassificationTrillionBusinessOrMerchant(
                            loanApplicationId, businessLoan);
                      });
            });
  }

  /**
   * Thresholds, {@code uploadDocumentList}, and whether BUSINESS_LOAN_CONFIG {@code version} is
   * {@value #BUSINESS_LOAN_CONFIG_VERSION_V1} (enables upload prerequisite after analytics).
   */
  private static final class BusinessLoanAnalyticsContext {
    private final double nameMatchThresholdPercent;
    private final double addressMatchThresholdPercent;
    private final List<String> uploadDocumentList;
    private final boolean v1UploadPrerequisite;

    private BusinessLoanAnalyticsContext(
        double nameMatchThresholdPercent,
        double addressMatchThresholdPercent,
        List<String> uploadDocumentList,
        boolean v1UploadPrerequisite) {
      this.nameMatchThresholdPercent = nameMatchThresholdPercent;
      this.addressMatchThresholdPercent = addressMatchThresholdPercent;
      this.uploadDocumentList = uploadDocumentList;
      this.v1UploadPrerequisite = v1UploadPrerequisite;
    }

    static BusinessLoanAnalyticsContext defaults() {

      return new BusinessLoanAnalyticsContext(
          DEFAULT_NAME_MATCH_THRESHOLD_PERCENT,
          DEFAULT_ADDRESS_MATCH_THRESHOLD_PERCENT,
          List.of("UDYAM_CERTIFICATE", "CPV"),
          true);
    }
  }

  private Mono<BusinessLoanAnalyticsContext> resolveBusinessLoanAnalyticsContext(
      String loanApplicationId) {
    return classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            classification -> {
              String productCode = classification.getProductCode();
              if (productCode == null || productCode.isBlank()) {
                log.warn(
                    "{} No productCode on classification for loanApplicationId: {}, using default"
                        + " analytics context",
                    UPDATE_BUSINESS_LOAN_ASYNC,
                    loanApplicationId);
                return Mono.just(BusinessLoanAnalyticsContext.defaults());
              }
              return productConfigMasterService
                  .getProductConfigMasterData(productCode)
                  .map(
                      tuple ->
                          extractBusinessLoanAnalyticsContext(tuple.getT2(), loanApplicationId));
            })
        .switchIfEmpty(
            Mono.fromCallable(
                () -> {
                  log.warn(
                      "{} No loan_type_classification for loanApplicationId: {}, using default"
                          + " analytics context",
                      UPDATE_BUSINESS_LOAN_ASYNC,
                      loanApplicationId);
                  return BusinessLoanAnalyticsContext.defaults();
                }));
  }

  private BusinessLoanAnalyticsContext extractBusinessLoanAnalyticsContext(
      ProductControl productControl, String loanApplicationId) {
    ProductControl.Flow flow =
        productConfigMasterService.getFlowFromProductConfig(
            productControl, BUSINESS_LOAN_CONFIG_IDENTIFIER);
    if (flow == null) {
      log.warn(
          "{} BUSINESS_LOAN_CONFIG flow missing for loanApplicationId: {}, using defaults",
          UPDATE_BUSINESS_LOAN_ASYNC,
          loanApplicationId);
      return BusinessLoanAnalyticsContext.defaults();
    }
    double name = DEFAULT_NAME_MATCH_THRESHOLD_PERCENT;
    double address = DEFAULT_ADDRESS_MATCH_THRESHOLD_PERCENT;
    if (flow.getConditions() == null) {
      log.warn(
          "{} BUSINESS_LOAN_CONFIG conditions missing for loanApplicationId: {}, using default"
              + " thresholds",
          UPDATE_BUSINESS_LOAN_ASYNC,
          loanApplicationId);
    } else {
      Map<String, Object> conditions = flow.getConditions();
      name =
          parseConfigThreshold(
              conditions.get(NAME_MATCH_THRESHOLD),
              DEFAULT_NAME_MATCH_THRESHOLD_PERCENT,
              NAME_MATCH_THRESHOLD);
      address =
          parseConfigThreshold(
              conditions.get(ADDRESS_MATCH_THRESHOLD),
              DEFAULT_ADDRESS_MATCH_THRESHOLD_PERCENT,
              ADDRESS_MATCH_THRESHOLD);
    }
    List<String> uploadList =
        flow.getUploadDocumentList() != null
            ? flow.getUploadDocumentList()
            : Collections.emptyList();
    boolean v1 =
        flow.getVersion() != null
            && BUSINESS_LOAN_CONFIG_VERSION_V1.equalsIgnoreCase(flow.getVersion().trim());
    return new BusinessLoanAnalyticsContext(name, address, uploadList, v1);
  }

  /**
   * BUSINESS_LOAN_CONFIG v1: tag in {@code uploadDocumentList} and {@code is_document_uploaded}
   * false → {@link AnalyticsOutcome#NOT_QUALIFIED}, preserving analytics scores/flags.
   */
  private Mono<TagAnalyticsResult> applyV1UploadDocumentPrerequisite(
      String loanApplicationId,
      String tag,
      TagAnalyticsResult result,
      BusinessLoanAnalyticsContext ctx) {
    if (!ctx.v1UploadPrerequisite
        || ctx.uploadDocumentList == null
        || !ctx.uploadDocumentList.contains(tag)) {
      return Mono.just(result);
    }
    return evaluationRepository
        .findByLoanApplicationIdAndTag(loanApplicationId, tag)
        .map(ev -> Boolean.TRUE.equals(ev.getIsDocumentUploaded()))
        .defaultIfEmpty(false)
        .map(
            uploaded -> {
              if (Boolean.TRUE.equals(uploaded)) {
                return result;
              }
              log.info(
                  "{} v1 upload prerequisite: tag {} in uploadDocumentList but is_document_uploaded"
                      + " is false → NOT_QUALIFIED (loanApplicationId={})",
                  UPDATE_BUSINESS_LOAN_ASYNC,
                  tag,
                  loanApplicationId);
              return new TagAnalyticsResult(
                  AnalyticsOutcome.NOT_QUALIFIED,
                  result.nameOk,
                  result.addressOk,
                  result.nameMatchScore,
                  result.addressMatchScore);
            });
  }

  private static double parseConfigThreshold(Object raw, double fallback, String conditionKey) {
    if (raw == null) {
      return fallback;
    }
    try {
      if (raw instanceof Number) {
        return ((Number) raw).doubleValue();
      }
      return Double.parseDouble(raw.toString().trim());
    } catch (NumberFormatException e) {
      log.warn(
          "{} Invalid {} value [{}], using fallback {}",
          UPDATE_BUSINESS_LOAN_ASYNC,
          conditionKey,
          raw,
          fallback);
      return fallback;
    }
  }

  private Mono<Void> setClassificationTrillionBusinessOrMerchant(
      String loanApplicationId, boolean businessLoan) {
    String status = businessLoan ? LoanType.BUSINESS_LOAN.name() : LoanType.MERCHANT_LOAN.name();
    return classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            c -> {
              c.setTrillionStatus(status);
              c.setUpdatedAt(LocalDateTime.now());
              return classificationRepository.save(c);
            })
        .then();
  }

  private Mono<Void> setClassificationTrillionMerchantLoan(String loanApplicationId) {
    return setClassificationTrillionBusinessOrMerchant(loanApplicationId, false);
  }

  /** Result of analytics for one tag. */
  private enum AnalyticsOutcome {
    QUALIFIED,
    NOT_QUALIFIED,
    FAIL
  }

  private static final class TagAnalyticsResult {
    private final AnalyticsOutcome outcome;
    private final boolean nameOk;
    private final boolean addressOk;

    /** Raw name similarity % from analytics; null if unavailable. */
    private final Double nameMatchScore;

    /** Raw address similarity % from analytics; null if unavailable. */
    private final Double addressMatchScore;

    TagAnalyticsResult(
        AnalyticsOutcome outcome,
        boolean nameOk,
        boolean addressOk,
        Double nameMatchScore,
        Double addressMatchScore) {
      this.outcome = outcome;
      this.nameOk = nameOk;
      this.addressOk = addressOk;
      this.nameMatchScore = nameMatchScore;
      this.addressMatchScore = addressMatchScore;
    }

    boolean isQualified() {
      return outcome == AnalyticsOutcome.QUALIFIED;
    }
  }

  // Method executes name and address similarity against each document, comines results - threshold
  // result and mark Qualified or Non Qualified
  private Mono<TagAnalyticsResult> executeNameAddressSimilarityAgainstDocumentDetails(
      String loanApplicationId,
      String refName,
      String refAddress,
      BusinessLoanDocumentItemDTO item,
      String clientIdStr,
      double nameMatchThresholdPercent,
      double addressMatchThresholdPercent) {
    String tag = item.getTag();
    AnalyticNameSimilarityRequest nameReq =
        AnalyticNameSimilarityRequest.builder()
            .name1(refName)
            .name2(item.getBusinessName())
            .build();
    AnalyticAddressSimilarityRequest addrReq =
        AnalyticAddressSimilarityRequest.builder()
            .address1(refAddress)
            .address2(item.getBusinessAddress())
            .build();

    // AnalyticApi uses Mono.deferContextual + context.get(TRACE_ID); async path has no request
    // context unless we attach it here (MDC alone is not enough for Reactor Context).
    String traceId = resolveTraceIdForAnalyticsContext();

    return Mono.zip(
            analyticApi
                .checkNameSimilarity(nameReq, clientIdStr, loanApplicationId)
                .contextWrite(ctx -> ctx.put(TRACE_ID, traceId)),
            analyticApi
                .checkAddressSimilarity(addrReq, clientIdStr, loanApplicationId)
                .contextWrite(ctx -> ctx.put(TRACE_ID, traceId)))
        .map(
            tuple -> {
              AnalyticNameSimilarityResponse nameResp = tuple.getT1();
              AnalyticAddressSimilarityResponse addrResp = tuple.getT2();
              Double namePct = extractNameMatchPercent(nameResp);
              Double addrPct = extractAddressMatchPercent(addrResp);
              // FAIL only when analytics did not return a usable score for that side
              if (namePct == null || addrPct == null) {
                log.warn(
                    "{} Missing analytic score for loanApplicationId: {}, tag: {} (namePct={},"
                        + " addrPct={})",
                    UPDATE_BUSINESS_LOAN_ASYNC,
                    loanApplicationId,
                    tag,
                    namePct,
                    addrPct);
                return new TagAnalyticsResult(
                    AnalyticsOutcome.FAIL, false, false, namePct, addrPct);
              }
              boolean nameOk = namePct >= nameMatchThresholdPercent;
              boolean addrOk = addrPct >= addressMatchThresholdPercent;
              boolean qualified = nameOk && addrOk;
              return new TagAnalyticsResult(
                  qualified ? AnalyticsOutcome.QUALIFIED : AnalyticsOutcome.NOT_QUALIFIED,
                  nameOk,
                  addrOk,
                  namePct,
                  addrPct);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "{} Analytics call failed for loanApplicationId: {}, tag: {}",
                  UPDATE_BUSINESS_LOAN_ASYNC,
                  loanApplicationId,
                  tag,
                  error);
              return Mono.just(
                  new TagAnalyticsResult(AnalyticsOutcome.FAIL, false, false, null, null));
            });
  }

  private static String resolveTraceIdForAnalyticsContext() {
    String fromMdc = MDC.get(TRACE_ID);
    return (fromMdc != null && !fromMdc.isBlank()) ? fromMdc : UUID.randomUUID().toString();
  }

  private static Double extractNameMatchPercent(AnalyticNameSimilarityResponse resp) {
    if (resp == null) {
      return null;
    }
    return resp.getNameMatchPercent();
  }

  private static Double extractAddressMatchPercent(AnalyticAddressSimilarityResponse resp) {
    if (resp == null || resp.getScore() == null || resp.getScore().isBlank()) {
      return null;
    }
    try {
      return Double.parseDouble(resp.getScore().trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Mono<Void> persistEvaluationAfterAnalytics(
      String loanApplicationId, String tag, TagAnalyticsResult result) {
    return evaluationRepository
        .findByLoanApplicationIdAndTag(loanApplicationId, tag)
        .flatMap(
            evaluation -> {
              applyAnalyticsResultToEvaluation(evaluation, result);
              return evaluationRepository.save(evaluation);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.warn(
                      "{} No evaluation row for loanApplicationId={}, tag={}; inserting from"
                          + " analytics",
                      UPDATE_BUSINESS_LOAN_ASYNC,
                      loanApplicationId,
                      tag);
                  LocalDateTime now = LocalDateTime.now();
                  BusinessLoanDocumentEvaluation created =
                      BusinessLoanDocumentEvaluation.builder()
                          .loanApplicationId(loanApplicationId)
                          .tag(tag)
                          .isDocumentUploaded(false)
                          .isNameMatched(result.nameOk)
                          .isAddressMatched(result.addressOk)
                          .nameMatchScore(toBigDecimalScore(result.nameMatchScore))
                          .addressMatchScore(toBigDecimalScore(result.addressMatchScore))
                          .evaluationStatus(evaluationStatusForAnalyticsOutcome(result.outcome))
                          .evaluatedAt(now)
                          .updatedAt(now)
                          .build();
                  return evaluationRepository.save(created);
                }))
        .then();
  }

  private static void applyAnalyticsResultToEvaluation(
      BusinessLoanDocumentEvaluation evaluation, TagAnalyticsResult result) {
    evaluation.setIsNameMatched(result.nameOk);
    evaluation.setIsAddressMatched(result.addressOk);
    evaluation.setNameMatchScore(toBigDecimalScore(result.nameMatchScore));
    evaluation.setAddressMatchScore(toBigDecimalScore(result.addressMatchScore));
    LocalDateTime now = LocalDateTime.now();
    evaluation.setEvaluatedAt(now);
    evaluation.setUpdatedAt(now);
    evaluation.setEvaluationStatus(evaluationStatusForAnalyticsOutcome(result.outcome));
  }

  private static String evaluationStatusForAnalyticsOutcome(AnalyticsOutcome outcome) {
    switch (outcome) {
      case FAIL:
        return DocumentEvaluationStatus.FAIL.name();
      case QUALIFIED:
        return DocumentEvaluationStatus.QUALIFIED.name();
      case NOT_QUALIFIED:
        return DocumentEvaluationStatus.NOT_QUALIFIED.name();
      default:
        return DocumentEvaluationStatus.NOT_QUALIFIED.name();
    }
  }

  private static BigDecimal toBigDecimalScore(Double v) {
    if (v == null) {
      return null;
    }
    return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
  }

  // This method is saving business name and address against tag and loanId in table :
  // business_loan_documents
  private Mono<Void> addBusinessLoanDocumentsDetails(
      String loanApplicationId, List<BusinessLoanDocumentItemDTO> items) {
    if (items == null || items.isEmpty()) {
      return Mono.empty();
    }
    return Flux.fromIterable(items)
        .filter(item -> item != null && item.getTag() != null && !item.getTag().isBlank())
        .concatMap(
            item ->
                businessLoanDocumentRepository
                    .findByLoanApplicationIdAndTag(loanApplicationId, item.getTag())
                    .flatMap(
                        existing -> {
                          existing.setDocumentNumber(item.getDocumentId());
                          existing.setBusinessName(item.getBusinessName());
                          existing.setBusinessAddress(item.getBusinessAddress());
                          existing.setUpdatedAt(LocalDateTime.now());
                          return businessLoanDocumentRepository.save(existing);
                        })
                    .switchIfEmpty(
                        Mono.defer(
                            () ->
                                businessLoanDocumentRepository.save(
                                    BusinessLoanDocument.builder()
                                        .loanApplicationId(loanApplicationId)
                                        .tag(item.getTag())
                                        .documentNumber(item.getDocumentId())
                                        .businessName(item.getBusinessName())
                                        .businessAddress(item.getBusinessAddress())
                                        .build()))))
        .then();
  }

  public void initializeLoanClassificationAsync(String loanApplicationId, String productCode) {
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .hasElement()
        .flatMap(
            exists -> {
              if (Boolean.TRUE.equals(exists)) {
                log.info(
                    "{} Classification already exists for loanApplicationId: {}",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    loanApplicationId);
                return Mono.empty();
              }
              LocalDateTime now = LocalDateTime.now();
              LoanTypeClassification classification =
                  LoanTypeClassification.builder()
                      .loanApplicationId(loanApplicationId)
                      .productCode(productCode)
                      .trillionStatus(LoanType.DATA_PENDING.name())
                      .createdAt(now)
                      .updatedAt(now)
                      .build();
              return classificationRepository.save(classification);
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(
            result -> {
              setMdcContext(mdcContext);
              if (result != null) {
                log.info(
                    "{} Initialized classification for loanApplicationId: {} with trillionStatus:"
                        + " {}",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    loanApplicationId,
                    LoanType.DATA_PENDING);
              }
            })
        .doOnError(
            error -> {
              setMdcContext(mdcContext);
              log.error(
                  "{} Failed to initialize classification for loanApplicationId: {}, error: {}",
                  BUSINESS_LOAN_EVALUATION_LOG,
                  loanApplicationId,
                  error.getMessage());
            })
        .subscribe();
  }

  public void triggerDocumentEvaluationAsync(String loanApplicationId, String tag) {
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    resolveAllDocumentList(loanApplicationId)
        .flatMap(
            allDocumentList -> {
              if (allDocumentList == null || !allDocumentList.contains(tag)) {
                log.warn(
                    "{} Tag {} not in product allDocumentList for loanApplicationId: {}, skipping",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    tag,
                    loanApplicationId);
                return Mono.empty();
              }
              return upsertEvaluationEntry(loanApplicationId, tag)
                  .flatMap(evaluation -> checkAndTriggerDocumentValidation(loanApplicationId, tag));
            })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error -> {
              setMdcContext(mdcContext);
              log.error(
                  "{} Error triggering evaluation for loanApplicationId: {}, tag: {}, error: {}",
                  BUSINESS_LOAN_EVALUATION_LOG,
                  loanApplicationId,
                  tag,
                  error.getMessage());
            })
        .doFinally(signalType -> MDC.clear())
        .subscribe();
  }

  public void markDocumentUploadedAndEvaluateAsync(String loanApplicationId, String tag) {
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    log.info(
        "{} Marking document uploaded for loanApplicationId: {}, tag: {}",
        BUSINESS_LOAN_EVALUATION_LOG,
        loanApplicationId,
        tag);

    upsertEvaluationEntryWithUploadFlag(loanApplicationId, tag)
        .flatMap(evaluation -> checkAndTriggerDocumentValidation(loanApplicationId, tag))
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(
            error -> {
              setMdcContext(mdcContext);
              log.error(
                  "{} Error marking document uploaded for loanApplicationId: {}, tag: {}, error:"
                      + " {}",
                  BUSINESS_LOAN_EVALUATION_LOG,
                  loanApplicationId,
                  tag,
                  error.getMessage());
            })
        .doFinally(signalType -> MDC.clear())
        .subscribe();
  }

  private Mono<BusinessLoanDocumentEvaluation> upsertEvaluationEntry(
      String loanApplicationId, String tag) {
    return evaluationRepository
        .findByLoanApplicationIdAndTag(loanApplicationId, tag)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  BusinessLoanDocumentEvaluation newEntry =
                      BusinessLoanDocumentEvaluation.builder()
                          .loanApplicationId(loanApplicationId)
                          .tag(tag)
                          .isDocumentUploaded(false)
                          .evaluationStatus(DocumentEvaluationStatus.NOT_READY.name())
                          .build();
                  return evaluationRepository.save(newEntry);
                }));
  }

  private Mono<BusinessLoanDocumentEvaluation> upsertEvaluationEntryWithUploadFlag(
      String loanApplicationId, String tag) {
    return evaluationRepository
        .findByLoanApplicationIdAndTag(loanApplicationId, tag)
        .flatMap(
            existing -> {
              existing.setIsDocumentUploaded(true);
              return evaluationRepository.save(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  BusinessLoanDocumentEvaluation newEntry =
                      BusinessLoanDocumentEvaluation.builder()
                          .loanApplicationId(loanApplicationId)
                          .tag(tag)
                          .isDocumentUploaded(true)
                          .evaluationStatus(DocumentEvaluationStatus.NOT_READY.name())
                          .build();
                  return evaluationRepository.save(newEntry);
                }));
  }

  private Mono<Tuple2<List<String>, List<String>>> resolveUploadAndDocumentIdDocumentLists(
      String loanApplicationId) {
    return classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            classification ->
                productConfigMasterService.getProductConfigMasterData(
                    classification.getProductCode()))
        .map(
            tuple -> {
              ProductControl.Flow flow =
                  productConfigMasterService.getFlowFromProductConfig(
                      tuple.getT2(), BUSINESS_LOAN_CONFIG_IDENTIFIER);
              List<String> upload =
                  flow != null && flow.getUploadDocumentList() != null
                      ? flow.getUploadDocumentList()
                      : Collections.emptyList();
              List<String> documentId =
                  flow != null && flow.getDocumentIdDocumentList() != null
                      ? flow.getDocumentIdDocumentList()
                      : Collections.emptyList();
              return Tuples.of(upload, documentId);
            })
        .defaultIfEmpty(
            Tuples.of(Collections.<String>emptyList(), Collections.<String>emptyList()));
  }

  private Mono<List<String>> resolveUploadDocumentList(String loanApplicationId) {
    return resolveUploadAndDocumentIdDocumentLists(loanApplicationId).map(tuple -> tuple.getT1());
  }

  private Mono<List<String>> resolveAllDocumentList(String loanApplicationId) {
    return classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            classification ->
                productConfigMasterService.getProductConfigMasterData(
                    classification.getProductCode()))
        .map(
            tuple -> {
              ProductControl.Flow flow =
                  productConfigMasterService.getFlowFromProductConfig(
                      tuple.getT2(), BUSINESS_LOAN_CONFIG_IDENTIFIER);
              if (flow != null && flow.getAllDocumentList() != null) {
                return flow.getAllDocumentList();
              }
              return Collections.<String>emptyList();
            })
        .defaultIfEmpty(Collections.emptyList());
  }

  private Mono<Void> checkAndTriggerDocumentValidation(String loanApplicationId, String tag) {
    log.info(
        "{} Checking prerequisites for document validation - loanApplicationId: {}, tag: {}",
        BUSINESS_LOAN_EVALUATION_LOG,
        loanApplicationId,
        tag);

    return evaluationRepository
        .findByLoanApplicationIdAndTag(loanApplicationId, tag)
        .flatMap(
            evaluation -> {
              if (shouldSkipEvaluation(evaluation)) {
                log.info(
                    "{} Document already QUALIFIED, skipping re-evaluation - loanApplicationId: {},"
                        + " tag: {}",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    loanApplicationId,
                    tag);
                return Mono.empty();
              }

              if (DocumentEvaluationStatus.NOT_QUALIFIED
                  .name()
                  .equals(evaluation.getEvaluationStatus())) {
                log.info(
                    "{} Document was NOT_QUALIFIED, attempting re-evaluation - loanApplicationId:"
                        + " {}, tag: {}",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    loanApplicationId,
                    tag);
              }

              return resolveUploadAndDocumentIdDocumentLists(loanApplicationId)
                  .flatMap(
                      tuple ->
                          checkPrerequisitesAndValidate(
                              loanApplicationId, tag, evaluation, tuple.getT1(), tuple.getT2()));
            })
        .then();
  }

  private boolean shouldSkipEvaluation(BusinessLoanDocumentEvaluation evaluation) {
    String status = evaluation.getEvaluationStatus();
    return DocumentEvaluationStatus.QUALIFIED.name().equals(status)
        || DocumentEvaluationStatus.FAIL.name().equals(status);
  }

  private Mono<Void> checkPrerequisitesAndValidate(
      String loanApplicationId,
      String tag,
      BusinessLoanDocumentEvaluation evaluation,
      List<String> uploadDocumentList,
      List<String> documentIdDocumentList) {

    return businessLoanDetailsRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            originalDetails -> {
              if (!hasOriginalDetails(originalDetails)) {
                log.info(
                    "{} Original business details not available - loanApplicationId: {}, skipping",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    loanApplicationId);
                return Mono.empty();
              }

              return businessLoanDocumentRepository
                  .findByLoanApplicationIdAndTag(loanApplicationId, tag)
                  .flatMap(
                      document -> {
                        if (!hasDocumentDetails(document)) {
                          log.info(
                              "{} Document details not complete - loanApplicationId: {}, tag: {},"
                                  + " skipping",
                              BUSINESS_LOAN_EVALUATION_LOG,
                              loanApplicationId,
                              tag);
                          return Mono.empty();
                        }

                        if (!checkUploadOrDocumentIdPrerequisite(
                            tag,
                            evaluation,
                            document,
                            uploadDocumentList,
                            documentIdDocumentList)) {
                          log.info(
                              "{} Upload/DocumentId prerequisite not met - loanApplicationId: {},"
                                  + " tag: {}, skipping",
                              BUSINESS_LOAN_EVALUATION_LOG,
                              loanApplicationId,
                              tag);
                          return Mono.empty();
                        }

                        return performValidation(
                            loanApplicationId, tag, evaluation, originalDetails, document);
                      });
            })
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.info(
                        "{} Original business details not found - loanApplicationId: {}, skipping",
                        BUSINESS_LOAN_EVALUATION_LOG,
                        loanApplicationId)))
        .then();
  }

  private boolean hasOriginalDetails(BusinessLoanDetails details) {
    return details != null
        && details.getBusinessName() != null
        && details.getBusinessAddress() != null;
  }

  private boolean hasDocumentDetails(BusinessLoanDocument document) {
    return document != null
        && document.getBusinessName() != null
        && document.getBusinessAddress() != null;
  }

  private boolean checkUploadOrDocumentIdPrerequisite(
      String tag,
      BusinessLoanDocumentEvaluation evaluation,
      BusinessLoanDocument document,
      List<String> uploadDocumentList,
      List<String> documentIdDocumentList) {
    if (uploadDocumentList != null && uploadDocumentList.contains(tag)) {
      return Boolean.TRUE.equals(evaluation.getIsDocumentUploaded());
    }
    if (documentIdDocumentList != null && documentIdDocumentList.contains(tag)) {
      return document.getDocumentNumber() != null && !document.getDocumentNumber().isEmpty();
    }
    return true;
  }

  private Mono<Void> performValidation(
      String loanApplicationId,
      String tag,
      BusinessLoanDocumentEvaluation evaluation,
      BusinessLoanDetails originalDetails,
      BusinessLoanDocument document) {

    log.info(
        "{} Performing validation for loanApplicationId: {}, tag: {}",
        BUSINESS_LOAN_EVALUATION_LOG,
        loanApplicationId,
        tag);

    return nameAddressMatchService
        .matchBusinessName(originalDetails.getBusinessName(), document.getBusinessName())
        .flatMap(
            nameMatched ->
                nameAddressMatchService
                    .matchBusinessAddress(
                        originalDetails.getBusinessAddress(), document.getBusinessAddress())
                    .map(addressMatched -> new boolean[] {nameMatched, addressMatched}))
        .flatMap(
            matchResults -> {
              boolean nameMatched = matchResults[0];
              boolean addressMatched = matchResults[1];
              boolean isQualified = nameMatched && addressMatched;

              evaluation.setIsNameMatched(nameMatched);
              evaluation.setIsAddressMatched(addressMatched);
              evaluation.setNameMatchScore(null);
              evaluation.setAddressMatchScore(null);
              evaluation.setEvaluationStatus(
                  isQualified
                      ? DocumentEvaluationStatus.QUALIFIED.name()
                      : DocumentEvaluationStatus.NOT_QUALIFIED.name());
              evaluation.setEvaluatedAt(LocalDateTime.now());

              log.info(
                  "{} Validation result for loanApplicationId: {}, tag: {} - nameMatched: {},"
                      + " addressMatched: {}, qualified: {}",
                  BUSINESS_LOAN_EVALUATION_LOG,
                  loanApplicationId,
                  tag,
                  nameMatched,
                  addressMatched,
                  isQualified);

              return evaluationRepository.save(evaluation);
            })
        .flatMap(savedEvaluation -> triggerGlobalEvaluation(loanApplicationId))
        .then();
  }

  private Mono<Void> triggerGlobalEvaluation(String loanApplicationId) {
    log.info(
        "{} Triggering global evaluation for loanApplicationId: {}",
        BUSINESS_LOAN_EVALUATION_LOG,
        loanApplicationId);

    return classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            classification -> {
              String currentFinalStatus = classification.getFinalStatus();

              if (isFinalState(currentFinalStatus)) {
                log.info(
                    "{} Loan already in final state: {} for loanApplicationId: {}, skipping",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    currentFinalStatus,
                    loanApplicationId);
                return Mono.empty();
              }

              return evaluationRepository
                  .countEvaluatedDocuments(loanApplicationId)
                  .flatMap(
                      evaluatedCount -> {
                        if (evaluatedCount < REQUIRED_EVALUATED_DOCS) {
                          log.info(
                              "{} Not enough evaluated documents ({}/{}) for loanApplicationId: {},"
                                  + " staying DATA_PENDING",
                              BUSINESS_LOAN_EVALUATION_LOG,
                              evaluatedCount,
                              REQUIRED_EVALUATED_DOCS,
                              loanApplicationId);
                          return Mono.empty();
                        }

                        return evaluationRepository
                            .countQualifiedDocuments(loanApplicationId)
                            .flatMap(
                                qualifiedCount -> {
                                  if (qualifiedCount >= REQUIRED_QUALIFIED_DOCS) {
                                    classification.setFinalStatus(LoanType.BUSINESS_LOAN.name());
                                    log.info(
                                        "{} Global evaluation result for loanApplicationId: {} -"
                                            + " qualified: {}, marking as BUSINESS_LOAN",
                                        BUSINESS_LOAN_EVALUATION_LOG,
                                        loanApplicationId,
                                        qualifiedCount);
                                    return classificationRepository.save(classification);
                                  } else {
                                    log.info(
                                        "{} Global evaluation for loanApplicationId: {} -"
                                            + " qualified: {}, staying DATA_PENDING",
                                        BUSINESS_LOAN_EVALUATION_LOG,
                                        loanApplicationId,
                                        qualifiedCount);
                                    return Mono.empty();
                                  }
                                });
                      });
            })
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.warn(
                        "{} Classification not found for loanApplicationId: {}, skipping global"
                            + " evaluation",
                        BUSINESS_LOAN_EVALUATION_LOG,
                        loanApplicationId)))
        .then();
  }

  /**
   * At disburse: (1) No entry → create with trillion_status=DATA_PENDING, lsp_status=MERCHANT_LOAN;
   * (2) Entry with final_status already BUSINESS_LOAN/MERCHANT_LOAN → no change; (3) Entry exists →
   * if lsp_status and trillion_status both BUSINESS_LOAN then final_status=BUSINESS_LOAN, else
   * final_status=MERCHANT_LOAN.
   */
  public Mono<Void> markAsTerminalLoanAtDisburse(String loanApplicationId, String productCode) {
    log.info(
        "{} Resolving loan type classification at disburse for loanApplicationId: {}",
        BUSINESS_LOAN_EVALUATION_LOG,
        loanApplicationId);

    return classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            classification -> {
              String currentFinalStatus = classification.getFinalStatus();

              // 2) Entry with final terminal state -> no change
              if (LoanType.BUSINESS_LOAN.name().equals(currentFinalStatus)) {
                log.info(
                    "{} Loan already in final status BUSINESS_LOAN for loanApplicationId: {}, no"
                        + " change",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    loanApplicationId);
                return Mono.empty();
              }
              if (LoanType.MERCHANT_LOAN.name().equals(currentFinalStatus)) {
                log.info(
                    "{} Loan already in final status MERCHANT_LOAN for loanApplicationId: {}, no"
                        + " change",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    loanApplicationId);
                return Mono.empty();
              }

              // 3) Entry exists but not terminal: only both lsp and trillion BUSINESS_LOAN ->
              // final_status=BUSINESS_LOAN, else MERCHANT_LOAN
              boolean bothBusinessLoan =
                  LoanType.BUSINESS_LOAN.name().equals(classification.getLspStatus())
                      && LoanType.BUSINESS_LOAN.name().equals(classification.getTrillionStatus());

              String newFinalStatus =
                  bothBusinessLoan ? LoanType.BUSINESS_LOAN.name() : LoanType.MERCHANT_LOAN.name();

              classification.setFinalStatus(newFinalStatus);
              if (productCode != null) {
                classification.setProductCode(productCode);
              }

              log.info(
                  "{} At disburse set final_status to {} for loanApplicationId: {} (lsp={},"
                      + " trillion={})",
                  BUSINESS_LOAN_EVALUATION_LOG,
                  newFinalStatus,
                  loanApplicationId,
                  classification.getLspStatus(),
                  classification.getTrillionStatus());

              return classificationRepository.save(classification);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  // 1) No entry -> create with trillion_status=DATA_PENDING,
                  // final_status=MERCHANT_LOAN
                  log.info(
                      "{} No classification entry for loanApplicationId: {}, creating with"
                          + " trillion_status=DATA_PENDING, final_status=MERCHANT_LOAN",
                      BUSINESS_LOAN_EVALUATION_LOG,
                      loanApplicationId);
                  LocalDateTime now = LocalDateTime.now();
                  LoanTypeClassification newClassification =
                      LoanTypeClassification.builder()
                          .loanApplicationId(loanApplicationId)
                          .productCode(productCode)
                          .trillionStatus(LoanType.DATA_PENDING.name())
                          .finalStatus(LoanType.MERCHANT_LOAN.name())
                          .createdAt(now)
                          .updatedAt(now)
                          .build();
                  return classificationRepository.save(newClassification);
                }))
        .then();
  }

  /**
   * Fire-and-forget: on disbursement callback, if {@code BUSINESS_LOAN_CONFIG.isBusinessLoan}, sets
   * {@code loan_type_classification.lan} ({@link Integer}, same as DB) from {@link
   * M2pDisbursementCallBackRequest#getLanID()} and {@code updated_at}. Inserts a row if none exists
   * (same baseline as {@link #markAsTerminalLoanAtDisburse} for new rows).
   */
  public void updateLoanTypeClassificationLanFromDisbursementCallbackAsync(
      M2pDisbursementCallBackRequest requestBody, String productCode) {
    if (requestBody == null
        || requestBody.getLoanApplicationId() == null
        || requestBody.getLanID() == null) {
      log.debug(
          "{} Skipping disbursement LAN on classification: missing loanApplicationId or lanID",
          BUSINESS_LOAN_EVALUATION_LOG);
      return;
    }
    if (productCode == null || productCode.isBlank()) {
      log.warn(
          "{} Skipping disbursement LAN on classification: productCode blank for loanAppId {}",
          BUSINESS_LOAN_EVALUATION_LOG,
          requestBody.getLoanApplicationId());
      return;
    }

    final String loanId = String.valueOf(requestBody.getLoanApplicationId());
    final Integer lanId = requestBody.getLanID();

    productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlTuple -> {
              ProductControl.Flow businessLoanConfig =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlTuple.getT2(), BUSINESS_LOAN_CONFIG_IDENTIFIER);
              if (businessLoanConfig == null
                  || !Boolean.TRUE.equals(businessLoanConfig.getIsBusinessLoan())) {
                return Mono.empty();
              }
              return classificationRepository
                  .findByLoanApplicationId(loanId)
                  .flatMap(
                      row -> {
                        row.setLan(lanId);
                        row.setUpdatedAt(LocalDateTime.now());
                        log.info(
                            "{} Disbursement callback: updating loan_type_classification.lan for"
                                + " loanApplicationId {}",
                            BUSINESS_LOAN_EVALUATION_LOG,
                            loanId);
                        return classificationRepository.save(row);
                      })
                  .switchIfEmpty(
                      Mono.defer(
                          () -> {
                            LocalDateTime now = LocalDateTime.now();
                            LoanTypeClassification inserted =
                                LoanTypeClassification.builder()
                                    .loanApplicationId(loanId)
                                    .productCode(productCode)
                                    .trillionStatus(LoanType.DATA_PENDING.name())
                                    .finalStatus(LoanType.MERCHANT_LOAN.name())
                                    .lan(lanId)
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .build();
                            log.info(
                                "{} Disbursement callback: inserting loan_type_classification with"
                                    + " lan for loanApplicationId {}",
                                BUSINESS_LOAN_EVALUATION_LOG,
                                loanId);
                            return classificationRepository.save(inserted);
                          }));
            })
        .doOnError(
            err ->
                log.error(
                    "{} Disbursement LAN update failed for loanId {}: {}",
                    BUSINESS_LOAN_EVALUATION_LOG,
                    loanId,
                    err.getMessage(),
                    err))
        .onErrorResume(err -> Mono.empty())
        .subscribe();
  }

  /**
   * One DB read for business-loan update early exit: T1 = evaluation already finalized, T2 =
   * matched status value for logging (prefer {@code trillion_status}, else {@code lsp_status}).
   */
  Mono<Boolean> getTrillionTerminalTupleForLoanUpdate(String loanApplicationId) {
    return loadTerminalClassificationForBusinessLoanUpdate(loanApplicationId);
  }

  private Mono<Boolean> loadTerminalClassificationForBusinessLoanUpdate(String loanApplicationId) {
    return classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .map(c -> isFinalState(c.getTrillionStatus()))
        .defaultIfEmpty(false);
  }

  /**
   * Get status for business loan (used in GET loan / business loan details). Returns
   * trillion_status; if no entry exists, creates one with trillion_status=DATA_PENDING and returns
   * DATA_PENDING.
   */
  public Mono<String> getOrCreateLoanTypeStatus(String loanApplicationId, String productCode) {
    return classificationRepository
        .findByLoanApplicationId(loanApplicationId)
        .map(
            classification -> {
              String trillionStatus = classification.getTrillionStatus();
              return trillionStatus != null && !trillionStatus.isBlank()
                  ? trillionStatus
                  : LoanType.DATA_PENDING.name();
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "{} No classification entry for loanApplicationId: {}, creating with"
                          + " trillion_status=DATA_PENDING",
                      BUSINESS_LOAN_EVALUATION_LOG,
                      loanApplicationId);
                  LocalDateTime now = LocalDateTime.now();
                  LoanTypeClassification newClassification =
                      LoanTypeClassification.builder()
                          .loanApplicationId(loanApplicationId)
                          .productCode(productCode)
                          .trillionStatus(LoanType.DATA_PENDING.name())
                          .createdAt(now)
                          .updatedAt(now)
                          .build();
                  return classificationRepository
                      .save(newClassification)
                      .map(
                          saved ->
                              saved.getTrillionStatus() != null
                                  ? saved.getTrillionStatus()
                                  : LoanType.DATA_PENDING.name());
                }));
  }

  private boolean isFinalState(String loanType) {
    return LoanType.BUSINESS_LOAN.name().equals(loanType)
        || LoanType.MERCHANT_LOAN.name().equals(loanType);
  }

  private void setMdcContext(Map<String, String> mdcContext) {
    if (mdcContext != null) {
      MDC.setContextMap(mdcContext);
    }
  }
}
