package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.LOAN_APPLICATION_NO;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.NotificationServiceApi;
import com.trillionloans.los.constant.DocumentTag;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.entity.CreditLineEntity;
import com.trillionloans.los.model.entity.Drawdown;
import com.trillionloans.los.model.request.NotificationRecipientRequest;
import com.trillionloans.los.model.request.NotificationRequest;
import com.trillionloans.los.model.response.ClientDetailsResponseDto;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Sends the credit-line limit approval SMS after activation. Resolves {@link
 * DocumentTag#LOAN_AGREEMENT_BNPL}, downloads the PDF from M2P, uploads to S3 (prefer {@code
 * s3.bucket.loanAgreementStorage} + {@code aws.kms.loanAgreementStorage}, else {@code
 * doc.upload.bucket-name}) with a presigned URL, URL-encodes it for var4, and triggers the
 * notification service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CreditLineLimitApprovedNotificationService {

  private static final String LOG_PREFIX_LIMIT = "CREDIT_LINE_LIMIT_SMS";
  private static final String LOG_PREFIX_DRAWDOWN = "CREDIT_LINE_DRAWDOWN_LIMIT_SMS";

  private static final String LIMIT_AGREEMENT_S3_FOLDER = "credit-line/limit-agreement";
  private static final String DRAWDOWN_AGREEMENT_S3_FOLDER = "credit-line/drawdown-agreement";

  private final M2PWrapperApi m2PWrapperApi;
  private final NotificationServiceApi notificationServiceApi;
  private final DocS3UploadService docS3UploadService;
  private final LoanApplicationService loanApplicationService;

  @Value("${notification-service.templates.credit-line-limit-approved-template:}")
  private String creditLineLimitApprovedTemplateId;

  @Value("${s3.bucket.loanAgreementStorage:}")
  private String loanAgreementStorageBucket;

  @Value("${aws.kms.loanAgreementStorage:}")
  private String loanAgreementStorageKms;

  @Value("${notification-service.templates.credit-line-drawdown-approved-template:}")
  private String creditLineDrawdownApprovedTemplateId;

  /**
   * Sends notification when {@code credit-line-limit-approved-template} is configured. Failures are
   * logged and swallowed so callers (including async) do not break upstream flows.
   */
  public Mono<Void> sendLimitApprovedNotificationIfConfigured(
      String loanApplicationId, CreditLineEntity creditLine) {
    if (StringUtils.isBlank(creditLineLimitApprovedTemplateId)) {
      log.debug(
          "[{}] template not configured, skip loanAppId={}", LOG_PREFIX_LIMIT, loanApplicationId);
      return Mono.empty().then();
    }

    return m2PWrapperApi
        .getContactDetailByloanAppId(loanApplicationId)
        .flatMap(
            contact -> {
              String mobile = formatMobileNumber(contact.getMobileNo());
              if (StringUtils.isBlank(mobile)) {
                log.warn("[{}] no mobile for loanAppId={}", LOG_PREFIX_LIMIT, loanApplicationId);
                return Mono.empty();
              }

              return loanApplicationService
                  .getClientIdForPennyDrop(loanApplicationId)
                  .flatMap(
                      clientid -> {
                        Mono<String> displayNameMono =
                            resolveDisplayName(loanApplicationId, clientid, LOG_PREFIX_LIMIT);

                        return displayNameMono.flatMap(
                            displayName ->
                                resolvePresignedUrlParamForLimitNotification(
                                        loanApplicationId, creditLine)
                                    .flatMap(
                                        encodedUrl -> {
                                          String var2 =
                                              formatCreditLimit(creditLine.getCreditLimit());
                                          String var3 = formatTenureMonths(creditLine);

                                          List<String> params = new ArrayList<>();
                                          params.add(displayName);
                                          params.add(var2);
                                          params.add(var3);
                                          params.add(encodedUrl);

                                          NotificationRequest request =
                                              NotificationRequest.builder()
                                                  .templateId(creditLineLimitApprovedTemplateId)
                                                  .recipients(
                                                      List.of(
                                                          NotificationRecipientRequest.builder()
                                                              .sms(mobile)
                                                              .referenceType(LOAN_APPLICATION_NO)
                                                              .referenceId(loanApplicationId)
                                                              .params(params)
                                                              .build()))
                                                  .build();

                                          return notificationServiceApi
                                              .triggerNotification(
                                                  request,
                                                  loanApplicationId,
                                                  Event.CREDIT_LINE_LIMIT_APPROVED_SMS)
                                              .doOnSuccess(
                                                  r ->
                                                      log.info(
                                                          "[{}] triggered for loanAppId={}",
                                                          LOG_PREFIX_LIMIT,
                                                          loanApplicationId))
                                              .onErrorResume(
                                                  err -> {
                                                    log.warn(
                                                        "[{}] notification failed loanAppId={}: {}",
                                                        LOG_PREFIX_LIMIT,
                                                        loanApplicationId,
                                                        err.getMessage());
                                                    return Mono.empty();
                                                  })
                                              .then();
                                        }));
                      });
            })
        .onErrorResume(
            err -> {
              log.warn(
                  "[{}] skipped loanAppId={}: {}",
                  LOG_PREFIX_LIMIT,
                  loanApplicationId,
                  err.getMessage());
              return Mono.empty();
            })
        .then();
  }

  /**
   * Sends notification when {@code credit-line-drawdown-approved-template} is configured. Failures
   * are logged and swallowed so callers (including async) do not break upstream flows.
   */
  public Mono<Void> sendDrawdownApprovedNotificationIfConfigured(
      String loanApplicationId, CreditLineEntity creditLine, Drawdown drawdown, String documentId) {
    if (StringUtils.isBlank(creditLineDrawdownApprovedTemplateId)) {
      log.debug(
          "[{}] Drawdown template not configured, skip loanAppId={}",
          LOG_PREFIX_DRAWDOWN,
          loanApplicationId);
      return Mono.empty();
    }
    return m2PWrapperApi
        .getContactDetailByloanAppId(loanApplicationId)
        .flatMap(
            contact -> {
              String mobile = formatMobileNumber(contact.getMobileNo());
              if (StringUtils.isBlank(mobile)) {
                log.warn(
                    "[{}] no mobile for loanAppId={} and drawdownId= {}",
                    LOG_PREFIX_DRAWDOWN,
                    loanApplicationId,
                    drawdown.getId());
                return Mono.empty();
              }
              return Mono.zip(
                      loanApplicationService
                          .getClientIdForPennyDrop(loanApplicationId)
                          .flatMap(
                              clientId ->
                                  resolveDisplayName(
                                      loanApplicationId, clientId, LOG_PREFIX_DRAWDOWN)),
                      resolvePresignedUrlParamForLimitNotification(loanApplicationId, creditLine),
                      resolvePresignedUrlParamForDrawdownNotification(
                          loanApplicationId, drawdown, documentId))
                  .flatMap(
                      tuple -> {
                        String displayName = tuple.getT1();
                        String limitUrl = tuple.getT2();
                        String drawdownUrl = tuple.getT3();
                        String formattedAmount = formatCreditLimit(drawdown.getAmount());
                        // Construct params list
                        List<String> params =
                            List.of(displayName, formattedAmount, limitUrl, drawdownUrl);
                        NotificationRequest request =
                            NotificationRequest.builder()
                                .templateId(creditLineDrawdownApprovedTemplateId)
                                .recipients(
                                    List.of(
                                        NotificationRecipientRequest.builder()
                                            .sms(mobile)
                                            .referenceId(loanApplicationId)
                                            .referenceType(LOAN_APPLICATION_NO)
                                            .params(params)
                                            .build()))
                                .build();
                        return notificationServiceApi
                            .triggerNotification(
                                request, loanApplicationId, Event.DRAWDOWN_LINE_LIMIT_APPROVED_SMS)
                            .doOnSuccess(
                                r ->
                                    log.info(
                                        "[{}] triggered for loanAppId={} and drawdownId={}",
                                        LOG_PREFIX_DRAWDOWN,
                                        loanApplicationId,
                                        drawdown.getId()))
                            .onErrorResume(
                                err -> {
                                  log.info(
                                      "[{}] notification failed for loanAppId={} and drawdownId={}:"
                                          + " {}",
                                      LOG_PREFIX_DRAWDOWN,
                                      loanApplicationId,
                                      drawdown.getId(),
                                      err.getMessage());
                                  return Mono.empty();
                                });
                      });
            })
        .onErrorResume(
            err -> {
              log.error(
                  "[{}] unexpected error for loanAppId={} and drawdownId={}: {}",
                  LOG_PREFIX_DRAWDOWN,
                  loanApplicationId,
                  drawdown.getId(),
                  err.getMessage());
              return Mono.empty();
            })
        .then();
  }

  private Mono<String> resolveDisplayName(
      String loanApplicationId, String clientId, String logPrefix) {
    if (clientId == null) {
      return Mono.just("Customer");
    }
    return m2PWrapperApi
        .getLeadData(clientId)
        .map(this::buildDisplayName)
        .onErrorResume(
            e -> {
              log.warn(
                  "[{}] could not load client name loanAppId={}, clientId={}: {}",
                  logPrefix,
                  loanApplicationId,
                  clientId,
                  e.getMessage());
              return Mono.just("Customer");
            });
  }

  private String buildDisplayName(ClientDetailsResponseDto client) {
    String first = StringUtils.trimToEmpty(client.getFirstName());
    String last = StringUtils.trimToEmpty(client.getLastName());
    String combined = StringUtils.trimToEmpty(first + " " + last);
    return StringUtils.isNotBlank(combined) ? combined : "Customer";
  }

  private DocS3UploadService.S3UploadResult uploadBnplAgreementToS3(
      byte[] pdfBytes,
      String productCode,
      String fileBaseSTartId,
      String limitAgreementS3FolderPath,
      String logPrefix) {
    String fileBase = fileBaseSTartId + "_bnpl_doc_" + UUID.randomUUID().toString().substring(0, 7);
    if (StringUtils.isNotBlank(loanAgreementStorageBucket)
        && StringUtils.isNotBlank(loanAgreementStorageKms)) {
      return docS3UploadService.uploadPdfBytesWithKmsAndGetS3Result(
          pdfBytes,
          productCode,
          limitAgreementS3FolderPath,
          fileBase,
          loanAgreementStorageBucket,
          loanAgreementStorageKms);
    }
    log.info(
        "[{}] s3.bucket.loanAgreementStorage / aws.kms.loanAgreementStorage not both set; using"
            + " doc.upload.bucket-name",
        logPrefix);
    return docS3UploadService.uploadPdfBytesAndGetS3Result(
        pdfBytes, productCode, limitAgreementS3FolderPath, fileBase);
  }

  /**
   * var4: URL-encoded presigned S3 URL. Any failure to resolve tag, fetch PDF from M2P, upload to
   * S3, or obtain a non-blank presigned URL completes as an error — SMS is not sent.
   */
  private Mono<String> resolvePresignedUrlParamForLimitNotification(
      String loanApplicationId, CreditLineEntity creditLine) {
    String productCode = StringUtils.defaultIfBlank(creditLine.getProductCode(), "credit-line");
    String agreementTag = DocumentTag.LOAN_AGREEMENT.getDisplayName();

    return findFirstLoanDocumentIdByTagValue(loanApplicationId, agreementTag, LOG_PREFIX_LIMIT)
        .switchIfEmpty(
            Mono.error(
                new IllegalStateException(
                    "["
                        + LOG_PREFIX_LIMIT
                        + "] No document with tag \""
                        + agreementTag
                        + "\" for loanAppId="
                        + loanApplicationId)))
        .flatMap(
            documentId ->
                m2PWrapperApi
                    .getDocumentByLoanIdAndDocumentId(loanApplicationId, documentId)
                    .filter(bytes -> bytes != null && bytes.length > 0)
                    .doOnNext(
                        b ->
                            log.info(
                                "[{}] fetched BNPL agreement loanAppId={}, documentId={}, bytes={}",
                                LOG_PREFIX_LIMIT,
                                loanApplicationId,
                                documentId,
                                b.length))
                    .flatMap(
                        pdfBytes ->
                            Mono.fromCallable(
                                    () ->
                                        uploadBnplAgreementToS3(
                                            pdfBytes,
                                            productCode,
                                            loanApplicationId,
                                            LIMIT_AGREEMENT_S3_FOLDER,
                                            LOG_PREFIX_LIMIT))
                                .subscribeOn(Schedulers.boundedElastic()))
                    .map(DocS3UploadService.S3UploadResult::presignedUrl)
                    .filter(StringUtils::isNotBlank)
                    .map(url -> URLEncoder.encode(url, StandardCharsets.UTF_8)))
        .switchIfEmpty(
            Mono.error(
                new IllegalStateException(
                    "["
                        + LOG_PREFIX_LIMIT
                        + "] Could not build agreement presigned URL (empty PDF from M2P or blank"
                        + " URL after S3) loanAppId="
                        + loanApplicationId)))
        .doOnError(
            e ->
                log.warn(
                    "[{}] presigned URL failed loanAppId={}: {}",
                    LOG_PREFIX_LIMIT,
                    loanApplicationId,
                    e.getMessage()));
  }

  /**
   * var4: URL-encoded presigned S3 URL. Any failure to resolve tag, fetch PDF from M2P, upload to
   * S3, or obtain a non-blank presigned URL completes as an error — SMS is not sent.
   */
  private Mono<String> resolvePresignedUrlParamForDrawdownNotification(
      String loanApplicationId, Drawdown drawdown, String documentId) {
    String productCode = StringUtils.defaultIfBlank(drawdown.getPartnerId(), "credit-line");

    return m2PWrapperApi
        .getDocumentByLoanIdAndDocumentId(loanApplicationId, documentId)
        .filter(bytes -> bytes != null && bytes.length > 0)
        .doOnNext(
            b ->
                log.info(
                    "[{}] fetched Drawdown agreement loanAppId={} drawdown={}, documentId={},"
                        + " bytes={}",
                    LOG_PREFIX_DRAWDOWN,
                    loanApplicationId,
                    drawdown.getId(),
                    documentId,
                    b.length))
        .flatMap(
            pdfBytes ->
                Mono.fromCallable(
                        () ->
                            uploadBnplAgreementToS3(
                                pdfBytes,
                                productCode,
                                String.valueOf(drawdown.getId()),
                                DRAWDOWN_AGREEMENT_S3_FOLDER,
                                LOG_PREFIX_DRAWDOWN))
                    .subscribeOn(Schedulers.boundedElastic()))
        .map(DocS3UploadService.S3UploadResult::presignedUrl)
        .filter(StringUtils::isNotBlank)
        .map(url -> URLEncoder.encode(url, StandardCharsets.UTF_8))
        .switchIfEmpty(
            Mono.error(
                new IllegalStateException(
                    "["
                        + LOG_PREFIX_DRAWDOWN
                        + "] Could not build agreement presigned URL (empty PDF from M2P or blank"
                        + " URL after S3) drawdownId="
                        + drawdown.getId())))
        .doOnError(
            e ->
                log.warn(
                    "[{}] presigned URL failed drawdownId={}: {}",
                    LOG_PREFIX_DRAWDOWN,
                    drawdown.getId(),
                    e.getMessage()));
  }

  /**
   * First document id whose tag matches {@code expectedTag}. M2P errors propagate. Invalid response
   * shape yields {@link IllegalStateException}. Empty when no matching document — {@link
   * #resolvePresignedUrlParamForLimitNotification} turns that into an error so SMS is not sent.
   */
  private Mono<String> findFirstLoanDocumentIdByTagValue(
      String loanApplicationId, String expectedTag, String logPrefix) {
    return m2PWrapperApi
        .getDocumentList(loanApplicationId)
        .flatMapMany(body -> documentMapsFromListResponse(loanApplicationId, body, logPrefix))
        .filter(docMap -> Objects.equals(expectedTag, tagValueFromDocMap(docMap)))
        .map(this::extractDocumentIdFromMap)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .next();
  }

  private static Flux<Map<String, Object>> documentMapsFromListResponse(
      String loanApplicationId, Object body, String logPrefix) {
    if (body == null) {
      return Flux.error(
          new IllegalStateException(
              "[" + logPrefix + "] M2P document list is null loanAppId=" + loanApplicationId));
    }
    if (!(body instanceof List)) {
      return Flux.error(
          new IllegalStateException(
              "["
                  + logPrefix
                  + "] M2P document list has unexpected type loanAppId="
                  + loanApplicationId
                  + " type="
                  + body.getClass().getName()));
    }
    List<?> rawList = (List<?>) body;
    return Flux.fromIterable(rawList)
        .handle(
            (item, sink) -> {
              if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> docMap = (Map<String, Object>) item;
                sink.next(docMap);
              } else {
                sink.error(
                    new IllegalStateException(
                        "["
                            + logPrefix
                            + "] M2P document list entry is not a map loanAppId="
                            + loanApplicationId));
              }
            });
  }

  private static String tagValueFromDocMap(Map<String, Object> docMap) {
    Object tv = docMap.get("tagValue");
    if (tv instanceof String) {
      return (String) tv;
    }
    Object front = docMap.get("frontImageDocument");
    if (front instanceof Map) {
      Object nested = ((Map<?, ?>) front).get("tagValue");
      if (nested instanceof String) {
        return (String) nested;
      }
    }
    return null;
  }

  private Optional<String> extractDocumentIdFromMap(Map<String, Object> docMap) {
    Object id = docMap.get("id");
    if (id == null) {
      id = docMap.get("documentId");
    }
    if (id != null && !(id instanceof Map)) {
      return Optional.of(String.valueOf(id));
    }
    Object front = docMap.get("frontImageDocument");
    if (front instanceof Map) {
      Object nestedId = ((Map<?, ?>) front).get("id");
      if (nestedId != null) {
        return Optional.of(String.valueOf(nestedId));
      }
    }
    return Optional.empty();
  }

  private static String formatCreditLimit(BigDecimal limit) {
    if (limit == null) {
      return "";
    }
    return limit.stripTrailingZeros().toPlainString();
  }

  private static String formatTenureMonths(CreditLineEntity creditLine) {
    if (creditLine.getTenureValue() == null) {
      return "";
    }
    return String.valueOf(creditLine.getTenureValue());
  }

  private static String formatMobileNumber(String mobileNumber) {
    if (StringUtils.isBlank(mobileNumber)) {
      return null;
    }
    return mobileNumber.startsWith("91") && mobileNumber.length() == 12
        ? mobileNumber
        : "91" + mobileNumber;
  }
}
