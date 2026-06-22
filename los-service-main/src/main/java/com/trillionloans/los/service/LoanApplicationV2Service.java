package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.util.DateTimeConverterUtil.convertEpochMilliToIst;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.constant.ResponseStatus;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.model.dto.LivelinessScoreDataTableDTO;
import com.trillionloans.los.model.request.AadhaarXmlRequest;
import com.trillionloans.los.model.request.AadharXmlRequestV2;
import com.trillionloans.los.model.request.ConsentRequest;
import com.trillionloans.los.model.request.MultiConsentRequest;
import com.trillionloans.los.model.request.SelfieUpload;
import com.trillionloans.los.model.request.m2p.M2pConsentRequest;
import com.trillionloans.los.model.response.MessageResponseDTO;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pSelfieUploadResponseDTO;
import com.trillionloans.los.model.response.okyc.M2pAadhaarXmlResponseDTO;
import com.trillionloans.los.service.ckyc.AadhaarXmlService;
import com.trillionloans.los.service.validationservice.DOBWaterfallValidationService;
import com.trillionloans.los.service.validationservice.ValidationFunnelService;
import com.trillionloans.los.util.FileValidatorUtil;
import com.trillionloans.los.util.LivelinessScoreUtil;
import io.micrometer.common.util.StringUtils;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@AllArgsConstructor
@Slf4j
public class LoanApplicationV2Service {

  private final M2PWrapperApi m2PWrapperApi;
  private final M2pFacadeService m2pFacadeService;
  private final Gson gson;
  private final AadhaarXmlService aadhaarXmlService;
  private final DOBWaterfallValidationService dobWaterfallValidationService;
  private final LoanClientLookupService loanClientLookupService;
  private final LoanLevelClientDetailsService loanLevelClientDetailsService;

  private final ValidationFunnelService validationFunnelService;
  private static final String REQUEST_ACCEPTED = "request accepted";

  public Mono<M2pAadhaarXmlResponseDTO> uploadAadhaarXml(
      AadharXmlRequestV2 aadhaarXmlRequest, String loanApplicationId, String productCode) {

    return loanClientLookupService
        .getClientIdForLoan(loanApplicationId, Event.AADHAR_XML_UPLOAD.getDefaultEventType())
        .onErrorResume(
            error -> {
              log.error(
                  "[AADHAAR_XML_UPLOAD][ERROR] Error while finding clientId from loanApplicationId:"
                      + " {}",
                  loanApplicationId,
                  error);
              return Mono.error(error);
            })
        .flatMap(
            clientId ->
                aadhaarXmlService
                    .uploadAadhaarXml(
                        new AadhaarXmlRequest(aadhaarXmlRequest.getRequestString()),
                        String.valueOf(clientId),
                        aadhaarXmlRequest.getAadhaarXMLType(),
                        loanApplicationId,
                        productCode)
                    .doOnSuccess(
                        response -> {
                          validationFunnelService.runDOBWaterfallValidationFunnelAtAadharXMLUpload(
                              aadhaarXmlRequest.getRequestString(),
                              clientId.toString(),
                              productCode,
                              loanApplicationId);

                          loanLevelClientDetailsService
                              .updateAadhaarDetailsInDb(
                                  loanApplicationId, aadhaarXmlRequest.getRequestString())
                              .subscribeOn(Schedulers.boundedElastic())
                              .subscribe();
                        }));
  }

  public Mono<?> uploadSelfieAgainstLoan(SelfieUpload selfieUploadData, String loanApplicationId) {
    try {
      FileValidatorUtil.isValidBase64(
          selfieUploadData.getFileContent(), selfieUploadData.getFileType());
    } catch (BaseException e) {
      return Mono.error(e);
    }
    return loanClientLookupService
        .getClientIdForLoan(loanApplicationId, Event.SELFIE_UPLOAD.getDefaultEventType())
        .onErrorResume(
            error -> {
              log.error(
                  "[SELFIE_UPLOAD] [ERROR] Error while finding clientId from loanApplicationId: {}",
                  loanApplicationId);
              return Mono.error(error);
            })
        .flatMap(
            clientId ->
                m2PWrapperApi
                    .uploadSelfieAgainstLead(
                        selfieUploadData, String.valueOf(clientId), loanApplicationId)
                    .flatMap(
                        response ->
                            Mono.deferContextual(
                                parentContext -> {
                                  M2pSelfieUploadResponseDTO m2pSelfieUploadResponseDTO =
                                      (M2pSelfieUploadResponseDTO) response;
                                  uploadScoreAgainstClient(
                                          selfieUploadData,
                                          String.valueOf(clientId),
                                          String.valueOf(m2pSelfieUploadResponseDTO.imageId()),
                                          loanApplicationId)
                                      .contextWrite(ctx -> ctx.putAll(parentContext))
                                      .publishOn(Schedulers.parallel())
                                      .subscribe();

                                  return Mono.just(response);
                                })));
  }

