package com.trillionloans.lms.service;

import static com.trillionloans.lms.constant.StringConstants.CKYC_NOTIFICATION;
import static com.trillionloans.lms.constant.StringConstants.CLIENT_ID;
import static com.trillionloans.lms.constant.StringConstants.HISTORICAL_CKYC_NOTIFICATION;
import static com.trillionloans.lms.constant.StringConstants.LOAN_ACCOUNT_NO;
import static com.trillionloans.lms.constant.StringConstants.LOAN_AGREEMENT_NOTIFICATIONS;
import static com.trillionloans.lms.constant.StringConstants.LOAN_APPLICATION_NO;
import static com.trillionloans.lms.constant.StringConstants.LOAN_ID;
import static com.trillionloans.lms.constant.StringConstants.LOAN_NOTIFICATIONS;
import static com.trillionloans.lms.constant.StringConstants.WELCOME_NOTIFICATIONS;

import com.google.gson.Gson;
import com.trillionloans.lms.api.m2p.M2PApi;
import com.trillionloans.lms.api.partner.KycAdaptorApi;
import com.trillionloans.lms.api.partner.NotificationServiceApi;
import com.trillionloans.lms.model.entity.LoanApplicationRestructureDetailsEntity;
import com.trillionloans.lms.model.entity.LoanRestructureNotificationTrackingEntity;
import com.trillionloans.lms.model.entity.ReKycTrackerEntity;
import com.trillionloans.lms.model.request.NotificationRecipientRequest;
import com.trillionloans.lms.model.request.NotificationRequest;
import com.trillionloans.lms.model.response.M2pCkycRecipientsDetailsDTO;
import com.trillionloans.lms.model.response.M2pClosedLoanDetailsDTO;
import com.trillionloans.lms.model.response.M2pLoanAgreementDetailsDTO;
import com.trillionloans.lms.model.response.M2pNewLoansDetailsDTO;
import com.trillionloans.lms.model.response.M2pOpenLoanCountDTO;
import com.trillionloans.lms.model.response.M2pOpenLoanDetailsDTO;
import com.trillionloans.lms.repository.LoanApplicationRestructureDetailsRepository;
import com.trillionloans.lms.repository.LoanRestructureNotificationTrackingRepository;
import com.trillionloans.lms.service.db.LoanDocumentMappingService;
import com.trillionloans.lms.service.db.ReKycTrackerService;
import com.trillionloans.lms.util.EncryptionUtil;
import io.micrometer.common.util.StringUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class NotificationService {
  private final M2PApi m2PApi;
  private final NotificationServiceApi notificationServiceApi;
  private final LoanDocumentMappingService loanDocumentMappingService;
  private final KafkaNotificationProducerService kafkaNotificationProducerService;
  private final ReKycTrackerService reKycTrackerService;
  private final EncryptionUtil encryptionUtil;
  private final S3Service s3Service;
  private final KycAdaptorApi kycAdaptorApi;
  private final String loanClosureTemplateId;
  private final String nocDeliveryTemplateId;
  private final String loanClosureSchedulerDays;
  private final String nocDeliverySchedulerDays;
  private final String nocBucketName;
  private final String nocBucketKms;
  private final String documentTemplateId;
  private final String documentBucketName;
  private final String documentBucketKms;
  private final String openedDPDLoanTemplateId;
  private final Integer batchSize;
  private final String welcomeSmsTemplateId;
  private final String welcomeAndLoanAgreementTemplateId;
  private final String portalLink;
  private final Gson gson;
  private final String ckycTemplateId;
  private final String reKycTemplateId;
  private final LoanRestructureNotificationTrackingRepository
      loanRestructureNotificationTrackingRepository;
  private final LoanApplicationRestructureDetailsRepository restructureDetailsRepository;
  private final String restructureApprovalTemplateId;
  private final Integer restructureSmsMaxRetries;
  private final Long restructureSmsRetryDelayMs;
  private final String restructureDocUploadBucketName;

  public NotificationService(
      M2PApi m2PApi,
      NotificationServiceApi notificationServiceApi,
      KafkaNotificationProducerService kafkaNotificationProducerService,
      ReKycTrackerService reKycTrackerService,
      EncryptionUtil encryptionUtil,
      S3Service s3Service,
      KycAdaptorApi kycAdaptorApi,
      @Value("${notification-service.templates.loan-closure-template}")
          String loanClosureTemplateId,
      @Value("${notification-service.templates.noc-delivery-template}")
          String nocDeliveryTemplateId,
      @Value("${notification-service.templates.loan-closure-scheduler-days}")
          String loanClosureSchedulerDays,
      @Value("${notification-service.templates.noc-delivery-scheduler-days}")
          String nocDeliverySchedulerDays,
      @Value("${s3.bucket.nocStorage}") String nocBucketName,
      @Value("${notification-service.templates.loan-agreement-delivery-template}")
          String documentTemplateId,
      @Value("${aws.kms.loanAgreementStorage}") String documentBucketKms,
      @Value("${s3.bucket.loanAgreementStorage}") String documentBucketName,
      @Value("${aws.kms.nocStorage}") String nocBucketKms,
      LoanDocumentMappingService loanDocumentMappingService,
      @Value("${notification-service.batchSize}") Integer batchSize,
      @Value("${notification-service.templates.open-dpd-loan-template}")
          String openedDPDLoanTemplateId,
      @Value("${notification-service.portal-link}") String portalLink,
      @Value("${notification-service.templates.welcome-sms-template}") String welcomeSmsTemplateId,
      @Value("${notification-service.templates.ckyc-sms-template}") String ckycTemplateId,
      @Value("${notification-service.templates.re-kyc-sms-template}") String reKycTemplateId,
      @Value("${notification-service.templates.welcome-loan-agreement-whatsapp-template}")
          String welcomeAndLoanAgreementTemplateId,
      LoanRestructureNotificationTrackingRepository loanRestructureNotificationTrackingRepository,
      LoanApplicationRestructureDetailsRepository restructureDetailsRepository,
      @Value("${notification-service.templates.restructure-approval-template:" + "RESTRUCTURE_SMS}")
          String restructureApprovalTemplateId,
      @Value("${notification-service.restructure.max-retries:5}") Integer restructureSmsMaxRetries,
      @Value("${notification-service.restructure.retry-delay-ms:15000}")
          Long restructureSmsRetryDelayMs,
      @Value("${doc.upload.bucket-name:partner-loan-docs-bucket}")
          String restructureDocUploadBucketName) {
    this.m2PApi = m2PApi;
    this.notificationServiceApi = notificationServiceApi;
    this.kafkaNotificationProducerService = kafkaNotificationProducerService;
    this.reKycTrackerService = reKycTrackerService;
    this.encryptionUtil = encryptionUtil;
    this.s3Service = s3Service;
    this.kycAdaptorApi = kycAdaptorApi;
    this.loanClosureTemplateId = loanClosureTemplateId;
    this.nocDeliveryTemplateId = nocDeliveryTemplateId;
    this.loanClosureSchedulerDays = loanClosureSchedulerDays;
    this.nocDeliverySchedulerDays = nocDeliverySchedulerDays;
    this.nocBucketName = nocBucketName;
    this.nocBucketKms = nocBucketKms;
    this.loanDocumentMappingService = loanDocumentMappingService;
    this.batchSize = batchSize > 0 ? batchSize : 1000;
    this.openedDPDLoanTemplateId = openedDPDLoanTemplateId;
    this.welcomeSmsTemplateId = welcomeSmsTemplateId;
    this.portalLink = portalLink;
    this.documentTemplateId = documentTemplateId;
    this.documentBucketName = documentBucketName;
    this.documentBucketKms = documentBucketKms;
    this.welcomeAndLoanAgreementTemplateId = welcomeAndLoanAgreementTemplateId;
    this.gson = new Gson();
    this.ckycTemplateId = ckycTemplateId;
    this.reKycTemplateId = reKycTemplateId;
    this.loanRestructureNotificationTrackingRepository =
        loanRestructureNotificationTrackingRepository;
    this.restructureDetailsRepository = restructureDetailsRepository;
    this.restructureApprovalTemplateId = restructureApprovalTemplateId;
    this.restructureSmsMaxRetries = restructureSmsMaxRetries;
    this.restructureSmsRetryDelayMs = restructureSmsRetryDelayMs;
    this.restructureDocUploadBucketName = restructureDocUploadBucketName;
  }

  public Mono<?> triggerLoanClosureNotifications() {
    ZoneId asiaZone = ZoneId.of("Asia/Kolkata");
    LocalDateTime fetchDate =
        LocalDateTime.now(asiaZone).minusDays(Integer.parseInt(loanClosureSchedulerDays));

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String formattedDate = fetchDate.format(formatter);
    return m2PApi
        .getClosedLoansListBasedOnDate(formattedDate)
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.info(
                        "[{}] no closed loans(loan closure) found for chosen date: {}",
                        LOAN_NOTIFICATIONS,
                        formattedDate)))
        .flatMap(this::processLoanClosureNotification)
        .collectList();
  }

  private Mono<String> processLoanClosureNotification(M2pClosedLoanDetailsDTO loanDetails) {
    NotificationRecipientRequest recipient =
        NotificationRecipientRequest.builder()
            .sms("91" + loanDetails.getMobileNumber())
            .referenceId(loanDetails.getLoanAccountNumber())
            .referenceType(LOAN_ACCOUNT_NO)
            .params(List.of(loanDetails.getClientName(), loanDetails.getLoanAccountNumber()))
            .build();
    NotificationRequest notificationRequest =
        NotificationRequest.builder()
            .templateId(loanClosureTemplateId)
            .recipients(List.of(recipient))
            .build();
    return notificationServiceApi
        .triggerNotification(notificationRequest)
        .flatMap(
            response -> {
              log.info(
                  "[{}] triggered loan closure sms: {}, mobile: {}, response: {}",
                  LOAN_NOTIFICATIONS,
                  loanDetails.getLoanAccountNumber(),
                  loanDetails.getMobileNumber(),
                  response);
              return Mono.just(
                  "triggered loan closure sms: "
                      + loanDetails.getLoanAccountNumber()
                      + ", mobile number: "
                      + loanDetails.getMobileNumber()
                      + ", response: "
                      + response);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] error loan closure sms: {}, mobile: {}, error: {}",
                  LOAN_NOTIFICATIONS,
                  loanDetails.getLoanAccountNumber(),
                  loanDetails.getMobileNumber(),
                  error.getMessage());
              return Mono.just(
                  "error loan closure sms: "
                      + loanDetails.getLoanAccountNumber()
                      + ", mobile number: "
                      + loanDetails.getMobileNumber()
                      + ", error: "
                      + error.getMessage());
            });
  }

  public Mono<?> triggerNocDeliveryNotifications() {
    ZoneId asiaZone = ZoneId.of("Asia/Kolkata");
    LocalDateTime fetchDate =
        LocalDateTime.now(asiaZone).minusDays(Integer.parseInt(nocDeliverySchedulerDays));

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    String formattedDate = fetchDate.format(formatter);
    return m2PApi
        .getClosedLoansListBasedOnDate(formattedDate)
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.info(
                        "[{}] no closed loans(noc delivery) found for chosen date: {}",
                        LOAN_NOTIFICATIONS,
                        formattedDate)))
        .flatMap(this::processNocDeliveryNotification)
        .collectList();
  }

  private Mono<String> processNocDeliveryNotification(M2pClosedLoanDetailsDTO loanDetails) {
    NotificationRecipientRequest recipient =
        NotificationRecipientRequest.builder()
            .sms("91" + loanDetails.getMobileNumber())
            .referenceId(loanDetails.getLoanAccountNumber())
            .referenceType(LOAN_ACCOUNT_NO)
            .params(List.of(loanDetails.getClientName(), loanDetails.getLoanAccountNumber()))
            .build();
    return m2PApi
        .fetchNOCPdf(loanDetails.getLoanAccountNumber())
        .flatMap(
            data -> {
              String fileName =
                  loanDetails.getLoanAccountNumber()
                      + "_noc_"
                      + UUID.randomUUID().toString().substring(0, 7);
              return s3Service
                  .uploadContentAndFetchPreSignedUrl(
                      data,
                      fileName,
                      MediaType.APPLICATION_PDF_VALUE,
                      nocBucketName,
                      nocBucketKms,
                      "noc")
                  .flatMap(
                      responseFromS3 -> {
                        String doubleEncoded =
                            URLEncoder.encode(responseFromS3, StandardCharsets.UTF_8);
                        recipient.setUrl(doubleEncoded);
                        NotificationRequest notificationRequest =
                            NotificationRequest.builder()
                                .templateId(nocDeliveryTemplateId)
                                .recipients(List.of(recipient))
                                .build();
                        return notificationServiceApi
                            .triggerNotification(notificationRequest)
                            .flatMap(
                                response -> {
                                  log.info(
                                      "[{}] triggered noc delivery sms: {}, mobile: {}, response:"
                                          + " {}",
                                      LOAN_NOTIFICATIONS,
                                      loanDetails.getLoanAccountNumber(),
                                      loanDetails.getMobileNumber(),
                                      response);
                                  return loanDocumentMappingService
                                      .save(loanDetails.getLoanAccountNumber(), "noc/" + fileName)
                                      .flatMap(
                                          dbResponse ->
                                              Mono.just(
                                                  "triggered noc delivery sms: "
                                                      + loanDetails.getLoanAccountNumber()
                                                      + ", mobile: "
                                                      + loanDetails.getMobileNumber()
                                                      + ", response: "
                                                      + response));
                                })
                            .onErrorResume(
                                error -> {
                                  log.error(
                                      "[{}] error noc delivery sms: {}, mobile: {}, error: {}",
                                      LOAN_NOTIFICATIONS,
                                      loanDetails.getLoanAccountNumber(),
                                      loanDetails.getMobileNumber(),
                                      error.getMessage(),
                                      error);
                                  return Mono.just(
                                      "error noc delivery sms: "
                                          + loanDetails.getLoanAccountNumber()
                                          + ", mobile: "
                                          + loanDetails.getMobileNumber()
                                          + ", error: "
                                          + error.getMessage());
                                });
                      });
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] something went wrong while noc delivery sms: {}, mobile: {}, error: {}",
                  LOAN_NOTIFICATIONS,
                  loanDetails.getLoanAccountNumber(),
                  loanDetails.getMobileNumber(),
                  error.getMessage());
              return Mono.just(
                  "something went wrong while noc delivery sms: "
                      + loanDetails.getMobileNumber()
                      + ", with error: "
                      + error.getMessage());
            });
  }

  public String normalizeMobileNumber(String mobileNumber) {
    if (mobileNumber == null || mobileNumber.isBlank()) {
      return null;
    }
    return mobileNumber.startsWith("91") && mobileNumber.length() == 12
        ? mobileNumber
        : "91" + mobileNumber;
  }

  public Flux<String> triggerBulkDPDNotifications() {
    return m2PApi
        .getAllOpenLoansHavingDPDCount()
        .switchIfEmpty(
            Mono.fromRunnable(
                    () -> log.info("[{}] no opened loans found with DPD days", LOAN_NOTIFICATIONS))
                .then(Mono.just(new M2pOpenLoanCountDTO(0))))
        .flatMap(
            dpdCount -> {
              Integer totalLoanCount = dpdCount.getTotalLoanCount();
              if (totalLoanCount == 0) {
                log.info("[{}] no DPD loans to be Alerted found", LOAN_NOTIFICATIONS);
                return Mono.just("Triggered batch notification for 0 recipients");
              }
              Integer totalBatches = (int) Math.ceil((double) totalLoanCount / batchSize);
              return Flux.range(0, totalBatches)
                  .concatMap(
                      batchIndex -> {
                        Integer offset = batchIndex * batchSize;
                        log.info(
                            "[{}] Fetching loans for batch {} (offset: {}, limit: {})",
                            LOAN_NOTIFICATIONS,
                            batchIndex + 1,
                            offset,
                            batchSize);
                        return m2PApi
                            .getLoansWithDPD(batchSize, offset)
                            .as(this::processDPDBulkNotification);
                      });
            });
  }

  private Mono<String> processDPDBulkNotification(Flux<M2pOpenLoanDetailsDTO> loansFlux) {
    return loansFlux
        .map(
            loan ->
                NotificationRecipientRequest.builder()
                    .sms(normalizeMobileNumber(loan.getMobileNumber()))
                    .params(
                        List.of(
                            loan.getLoanId(),
                            "Rs." + loan.getAmount(),
                            String.valueOf(loan.getDpdDays()),
                            loan.getClientName()))
                    .build())
        .collectList()
        .flatMap(
            recipients -> {
              NotificationRequest notificationRequest =
                  NotificationRequest.builder()
                      .templateId(openedDPDLoanTemplateId)
                      .recipients(recipients)
                      .build();

              return notificationServiceApi
                  .triggerBulkNotification(notificationRequest)
                  .doOnNext(
                      response ->
                          log.info(
                              "[{}] Batch notification triggered: recipients = {}, response = {}",
                              LOAN_NOTIFICATIONS,
                              recipients.size(),
                              response))
                  .map(
                      response ->
                          "Triggered batch notification for " + recipients.size() + " recipients");
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] Error sending batch notification: {}",
                  LOAN_NOTIFICATIONS,
                  error.getMessage());
              return Mono.just("Error sending batch notification: " + error.getMessage());
            });
  }

  public Flux<String> triggerWelcomeSmsNotifications() {
    return m2PApi
        .getAllNewLoansCount()
        .switchIfEmpty(
            Mono.fromRunnable(
                    () ->
                        log.info("[{}] No new loans found in the past day", WELCOME_NOTIFICATIONS))
                .then(Mono.just(new M2pOpenLoanCountDTO(0))))
        .flatMap(
            countDTO -> {
              Integer count = countDTO.getTotalLoanCount();
              if (count == null || count == 0) {
                log.info("[{}}] totalLoanCount is null or 0", WELCOME_NOTIFICATIONS);
                return Mono.just("Triggered batch notification for 0 recipients");
              }
              int totalLoanCount = count;
              int totalBatches = (int) Math.ceil(totalLoanCount / (double) batchSize);

              return Flux.range(0, totalBatches)
                  .concatMap(
                      batchIndex -> {
                        int offset = batchIndex * batchSize;
                        log.info(
                            "[{}}] Fetching loans batch {} (offset {}, limit {})",
                            WELCOME_NOTIFICATIONS,
                            batchIndex + 1,
                            offset,
                            batchSize);

                        return m2PApi
                            .getNewLoansDetails(batchSize, offset)
                            .as(this::processWelcomeSmsNotifications);
                      });
            });
  }

  private Mono<String> processWelcomeSmsNotifications(Flux<M2pNewLoansDetailsDTO> loansFlux) {
    return loansFlux
        .flatMap(
            loan -> {
              NotificationRecipientRequest recipient =
                  NotificationRecipientRequest.builder()
                      .sms(normalizeMobileNumber(loan.getMobileNo()))
                      .params(List.of(loan.getCustomerName(), portalLink))
                      .build();

              NotificationRequest notificationRequest =
                  NotificationRequest.builder()
                      .templateId(welcomeSmsTemplateId)
                      .recipients(List.of(recipient))
                      .build();

              return kafkaNotificationProducerService
                  .sendNotification(notificationRequest)
                  .doOnSuccess(
                      result ->
                          log.info(
                              "[{}] sent welcome sms to \"mobileNo\":\"{}\" with kafka offset: {},"
                                  + " partition: {}",
                              WELCOME_NOTIFICATIONS,
                              recipient.getSms(),
                              result.getRecordMetadata().offset(),
                              result.getRecordMetadata().partition()))
                  .thenReturn("Welcome sms sent to " + recipient.getSms())
                  .onErrorResume(
                      error -> {
                        log.error(
                            "[{}] failed to send sms to \"mobileNo\":\"{}\", error: {}",
                            WELCOME_NOTIFICATIONS,
                            recipient.getSms(),
                            error.getMessage());
                        return Mono.just("failed to send sms to " + recipient.getSms());
                      });
            })
        .collectList()
        .map(results -> String.join(", ", results));
  }

  public Mono<?> triggerLoanAgreementNotifications() {
    log.info(LOAN_AGREEMENT_NOTIFICATIONS);
    return m2PApi
        .getDocumentListBasedOnDate()
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.info(
                        "[{}] no document records found for date", LOAN_AGREEMENT_NOTIFICATIONS)))
        .flatMap(this::processDocumentSmsNotification, 1)
        .collectList();
  }

  private Mono<String> processDocumentSmsNotification(M2pLoanAgreementDetailsDTO documentDetails) {

    NotificationRecipientRequest recipient =
        NotificationRecipientRequest.builder()
            .sms("91" + documentDetails.getMobileNumber())
            .referenceType(LOAN_APPLICATION_NO)
            .referenceId(documentDetails.getLoanId())
            .params(List.of(documentDetails.getClientName(), documentDetails.getStartDate()))
            .build();

    return m2PApi
        .fetchDocumentPdf(documentDetails.getLoanId(), documentDetails.getDocumentId())
        .flatMap(
            data -> {
              String fileName =
                  documentDetails.getLoanId()
                      + "_doc_"
                      + UUID.randomUUID().toString().substring(0, 7);

              return s3Service
                  .uploadContentAndFetchPreSignedUrl(
                      data,
                      fileName,
                      MediaType.APPLICATION_PDF_VALUE,
                      documentBucketName,
                      documentBucketKms,
                      "loanAgreement")
                  .flatMap(
                      responseFromS3 -> {
                        List<String> updatedParams = new ArrayList<>(recipient.getParams());
                        String doubleEncoded =
                            URLEncoder.encode(responseFromS3, StandardCharsets.UTF_8);
                        updatedParams.add(doubleEncoded);
                        recipient.setParams(updatedParams);
                        NotificationRequest notificationRequest =
                            NotificationRequest.builder()
                                .templateId(documentTemplateId)
                                .recipients(List.of(recipient))
                                .build();
                        return kafkaNotificationProducerService
                            .sendNotification(notificationRequest)
                            .flatMap(
                                response -> {
                                  log.info(
                                      "[{}] SMS triggered for document, loanId: {}, mobile: {},"
                                          + " response: {}",
                                      LOAN_AGREEMENT_NOTIFICATIONS,
                                      documentDetails.getLoanId(),
                                      documentDetails.getMobileNumber(),
                                      response);

                                  return Mono.just(
                                      "Document SMS triggered for " + documentDetails.getLoanId());
                                })
                            .onErrorResume(
                                error -> {
                                  log.error(
                                      "[{}] Error triggering SMS for document loanId: {}, error:"
                                          + " {}",
                                      LOAN_AGREEMENT_NOTIFICATIONS,
                                      documentDetails.getLoanId(),
                                      error.getMessage());
                                  return Mono.just(
                                      "Error sending document SMS for "
                                          + documentDetails.getLoanId()
                                          + " with error :  "
                                          + error.getMessage());
                                });
                      });
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] Failed to fetch/upload document for loanId: {}, error: {}",
                  LOAN_AGREEMENT_NOTIFICATIONS,
                  documentDetails.getLoanId(),
                  error.getMessage());
              return Mono.just(
                  "Failed processing document for : "
                      + documentDetails.getLoanId()
                      + " with error :  "
                      + error.getMessage());
            });
  }

  public Mono<?> triggerHistoricalCkycNotifications() {
    return m2PApi
        .getHistoricalCkycRecipients()
        .switchIfEmpty(
            Mono.fromRunnable(
                () -> log.info("[{}] no recipients found", HISTORICAL_CKYC_NOTIFICATION)))
        .as(this::processCkycSmsNotification);
  }

  public Mono<?> triggerCkycNotifications() {
    return m2PApi
        .getCkycRecipientsBasedOnDate()
        .switchIfEmpty(
            Mono.fromRunnable(
                () -> log.info("[{}] no recipients found for date", CKYC_NOTIFICATION)))
        .as(this::processCkycSmsNotification);
  }

  private Mono<String> processCkycSmsNotification(
      Flux<M2pCkycRecipientsDetailsDTO> recipientsDetails) {
    return recipientsDetails
        .flatMap(
            recipientDetail -> {
              NotificationRecipientRequest recipient =
                  NotificationRecipientRequest.builder()
                      .sms(normalizeMobileNumber(recipientDetail.getMobileNo()))
                      .referenceId(recipientDetail.getClientId())
                      .referenceType(CLIENT_ID)
                      .params(List.of(recipientDetail.getCkycId()))
                      .build();

              NotificationRequest notificationRequest =
                  NotificationRequest.builder()
                      .templateId(ckycTemplateId)
                      .recipients(List.of(recipient))
                      .build();

              return kafkaNotificationProducerService
                  .sendNotification(notificationRequest)
                  .doOnSuccess(
                      result ->
                          log.info(
                              "[{}] sent ckyc sms to \"mobileNo\":\"{}\" with kafka offset: {},"
                                  + " partition: {}",
                              CKYC_NOTIFICATION,
                              recipient.getSms(),
                              result.getRecordMetadata().offset(),
                              result.getRecordMetadata().partition()))
                  .thenReturn("CKYC sms sent to " + recipient.getSms())
                  .onErrorResume(
                      error -> {
                        log.error(
                            "[{}] failed to send ckyc sms to \"mobileNo\":\"{}\", error: {}",
                            CKYC_NOTIFICATION,
                            recipient.getSms(),
                            error.getMessage());
                        return Mono.just("failed to send ckyc sms to " + recipient.getSms());
                      });
            })
        .collectList()
        .map(results -> String.join(", ", results));
  }

  public Mono<?> processWelcomeAndLoanAgreementNotifications() {
    log.info(
        "[{}] loan agreement and welcome notifications, process triggered",
        LOAN_AGREEMENT_NOTIFICATIONS);
    return m2PApi
        .getLoanAgreementsForDisbursedLoansOneDayBack()
        .switchIfEmpty(
            Mono.fromRunnable(
                () ->
                    log.info(
                        "[{}] no loan-agreement document records found for date",
                        LOAN_AGREEMENT_NOTIFICATIONS)))
        .flatMap(this::processWelcomeAndAgreementNotification, 1)
        .collectList();
  }

  private Mono<String> processWelcomeAndAgreementNotification(
      M2pLoanAgreementDetailsDTO documentDetails) {
    NotificationRecipientRequest welcomeSmsRecipient =
        NotificationRecipientRequest.builder()
            .sms(normalizeMobileNumber(documentDetails.getMobileNumber()))
            .referenceId(documentDetails.getLoanId())
            .referenceType(LOAN_APPLICATION_NO)
            .params(List.of(documentDetails.getClientName(), portalLink))
            .build();
    NotificationRecipientRequest loanAgreementSmsRecipient =
        NotificationRecipientRequest.builder()
            .sms("91" + documentDetails.getMobileNumber())
            .referenceId(documentDetails.getLoanId())
            .referenceType(LOAN_APPLICATION_NO)
            .params(List.of(documentDetails.getClientName(), documentDetails.getStartDate()))
            .build();
    NotificationRecipientRequest welcomeAndLoanAgreementWhatsAppRecipient =
        NotificationRecipientRequest.builder()
            .sms("91" + documentDetails.getMobileNumber())
            .referenceId(documentDetails.getLoanId())
            .referenceType(LOAN_APPLICATION_NO)
            .params(
                List.of(
                    documentDetails.getClientName(),
                    roundToTwoDecimalString(documentDetails.getDisbursedAmount()),
                    maskBankAccountNumber(documentDetails.getBankAccountNumber()),
                    documentDetails.getStartDate()))
            .build();
    return m2PApi
        .fetchDocumentPdf(documentDetails.getLoanId(), documentDetails.getDocumentId())
        .flatMap(
            data -> {
              String fileName =
                  documentDetails.getLoanId()
                      + "_doc_"
                      + UUID.randomUUID().toString().substring(0, 7);
              return s3Service
                  .uploadContentAndFetchPreSignedUrl(
                      data,
                      fileName,
                      MediaType.APPLICATION_PDF_VALUE,
                      documentBucketName,
                      documentBucketKms,
                      "loanAgreement")
                  .flatMap(
                      responseFromS3 -> {
                        List<String> updatedParams =
                            new ArrayList<>(loanAgreementSmsRecipient.getParams());
                        updatedParams.add(responseFromS3);
                        loanAgreementSmsRecipient.setParams(updatedParams);
                        welcomeAndLoanAgreementWhatsAppRecipient.setUrl(responseFromS3);

                        NotificationRequest.FailOverNotification loanAgreementNotificationRequest =
                            NotificationRequest.FailOverNotification.builder()
                                .templateId(documentTemplateId)
                                .recipients(List.of(loanAgreementSmsRecipient))
                                .build();
                        NotificationRequest.FailOverNotification welcomeNotificationRequest =
                            NotificationRequest.FailOverNotification.builder()
                                .templateId(welcomeSmsTemplateId)
                                .recipients(List.of(welcomeSmsRecipient))
                                .build();
                        NotificationRequest welcomeAndLoanAgreementNotificationRequest =
                            NotificationRequest.builder()
                                .templateId(welcomeAndLoanAgreementTemplateId)
                                .recipients(List.of(welcomeAndLoanAgreementWhatsAppRecipient))
                                .failOverNotifications(
                                    List.of(
                                        loanAgreementNotificationRequest,
                                        welcomeNotificationRequest))
                                .build();
                        log.info(
                            "[{}] sending loan agreement notification: {}",
                            LOAN_AGREEMENT_NOTIFICATIONS,
                            gson.toJson(welcomeAndLoanAgreementNotificationRequest));
                        return kafkaNotificationProducerService
                            .sendNotification(welcomeAndLoanAgreementNotificationRequest)
                            .flatMap(
                                response -> {
                                  log.info(
                                      "[{}] notification triggered for loan-agreement, loanId: {},"
                                          + " mobile: {}, response: {}",
                                      LOAN_AGREEMENT_NOTIFICATIONS,
                                      documentDetails.getLoanId(),
                                      documentDetails.getMobileNumber(),
                                      response);

                                  return Mono.just(
                                      "loan-agreement notification triggered for loanId: "
                                          + documentDetails.getLoanId());
                                })
                            .onErrorResume(
                                error -> {
                                  log.error(
                                      "[{}] error triggering notification for loan-agreement,"
                                          + " loanId: {}, error: {}",
                                      LOAN_AGREEMENT_NOTIFICATIONS,
                                      documentDetails.getLoanId(),
                                      error.getMessage());
                                  return Mono.just(
                                      "error sending loan-agreement notification for "
                                          + documentDetails.getLoanId()
                                          + " with error:  "
                                          + error.getMessage());
                                });
                      });
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] failed to process loan-agreement notification for loanId: {}, error: {}",
                  LOAN_AGREEMENT_NOTIFICATIONS,
                  documentDetails.getLoanId(),
                  error.getMessage());
              return Mono.just(
                  "failed processing document for: "
                      + documentDetails.getLoanId()
                      + " with error:  "
                      + error.getMessage());
            });
  }

  public Mono<Void> syncReKycSmsTracker() {
    return reKycTrackerService.syncReKycSmsTracker();
  }

  public Mono<?> triggerReKycNotifications() {
    AtomicInteger totalProcessed = new AtomicInteger(0);
    AtomicInteger notificationsTriggered = new AtomicInteger(0);

    return reKycTrackerService
        .findAllByIsActiveTrue()
        .doOnComplete(
            () ->
                log.info(
                    "[RE_KYC_NOTIFICATION] no eligible loans found for re-kyc notification in"
                        + " re-kyc tracker."))
        .filter(
            tracker -> {
              boolean eligible =
                  tracker.getEligibleSmsCode() != null
                      && (tracker.getLastTriggerCode() == null
                          || !tracker.getLastTriggerCode().equals(tracker.getEligibleSmsCode()));

              if (!eligible) {
                log.info(
                    "[RE_KYC_NOTIFICATION] skipping clientId={}, lanId={}, eligibleSms={},"
                        + " lastTriggered={} as notification already sent.",
                    tracker.getClientId(),
                    tracker.getLanId(),
                    tracker.getEligibleSmsCode(),
                    tracker.getLastTriggerCode());
              }
              return eligible;
            })
        .flatMap(
            reKycTrackerEntity -> {
              totalProcessed.incrementAndGet();
              return fetchVcipLink(reKycTrackerEntity.getClientId())
                  .switchIfEmpty(
                      Mono.fromRunnable(
                          () ->
                              log.error(
                                  "[ERROR][RE_KYC_NOTIFICATION] no vcip link found for"
                                      + " client {}. skipping notification.",
                                  reKycTrackerEntity.getClientId())))
                  .flatMap(
                      vcipLink -> {
                        notificationsTriggered.incrementAndGet();
                        return processReKycNotification(reKycTrackerEntity, vcipLink);
                      });
            },
            200)
        .subscribeOn(Schedulers.boundedElastic())
        .then(
            Mono.defer(
                () -> {
                  int processed = totalProcessed.get();
                  int triggered = notificationsTriggered.get();

                  if (triggered == 0) {
                    log.info(
                        "[RE_KYC_NOTIFICATION] cron completed successfully. Processed {} records,"
                            + " no record were eligible today for re-kyc notifications.",
                        processed);
                  } else {
                    log.info(
                        "[RE_KYC_NOTIFICATION] cron completed successfully. triggered: {} / total"
                            + " active records: {}",
                        triggered,
                        processed);
                  }
                  return Mono.empty();
                }));
  }

  private Mono<Void> processReKycNotification(ReKycTrackerEntity eligibleLoan, String vcipLink) {
    NotificationRecipientRequest recipient =
        NotificationRecipientRequest.builder()
            .sms(normalizeMobileNumber(encryptionUtil.decrypt(eligibleLoan.getMobileNo())))
            .referenceId(eligibleLoan.getLanId())
            .referenceType(LOAN_ID)
            .params(List.of(eligibleLoan.getClientName(), eligibleLoan.getLanId(), vcipLink))
            .build();

    NotificationRequest notificationRequest =
        NotificationRequest.builder()
            .templateId(reKycTemplateId)
            .recipients(List.of(recipient))
            .build();

    return kafkaNotificationProducerService
        .sendNotification(notificationRequest)
        .then(
            reKycTrackerService.updateTriggerStatus(
                eligibleLoan.getClientId(), eligibleLoan.getEligibleSmsCode()))
        .doOnSuccess(
            v ->
                log.info(
                    "[RE_KYC_NOTIFICATION]triggered for clientId {}, smsCode {}",
                    eligibleLoan.getClientId(),
                    eligibleLoan.getEligibleSmsCode()))
        .then();
  }

  private Mono<String> fetchVcipLink(String clientId) {
    return kycAdaptorApi
        .InitiateVkyc(clientId)
        .map(
            vkycResp ->
                vkycResp != null && vkycResp.getAdditionalInfo() != null
                    ? vkycResp.getAdditionalInfo().getVcipLink()
                    : null)
        .filter(StringUtils::isNotBlank)
        .onErrorResume(
            e -> {
              log.error(
                  "[ERROR][RE_KYC_NOTIFICATION] failed to get vkyc link for"
                      + " client {}: {}. skipping notification.",
                  clientId,
                  e.getMessage());
              return Mono.empty();
            });
  }

  public Mono<Void> triggerRestructureApprovalSmsAsync(
      Long restructureDetailsId, Long lan, String effectiveDate) {
    return processRestructureApprovalSms(restructureDetailsId, lan, effectiveDate)
        .doOnSuccess(
            unused ->
                log.info(
                    "[RESTRUCTURE_SMS] async flow completed for restructureDetailsId: {}, lan: {}",
                    restructureDetailsId,
                    lan))
        .doOnError(
            error ->
                log.error(
                    "[RESTRUCTURE_SMS] async flow failed for restructureDetailsId: {}, lan: {},"
                        + " error: {}",
                    restructureDetailsId,
                    lan,
                    error.getMessage(),
                    error));
  }

  private Mono<Void> processRestructureApprovalSms(
      Long restructureDetailsId, Long lan, String effectiveDate) {
    return initializeRestructureTracking(restructureDetailsId, lan)
        .flatMap(
            tracking ->
                fetchS3PathWithRetry(tracking, nextAttempt(tracking))
                    .switchIfEmpty(
                        Mono.defer(() -> markMaxRetryExceeded(tracking).then(Mono.empty())))
                    .flatMap(
                        sourceEntity ->
                            sendRestructureApprovalSms(tracking, sourceEntity, effectiveDate)))
        .onErrorResume(
            error ->
                markFailure(restructureDetailsId, error)
                    .then(
                        Mono.fromRunnable(
                            () ->
                                log.error(
                                    "[RESTRUCTURE_SMS] failed for restructureDetailsId: {}, lan:"
                                        + " {}, error: {}",
                                    restructureDetailsId,
                                    lan,
                                    error.getMessage(),
                                    error))))
        .then();
  }

  private int nextAttempt(LoanRestructureNotificationTrackingEntity tracking) {
    return tracking.getAttemptCount() == null ? 1 : tracking.getAttemptCount() + 1;
  }

  private Mono<LoanRestructureNotificationTrackingEntity> initializeRestructureTracking(
      Long restructureDetailsId, Long lan) {
    return loanRestructureNotificationTrackingRepository
        .findTopByRestructureDetailsIdOrderByIdDesc(restructureDetailsId)
        .defaultIfEmpty(
            LoanRestructureNotificationTrackingEntity.builder()
                .restructureDetailsId(restructureDetailsId)
                .loanAccountNumber(lan)
                .createdAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
                .build())
        .flatMap(
            tracking ->
                updateTracking(
                    tracking,
                    "INITIATED",
                    tracking.getAttemptCount() == null ? 0 : tracking.getAttemptCount(),
                    null,
                    tracking.getS3FilePath()));
  }

  private Mono<LoanApplicationRestructureDetailsEntity> fetchS3PathWithRetry(
      LoanRestructureNotificationTrackingEntity tracking, int attempt) {
    if (tracking.getRestructureDetailsId() == null) {
      return Mono.empty();
    }
    Long restructureDetailsId = Objects.requireNonNull(tracking.getRestructureDetailsId());
    return restructureDetailsRepository
        .findById(restructureDetailsId)
        .filter(
            source ->
                source.getSignedUrl() != null
                    && !source.getSignedUrl().isBlank()
                    && normalizeMobileNumber(source.getMobileNumber()) != null)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  if (attempt >= restructureSmsMaxRetries) {
                    return Mono.empty();
                  }
                  return updateTracking(
                          tracking,
                          "RETRYING",
                          attempt,
                          "signed_url or mobile not available yet. Retrying",
                          tracking.getS3FilePath())
                      .then(Mono.delay(Duration.ofMillis(restructureSmsRetryDelayMs)))
                      .then(fetchS3PathWithRetry(tracking, attempt + 1));
                }));
  }

  private Mono<Void> sendRestructureApprovalSms(
      LoanRestructureNotificationTrackingEntity tracking,
      LoanApplicationRestructureDetailsEntity sourceEntity,
      String effectiveDate) {
    return s3Service
        .fetchPreSignedUrl(sourceEntity.getSignedUrl(), restructureDocUploadBucketName)
        .flatMap(
            signedUrl -> {
              NotificationRecipientRequest recipient =
                  NotificationRecipientRequest.builder()
                      .sms(normalizeMobileNumber(sourceEntity.getMobileNumber()))
                      .referenceType(LOAN_ID)
                      .referenceId(String.valueOf(sourceEntity.getLan()))
                      .params(
                          List.of(
                              nonNullOrDefault(sourceEntity.getCustomerName(), "Customer"),
                              nonNullOrDefault(effectiveDate, ""),
                              URLEncoder.encode(signedUrl, StandardCharsets.UTF_8)))
                      .build();

              NotificationRequest notificationRequest =
                  NotificationRequest.builder()
                      .templateId(restructureApprovalTemplateId)
                      .recipients(List.of(recipient))
                      .build();
              return kafkaNotificationProducerService
                  .sendNotification(notificationRequest)
                  .flatMap(
                      ignored ->
                          updateTracking(
                              tracking,
                              "SUCCESS",
                              tracking.getAttemptCount(),
                              null,
                              sourceEntity.getSignedUrl()))
                  .doOnSuccess(
                      ignored ->
                          log.info(
                              "[RESTRUCTURE_SMS] sent for restructureDetailsId: {}, lan: {},"
                                  + " mobile: {}",
                              tracking.getRestructureDetailsId(),
                              tracking.getLoanAccountNumber(),
                              recipient.getSms()))
                  .then();
            })
        .onErrorResume(
            error ->
                updateTracking(
                        tracking,
                        "FAILED",
                        tracking.getAttemptCount(),
                        error.getMessage(),
                        tracking.getS3FilePath())
                    .then(
                        Mono.fromRunnable(
                            () ->
                                log.error(
                                    "[RESTRUCTURE_SMS] failed while preparing/sending sms for"
                                        + " restructureDetailsId: {}, lan: {}, error: {}",
                                    tracking.getRestructureDetailsId(),
                                    tracking.getLoanAccountNumber(),
                                    error.getMessage(),
                                    error)))
                    .then());
  }

  private Mono<Void> markMaxRetryExceeded(LoanRestructureNotificationTrackingEntity tracking) {
    return updateTracking(
            tracking,
            "MAX_RETRY_EXCEEDED",
            restructureSmsMaxRetries,
            "S3 file path unavailable after all retries",
            tracking.getS3FilePath())
        .doOnSuccess(
            ignored ->
                log.info(
                    "[RESTRUCTURE_SMS] max retries exceeded for restructureDetailsId: {}, lan: {}",
                    tracking.getRestructureDetailsId(),
                    tracking.getLoanAccountNumber()))
        .then();
  }

  private Mono<Void> markFailure(Long restructureDetailsId, Throwable error) {
    return loanRestructureNotificationTrackingRepository
        .findTopByRestructureDetailsIdOrderByIdDesc(restructureDetailsId)
        .flatMap(
            tracking ->
                updateTracking(
                    tracking,
                    "FAILED",
                    tracking.getAttemptCount(),
                    error.getMessage(),
                    tracking.getS3FilePath()))
        .then();
  }

  private Mono<LoanRestructureNotificationTrackingEntity> updateTracking(
      LoanRestructureNotificationTrackingEntity tracking,
      String status,
      Integer attemptCount,
      String lastError,
      String s3FilePath) {
    tracking.setStatus(status);
    tracking.setAttemptCount(attemptCount);
    tracking.setLastError(lastError);
    tracking.setS3FilePath(s3FilePath);
    tracking.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
    if (tracking.getCreatedAt() == null) {
      tracking.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
    }
    return loanRestructureNotificationTrackingRepository.save(tracking);
  }

  private String roundToTwoDecimalString(double amount) {
    return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
  }

  private String nonNullOrDefault(String value, String defaultValue) {
    return value == null ? defaultValue : value;
  }

  private String maskBankAccountNumber(String accountNumber) {
    if (accountNumber == null || accountNumber.length() <= 5) {
      return "XXXXX";
    }
    return "XXXXX" + accountNumber.substring(5);
  }
}
