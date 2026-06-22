package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.LEAD_ACKNOWLEDGEMENT;
import static com.trillionloans.los.constant.StringConstants.LOAN_APPLICATION_NO;

import com.trillionloans.los.api.partner.NotificationServiceApi;
import com.trillionloans.los.model.entity.LeadAcknowledgement;
import com.trillionloans.los.model.request.NotificationRecipientRequest;
import com.trillionloans.los.model.request.NotificationRequest;
import com.trillionloans.los.repository.LeadAcknowledgementRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadAcknowledgementNotificationService {

  private final NotificationServiceApi notificationServiceApi;
  private final LeadAcknowledgementRepository leadAcknowledgementRepository;

  @Value("${notification-service.templates.lead-acknowledgement-template}")
  String leadAcknowledgementTemplateId;

  public Mono<Boolean> isAcknowledgementAlreadySent(String loanId) {
    return leadAcknowledgementRepository
        .findByLoanId(loanId)
        .map(existing -> "SENT".equals(existing.getAcknowledgementStatus()))
        .defaultIfEmpty(false);
  }

  public Mono<Void> sendLeadAcknowledgementSms(String loanId, String loanRefNo, String mobile) {
    NotificationRequest notificationRequest =
        NotificationRequest.builder()
            .templateId(leadAcknowledgementTemplateId)
            .recipients(
                List.of(
                    NotificationRecipientRequest.builder()
                        .referenceId(loanId)
                        .referenceType(LOAN_APPLICATION_NO)
                        .sms(mobile)
                        .params(List.of(loanRefNo))
                        .build()))
            .build();

    return notificationServiceApi
        .triggerNotification(notificationRequest, loanId)
        .flatMap(response -> handleNotificationResponse(response, loanId, loanRefNo, mobile))
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] SMS failed: LoanAppId={}, mobile={}, Error={}",
                  LEAD_ACKNOWLEDGEMENT,
                  loanId,
                  mobile,
                  error.getMessage());
              return saveAcknowledgement(loanId, mobile, error).then();
            });
  }

  private Mono<Void> handleNotificationResponse(
      Object response, String loanId, String loanRefNo, String mobile) {
    if (response instanceof Map) {
      Map<String, Object> responseMap = (Map<String, Object>) response;
      String status = (String) responseMap.getOrDefault("status", "FAILED");

      if ("SUCCESS".equalsIgnoreCase(status)) {
        log.info(
            "[{}] Lead acknowledgment SMS sent successfully: LoanAppId={}, mobile={}",
            LEAD_ACKNOWLEDGEMENT,
            loanRefNo,
            mobile);
        return saveAcknowledgement(loanId, mobile, null).then();
      } else {
        log.warn(
            "[{}] Failed to trigger lead acknowledgment SMS: LoanAppId={}, mobile={}, Response={}",
            LEAD_ACKNOWLEDGEMENT,
            loanRefNo,
            mobile,
            responseMap);
        return saveAcknowledgement(loanId, mobile, new Exception("SMS failed")).then();
      }
    } else {
      log.warn(
          "[{}] Unexpected response format from notification service: {}",
          LEAD_ACKNOWLEDGEMENT,
          response);
      return saveAcknowledgement(loanId, mobile, new Exception("Invalid response format")).then();
    }
  }

  public Mono<LeadAcknowledgement> saveAcknowledgement(
      String loanId, String mobileNumber, Throwable error) {
    return leadAcknowledgementRepository
        .findByLoanId(loanId)
        .flatMap(
            existing -> {
              existing.setAcknowledgementStatus(error == null ? "SENT" : "FAILED");
              existing.setErrorMessage(error == null ? null : error.getMessage());
              existing.setAcknowledgementTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

              log.info(
                  "[LeadAcknowledgement] Updating existing acknowledgment: LoanAppId={}, mobile={},"
                      + " Status={}",
                  loanId,
                  mobileNumber,
                  existing.getAcknowledgementStatus());

              return leadAcknowledgementRepository.save(existing);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  LeadAcknowledgement newAcknowledgement =
                      LeadAcknowledgement.builder()
                          .loanId(loanId)
                          .acknowledgementStatus(error == null ? "SENT" : "FAILED")
                          .errorMessage(error == null ? null : error.getMessage())
                          .acknowledgementTime(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
                          .build();

                  log.info(
                      "[{}] Saving new acknowledgment: LoanAppId={}, mobile={}, Status={}",
                      LEAD_ACKNOWLEDGEMENT,
                      loanId,
                      mobileNumber,
                      newAcknowledgement.getAcknowledgementStatus());

                  return leadAcknowledgementRepository.save(newAcknowledgement);
                }))
        .doOnSuccess(
            saved ->
                log.info(
                    "[{}] Acknowledgment saved/updated for LoanAppId={}",
                    LEAD_ACKNOWLEDGEMENT,
                    loanId))
        .doOnError(
            err ->
                log.error(
                    "[{}] Failed to save/update acknowledgment: LoanAppId={}, Error={}",
                    LEAD_ACKNOWLEDGEMENT,
                    loanId,
                    err.getMessage()));
  }
}