  private Mono<?> uploadScoreAgainstClient(
      SelfieUpload selfieUploadData, String leadId, String imageId, String loanApplicationId) {

    return Mono.defer(
        () -> {
          LivelinessScoreDataTableDTO livelinessScoreDataTableDTO =
              LivelinessScoreDataTableDTO.builder()
                  .score(selfieUploadData.getScore())
                  .loanApplicationId(loanApplicationId)
                  .lead(leadId)
                  .timestamp(convertEpochMilliToIst(System.currentTimeMillis()))
                  .imageId(imageId)
                  .ip(selfieUploadData.getIp())
                  .build();
          boolean isValid =
              LivelinessScoreUtil.validateLivelinessScoreParameters(livelinessScoreDataTableDTO);
          if (!isValid) {
            return Mono.empty();
          }
          return m2PWrapperApi.uploadScoreAgainstLead(livelinessScoreDataTableDTO, leadId);
        });
  }

  public Mono<?> createConsents(
      MultiConsentRequest multiConsentRequest, String leadId, String loanId) {
    return Mono.deferContextual(
        contextView -> {
          String traceId =
              contextView.getOrDefault(TRACE_ID, UUID.randomUUID().toString().substring(0, 8));
          ResponseDTO<MessageResponseDTO> responseDTO =
              ResponseDTO.<MessageResponseDTO>builder()
                  .status(ResponseStatus.SUCCESS)
                  .message(REQUEST_ACCEPTED)
                  .data(
                      MessageResponseDTO.builder()
                          .developerMessage(REQUEST_ACCEPTED)
                          .defaultUserMessage(REQUEST_ACCEPTED)
                          .build())
                  .traceId(traceId)
                  .build();

          Flux.fromIterable(multiConsentRequest.getConsents())
              .flatMap(consentRequest -> createConsent(consentRequest, leadId, loanId))
              .doOnNext(
                  result ->
                      log.info(
                          "[CREATE_CONSENT_V2] consent processed for client: {}, loan application"
                              + " id: {}, result: {}",
                          leadId,
                          loanId,
                          result))
              .doOnError(
                  error ->
                      log.error(
                          "[CREATE_CONSENT_V2] unexpected error during consent processing for"
                              + " client: {}, loan application id: {}",
                          leadId,
                          loanId,
                          error))
              .contextWrite(context -> context.put(TRACE_ID, traceId))
              .subscribeOn(Schedulers.boundedElastic())
              .subscribe();
          return Mono.just(responseDTO);
        });
  }

  private Mono<String> createConsent(ConsentRequest consentRequest, String leadId, String loanId) {
    return Mono.deferContextual(
        ctx ->
            buildM2PConsentRequest(consentRequest)
                .flatMap(
                    request ->
                        m2PWrapperApi
                            .createConsent(request, leadId, loanId)
                            .flatMap(response -> Mono.just(gson.toJson(response))))
                .onErrorResume(
                    error -> {
                      log.error(
                          "[CREATE_CONSENT_V2] error while uploading consent to m2p for client id:"
                              + " {}, loan application id: {}, error: {}",
                          leadId,
                          loanId,
                          error.getMessage());
                      return Mono.just(error.getMessage());
                    }));
  }

  private Mono<M2pConsentRequest> buildM2PConsentRequest(ConsentRequest consentRequest) {
    return Mono.defer(
        () -> {
          if (ObjectUtils.isEmpty(consentRequest)) {
            return Mono.error(
                new ClientSideException(
                    "consentRequest cannot be null", null, HttpStatus.BAD_REQUEST));
          }
          try {
            String additionalDetails = consentRequest.getAdditionalDetails();
            JsonObject additionalDetailsObj =
                StringUtils.isNotBlank(additionalDetails)
                    ? JsonParser.parseString(additionalDetails).getAsJsonObject()
                    : new JsonObject();

            String dateTime = consentRequest.getDateTime();
            if (StringUtils.isNotBlank(dateTime)) {
              additionalDetailsObj.addProperty("approvedDateTime", dateTime);
            }

            M2pConsentRequest result =
                M2pConsentRequest.builder()
                    .consentKey(consentRequest.getConsentKey())
                    .ipAddress(consentRequest.getIpAddress())
                    .isAccepted(Boolean.TRUE.equals(consentRequest.getIsAccepted()))
                    .additionalDetails(gson.toJson(additionalDetailsObj))
                    .build();

            return Mono.just(result);
          } catch (Exception e) {
            return Mono.error(
                new ClientSideException(
                    "failed to parse additionalDetails json in the request body",
                    null,
                    HttpStatus.BAD_REQUEST));
          }
        });
  }

  public Mono<?> getLoanDisbursementStatus(String loanId) {
    return m2PWrapperApi.getLoanDisbursementStatusV2(loanId);
  }
}
