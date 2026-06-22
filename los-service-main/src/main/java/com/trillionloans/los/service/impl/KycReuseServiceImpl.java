package com.trillionloans.los.service.impl;

import static com.trillionloans.los.constant.StringConstants.NO_AADHAAR_XML_FOUND;
import static com.trillionloans.los.util.LeadDataUtil.parseXml;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.AadhaarXMLType;
import com.trillionloans.los.constant.AddressType;
import com.trillionloans.los.constant.ResponseStatus;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.model.dto.AadhaarXmlDetailsDTO;
import com.trillionloans.los.model.dto.KycClientDetails;
import com.trillionloans.los.model.request.ConsentRequest;
import com.trillionloans.los.model.request.m2p.M2pCkycrCallbackRequest;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.ReuseAddressDetailsDTO;
import com.trillionloans.los.service.KycReuseService;
import com.trillionloans.los.service.LoanApplicationService;
import com.trillionloans.los.service.M2pFacadeService;
import com.trillionloans.los.service.ckyc.AadhaarXmlServiceImpl;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@AllArgsConstructor
@Service
@Slf4j
public class KycReuseServiceImpl implements KycReuseService {
  private AadhaarXmlServiceImpl aadhaarXmlServiceImpl;
  private M2PWrapperApi m2PWrapperApi;
  private M2pFacadeService m2pFacadeService;
  private LoanApplicationService loanApplicationService;

