package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.LEAD_ACKNOWLEDGEMENT;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.entity.LoanLevelClientDetail;
import com.trillionloans.los.model.request.LeadAcknowledgementRequest;
import com.trillionloans.los.model.response.LeadAcknowledgementResponse;
import com.trillionloans.los.model.response.m2p.M2pLoanAppIDContactDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadAcknowledgementService {

  private final M2PWrapperApi m2PWrapperApi;
  private final LeadAcknowledgementNotificationService notificationService;
  private final LoanLevelClientDetailsService loanLevelClientDetailsService;

  public Mono<LeadAcknowledgementResponse> acknowledgementLead(
      String loanId, LeadAcknowledgementRequest leadAcknowledgementRequest) {
    log.info(
        "[{}] Processing lead acknowledgment for LoanId={} for process {}",
        LEAD_ACKNOWLEDGEMENT,
        loanId,
        leadAcknowledgementRequest.getProcessName());
    LeadAcknowledgementResponse leadAcknowledgementResponse =
        LeadAcknowledgementResponse.builder()
            .message("Lead acknowledgment processed successfully")
            .build();

    return processAndVerifyAcknowledgement(loanId).thenReturn(leadAcknowledgementResponse);
  }

  public Mono<Void> processAndVerifyAcknowledgement(String loanId) {

    return notificationService
        .isAcknowledgementAlreadySent(loanId)
        .flatMap(
            isSent -> {
              if (isSent) {
                log.info(
                    "[{}] Lead acknowledgment already sent for LoanAppId={}, proceeding",
                    LEAD_ACKNOWLEDGEMENT,
                    loanId);
                return Mono.empty();
              }
              return processLeadAcknowledgement(loanId);
            });
  }

  private Mono<Void> processLeadAcknowledgement(String loanId) {
    return loanLevelClientDetailsService
        .fetchLoanLevelClientDetailsFromDb(loanId)
        .map(this::toLeadContactDetails)
        .flatMap(Mono::justOrEmpty)
        .switchIfEmpty(Mono.defer(() -> m2PWrapperApi.getContactDetailByloanAppId(loanId)))
        .flatMap(
            contactDetails -> {
              String mobileNo = contactDetails.getMobileNo();
              if (StringUtils.isBlank(mobileNo)) {
                log.info(
                    "[{}] No valid mobile number for LoanAppId={}, skipping SMS",
                    LEAD_ACKNOWLEDGEMENT,
                    loanId);
                return Mono.empty();
              }

              String formattedMobile = formatMobileNumber(mobileNo);
              log.info(
                  "[{}] Sending lead acknowledgment SMS to mobile={} for LoanAppId={}",
                  LEAD_ACKNOWLEDGEMENT,
                  formattedMobile,
                  loanId);

              return notificationService
                  .sendLeadAcknowledgementSms(
                      loanId, contactDetails.getLoanApplicationReferenceNo(), formattedMobile)
                  .then();
            });
  }

  private M2pLoanAppIDContactDTO toLeadContactDetails(LoanLevelClientDetail loanLevelClientDetail) {
    if (loanLevelClientDetail == null) {
      return null;
    }

    String mobileNo = loanLevelClientDetail.getMobileNo();
    String loanApplicationReferenceNo = loanLevelClientDetail.getLoanApplicationReferenceNo();
    if (StringUtils.isBlank(mobileNo) || StringUtils.isBlank(loanApplicationReferenceNo)) {
      return null;
    }

    M2pLoanAppIDContactDTO contactDTO = new M2pLoanAppIDContactDTO();
    contactDTO.setMobileNo(mobileNo);
    contactDTO.setLoanApplicationReferenceNo(loanApplicationReferenceNo);
    return contactDTO;
  }

  private String formatMobileNumber(String mobileNumber) {
    if (StringUtils.isBlank(mobileNumber)) {
      return null;
    }
    return mobileNumber.startsWith("91") && mobileNumber.length() == 12
        ? mobileNumber
        : "91" + mobileNumber;
  }
}