  public Mono<KycClientDetails> getKycClientDetailsFromAaadhar(String clientId) {
    return Mono.justOrEmpty(clientId)
        .filter(id -> !id.isBlank())
        .switchIfEmpty(
            Mono.error(
                new BaseException("Client ID is null or blank", null, HttpStatus.BAD_REQUEST)))
        .flatMap(
            xmlData -> {
              return getKycXmlData(clientId);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[GET_KYC-DATA-FROM-AADHAAR][CLIENT DATA][ERROR] error for client: {}, error: {}",
                  clientId,
                  error.getMessage(),
                  error);

              return Mono.error(error);
            });
  }

  @Override
  public Mono<ResponseDTO<Object>> registerKycReuseConsent(
      String productCode, String clientId, String loanId, ConsentRequest consentRequest) {

    return loanApplicationService
        .createConsent(consentRequest, clientId, loanId)
        .doOnSuccess(
            consent ->
                log.info(
                    "[KYC_REUSE_CONFIRM][SUCCESS] consent registered for loan:{}, client id:{}",
                    loanId,
                    clientId))
        .flatMap(
            consentResponse -> {

              // UPDATING LEADS
              updateLeadFromAadhaarXml(clientId, loanId, productCode)
                  .doOnSuccess(
                      r ->
                          log.info(
                              "[KYC_REUSE_CONFIRM][ASYNC_UPDATE_LEAD][SUCCESS] completed for"
                                  + " loan:{}, client:{}",
                              loanId,
                              clientId))
                  .doOnError(
                      e ->
                          log.error(
                              "[KYC_REUSE_CONFIRM][ASYNC_UPDATE_LEAD][ERROR] ClientId: {}, LoanId:"
                                  + " {}, Error: {}",
                              clientId,
                              loanId,
                              e.getMessage(),
                              e))
                  .subscribe();

              // HITTING CTA
              M2pCkycrCallbackRequest kycCtaBody =
                  M2pCkycrCallbackRequest.builder()
                      .productCode(productCode)
                      .loanId(Integer.valueOf(loanId))
                      .build();
              log.info("[KYC_REUSE_CONFIRM][CTA HIT]");
              return m2pFacadeService
                  .registerCkycrStatus(kycCtaBody)
                  .thenReturn(
                      ResponseDTO.builder()
                          .status(ResponseStatus.SUCCESS)
                          .message("Consent registered and CTA triggered")
                          .build());
            })
        .onErrorResume(
            e -> {
              log.error(
                  "[KYC_REUSE_CONFIRM][REGISTER_CONSENT][ERROR] ClientId: {}, LoanId: {}, Error:"
                      + " {}",
                  clientId,
                  loanId,
                  e.getMessage(),
                  e);
              return Mono.error(
                  new BaseException(
                      "[KYC_REUSE_CONFIRM] failed to create consent",
                      "",
                      HttpStatus.INTERNAL_SERVER_ERROR));
            });
  }

  private Mono<AadhaarXmlDetailsDTO> getLatestAadhaarXml(String clientId) {
    return m2PWrapperApi
        .getLatestAadhaarXmlDocId(clientId)
        .switchIfEmpty(Mono.error(new NotFoundException(NO_AADHAAR_XML_FOUND)))
        .flatMap(
            documentList -> {
              log.info(
                  "[GET LATEST AADHAAR XML][SUCCESS] client latest aadhar xml doc id: {}, against"
                      + " client: {}",
                  documentList.getId(),
                  clientId);
              return m2PWrapperApi
                  .getLatestAadhaarXml(clientId, documentList.getId())
                  .switchIfEmpty(Mono.error(new NotFoundException(NO_AADHAAR_XML_FOUND)))
                  .flatMap(
                      rawData -> {
                        return parseXml(rawData);
                      });
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[GET LATEST AADHAAR XML][ERROR] Failed fetching aadhaar xml for client: {}"
                      + " exception: {}",
                  clientId,
                  error);
              return Mono.error(new NotFoundException(NO_AADHAAR_XML_FOUND));
            });
  }

  private List<ReuseAddressDetailsDTO> buildAddressDetails(
      AadhaarXmlDetailsDTO dto, String addressType) {
    log.info("[BUILDING_ADDRESS][SUCCESS] from:, {}", dto);
    String addressLineOne =
        Stream.of(dto.getHouse(), dto.getStreet(), dto.getLandmark())
            .filter(s -> Objects.nonNull(s) && !s.isBlank())
            .collect(Collectors.joining(","));

    String addressLineTwo =
        Stream.of(
                dto.getLocality(),
                dto.getVtc(),
                dto.getSubdistrict(),
                dto.getDistrict(),
                dto.getState())
            .filter(s -> Objects.nonNull(s) && !s.isBlank())
            .collect(Collectors.joining(","));
    if (addressLineOne.length() > 200) {
      int splitIndex = addressLineOne.lastIndexOf(',', 200);
      if (splitIndex == -1) splitIndex = 200;
      addressLineTwo = addressLineOne.substring(splitIndex + 1).trim();
      addressLineOne = addressLineOne.substring(0, splitIndex).trim();
    }

    return List.of(
        ReuseAddressDetailsDTO.builder()
            .addressType(addressType)
            .addressLine1(addressLineOne)
            .addressLine2(addressLineTwo)
            .pincode(dto.getPincode())
            .city(dto.getDistrict())
            .build());
  }

  private Mono<KycClientDetails> getKycXmlData(String clientId) {
    log.info("[GETTING_XML_BUILD_DATA][START] building client data for client id: {}", clientId);
    return getLatestAadhaarXml(clientId)
        .flatMap(
            clientXmlDetailsDTO -> {
              List<ReuseAddressDetailsDTO> reuseAddressDetailsDTO =
                  buildAddressDetails(clientXmlDetailsDTO, AddressType.PERMANENT.getDisplayName());
              return Mono.just(
                  KycClientDetails.builder()
                      .addressInfo(reuseAddressDetailsDTO)
                      .name(clientXmlDetailsDTO.getName())
                      .dob(clientXmlDetailsDTO.getDob())
                      .dependent(clientXmlDetailsDTO.getDependent())
                      .build());
            });
  }

  private Mono<?> updateLeadFromAadhaarXml(String clientId, String loanId, String productCode) {
    return Mono.defer(
            () ->
                aadhaarXmlServiceImpl
                    .getLatestAadhaarXml(clientId)
                    .flatMap(
                        clientXmlDetailsDTO -> {
                          log.info(
                              "[KYC_REUSE_CONFIRM][SUCCESS] got latest aadhaar xml for loan:{},"
                                  + " client id:{}",
                              loanId,
                              clientId);

                          log.info(
                              "[KYC_REUSE_CONFIRM][UPDATE] client address and family details"
                                  + " against loanId:{}",
                              loanId);
                          return aadhaarXmlServiceImpl
                              .processClientXmlDetails(
                                  clientXmlDetailsDTO,
                                  clientId,
                                  AddressType.PERMANENT.getDisplayName())
                              .then(
                                  aadhaarXmlServiceImpl.kycValidation(
                                      clientXmlDetailsDTO,
                                      clientId,
                                      loanId,
                                      productCode,
                                      clientXmlDetailsDTO.getTs() == null
                                              || clientXmlDetailsDTO.getTs().isEmpty()
                                          ? AadhaarXMLType.OKYC
                                          : AadhaarXMLType.DIGI_LOCKER));
                        }))
        .subscribeOn(Schedulers.boundedElastic());
  }
}
