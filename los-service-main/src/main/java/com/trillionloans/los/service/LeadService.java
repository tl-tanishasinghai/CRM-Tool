package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.DATE_FORMAT;
import static com.trillionloans.los.constant.StringConstants.EN;
import static com.trillionloans.los.constant.StringConstants.M2P_PRODUCT_ID_NOT_FOUND;
import static com.trillionloans.los.constant.StringConstants.NOT_FOUND;
import static com.trillionloans.los.constant.StringConstants.OFFICE_MAPPING_NOT_FOUND;
import static com.trillionloans.los.constant.StringConstants.PRODUCT_MAPPING_NOT_FOUND;
import static com.trillionloans.los.util.DateTimeConverterUtil.convertEpochMilliToIst;
import static com.trillionloans.los.util.LivelinessScoreUtil.validateLivelinessScoreParameters;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.AddressType;
import com.trillionloans.los.constant.RepaymentFrequency;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.AddressDetailsDTO;
import com.trillionloans.los.model.dto.ClientDetailsCpRpsResponseDto;
import com.trillionloans.los.model.dto.LeadIdResponse;
import com.trillionloans.los.model.dto.LivelinessScoreDataTableDTO;
import com.trillionloans.los.model.dto.internal.LoanLevelClientDetailsCacheDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadUpdateDTO;
import com.trillionloans.los.model.partner.m2p.M2pUcicUpdateDTO;
import com.trillionloans.los.model.request.DedupeLeadRequest;
import com.trillionloans.los.model.request.Lead;
import com.trillionloans.los.model.request.LeadBulkDocumentsUploadRequest;
import com.trillionloans.los.model.request.LeadUpdate;
import com.trillionloans.los.model.request.PanDetails;
import com.trillionloans.los.model.request.RepaymentScheduleRequest;
import com.trillionloans.los.model.request.SelfieUpload;
import com.trillionloans.los.model.request.m2p.M2pBankDetailsRequestDTO;
import com.trillionloans.los.model.response.ClientDetailsCpResponseDto;
import com.trillionloans.los.model.response.ClientDetailsResponseDto;
import com.trillionloans.los.model.response.m2p.M2pBankDetailsResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pClientCreationResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pClientIdsOfNullUcic;
import com.trillionloans.los.model.response.m2p.M2pMaxDpdResponseDto;
import com.trillionloans.los.model.response.m2p.M2pNpaLoanApplicationsDto;
import com.trillionloans.los.model.response.m2p.M2pPanAadhaarDetailsDTO;
import com.trillionloans.los.model.response.m2p.M2pSelfieUploadResponseDTO;
import com.trillionloans.los.service.db.LeadMiscellaneousDetailsService;
import com.trillionloans.los.service.db.PartnerMasterService;
import com.trillionloans.los.service.validationservice.NsdlPanValidationService;
import com.trillionloans.los.util.FileValidatorUtil;
import com.trillionloans.los.util.LeadDataUtil;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@AllArgsConstructor
public class LeadService {
  private final M2PWrapperApi m2PWrapperApi;
  private final PartnerMasterService partnerMasterService;
  private final KycService kycService;
  private final ClientCacheService clientCacheService;
  private final NsdlPanValidationService NSDLPanValidationService;
  private final LoanClientLookupService loanClientLookupService;
  private final LeadMiscellaneousDetailsService leadMiscellaneousDetailsService;
  private final LoanLevelClientDetailsService loanLevelClientDetailsService;

  public Mono<ClientDetailsResponseDto> getLeadData(String leadId) {
    return m2PWrapperApi.getLeadData(leadId);
  }

  public Mono<LoanLevelClientDetailsCacheDTO> getLoanLevelClientDetails(
      String clientId, String loanApplicationId, String productCode) {
    return loanLevelClientDetailsService.fetchLoanLevelClientDetails(
        clientId, loanApplicationId, productCode);
  }

  public Mono<Lead> getLeadFullData(String leadId) {
    return m2PWrapperApi.getLeadFullData(leadId);
  }

  public Mono<ClientDetailsCpResponseDto> getCpLeadData(String leadId) {
    return m2PWrapperApi.getCpLeadData(leadId);
  }

  public Mono<?> getLeadData(Map<String, String> queryParams) {
    Map<String, String> queryParamMappings = getQueryParamMappings();
    Map<String, String> requestParams = new HashMap<>();

    for (Map.Entry<String, String> queryParam : queryParams.entrySet()) {
      if (queryParamMappings.containsKey(queryParam.getKey())) {
        requestParams.put(queryParamMappings.get(queryParam.getKey()), queryParam.getValue());
        break; // only taking single request param as we don't want to return multiple clients
      }
    }

    if (requestParams.isEmpty()) {
      return Mono.error(
          new BaseException(
              "Missing required parameter: clientId or panDocumentKey",
              null,
              HttpStatus.BAD_REQUEST));
    }

    return m2PWrapperApi.getLeadData(requestParams);
  }

  private Map<String, String> getQueryParamMappings() {
    Map<String, String> mappings = new HashMap<>();
    mappings.put("clientId", "R_clientId");
    mappings.put("panDocumentKey", "R_documentKey");
    return mappings;
  }

  public Mono<ClientDetailsCpRpsResponseDto> getCpRpsLeadData(String leadId, String accountNo) {
    return m2PWrapperApi
        .getCpRpsLeadData(leadId, accountNo)
        .map(
            dto -> {
              dto.setRepaymentPeriodFrequency(
                  RepaymentFrequency.fromCode(dto.getRepaymentPeriodFrequencyEnum()));
              return dto;
            });
  }

  public Mono<M2pClientCreationResponseDTO> createLead(Lead leadData, String productCode) {
    return partnerMasterService
        .findByProductCode(productCode)
        .flatMap(
            partnerMasterEntity -> {
              String officeName = partnerMasterEntity.getOfficeName();
              M2pLeadRequestDTO leadRequest = getM2PLeadRequest(leadData, officeName);
              return m2PWrapperApi
                  .createLead(leadRequest)
                  .flatMap(
                      m2pClientCreationResponseDTO -> {
                        persistClientDetailsInDbAsync(
                            leadData, productCode, m2pClientCreationResponseDTO.getClientId());
                        return Mono.just(m2pClientCreationResponseDTO);
                      });
            });
  }

  private void persistClientDetailsInDbAsync(Lead leadData, String productCode, Integer clientId) {
    Mono.deferContextual(
            contextView ->
                loanLevelClientDetailsService
                    .persistClientCreationRequest(leadData, productCode, clientId.toString())
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnError(
                        e ->
                            log.error(
                                "[ERROR][LOAN_LEVEL_CLIENT_DATA][CLIENT_CREATION] failed to save"
                                    + " request data in database for clientId: {}",
                                clientId,
                                e))
                    .doOnSuccess(
                        s ->
                            log.info(
                                "[LOAN_LEVEL_CLIENT_DATA][CLIENT_CREATION] request data saved"
                                    + " successfully for clientId: {}",
                                clientId))
                    .contextWrite(contextView))
        .subscribe();
  }

  public Mono<M2pClientCreationResponseDTO> initiateCreateLead(Lead leadData, String productCode) {

    return NSDLPanValidationService.getNsdlPanValidationConfig(productCode)
        .flatMap(
            config -> {
              log.info("PAN validation enabled? {}", config.isPanValidationFeatureFlag());

              Mono<M2pClientCreationResponseDTO> leadCreationMono;
              if (Boolean.TRUE.equals(config.isPanValidationFeatureFlag())) {
                leadCreationMono = createLeadAndCacheDetails(leadData, productCode);
              } else {
                leadCreationMono = createLead(leadData, productCode);
              }

              return leadCreationMono.doOnSuccess(
                  response -> saveMiscellaneousDetailsAsync(response.getClientId(), leadData));
            });
  }

  /**
   * Saves miscellaneous details for a lead asynchronously after successful creation. This method
   * fires and forgets - it doesn't block the response.
   *
   * @param clientId the client ID from the lead creation response
   * @param leadData the original lead data containing miscellaneous details
   */
  private void saveMiscellaneousDetailsAsync(Integer clientId, Lead leadData) {

    leadMiscellaneousDetailsService.saveMiscellaneousDetailsAsync(
        clientId, leadData.getMiscellaneousDetails());
  }

  public Mono<M2pClientCreationResponseDTO> createLeadAndCacheDetails(
      Lead leadData, String productCode) {
    return createLead(leadData, productCode)
        .flatMap(
            response ->
                extractAndCacheClientDetails(productCode, leadData, response.getClientId())
                    .thenReturn(response));
  }

  /** Cache client details (write-through cache). */
  private Mono<Void> extractAndCacheClientDetails(
      String productCode, Lead leadData, Integer clientId) {
    return Mono.fromSupplier(
            () -> clientCacheService.buildClientCacheObject(productCode, leadData, clientId))
        .flatMap(clientCacheService::cacheClientDetailsConsumedInPanValidation)
        .doOnError(
            e ->
                log.error(
                    "[REDIS_OPS] Failed async caching for clientId={}, productCode={} error={}",
                    clientId,
                    productCode,
                    e.getMessage()));
  }

  public Mono<?> updateLead(LeadUpdate leadData, String leadId) {
    if (leadData.getAddressDetails() != null) {
      leadData
          .getAddressDetails()
          .forEach(
              address -> {
                address.setAddressLineOne(sanitizeAddress(address.getAddressLineOne()));
                address.setAddressLineTwo(sanitizeAddress(address.getAddressLineTwo()));
                address.setLandmark(sanitizeAddress(address.getLandmark()));
              });
    }
    M2pLeadUpdateDTO leadRequest = getM2PUpdateLeadRequest(leadData);
    return m2PWrapperApi.updateLead(leadRequest, leadId);
  }

  /* Regex to sanitize address with special characters as allowed by experian */
  private String sanitizeAddress(String input) {
    if (input == null) return null;
    return input.replaceAll("[^A-Za-z0-9:,@+ _\\t\\r\\n\"().\\-{}\\[\\]/]", "-");
  }

  public Mono<?> uploadSelfieAgainstLead(SelfieUpload selfieUploadData, String leadId) {
    try {
      FileValidatorUtil.isValidBase64(
          selfieUploadData.getFileContent(), selfieUploadData.getFileType());
    } catch (BaseException e) {
      return Mono.error(e);
    }
    return m2PWrapperApi
        .uploadSelfieAgainstLead(selfieUploadData, leadId, null)
        .flatMap(
            response -> {
              M2pSelfieUploadResponseDTO m2pSelfieUploadResponseDTO =
                  (M2pSelfieUploadResponseDTO) response;
              return uploadScoreAgainstClient(
                      selfieUploadData,
                      leadId,
                      String.valueOf(m2pSelfieUploadResponseDTO.imageId()),
                      m2pSelfieUploadResponseDTO)
                  .flatMap(res -> Mono.just(response))
                  .onErrorResume(e -> Mono.just(response))
                  .switchIfEmpty(Mono.defer(() -> Mono.just(response)));
            });
  }

  private Mono<?> uploadScoreAgainstClient(
      SelfieUpload selfieUploadData,
      String leadId,
      String imageId,
      M2pSelfieUploadResponseDTO m2pSelfieUploadResponseDTO) {

    LivelinessScoreDataTableDTO livelinessScoreDataTableDTO =
        LivelinessScoreDataTableDTO.builder()
            .score(selfieUploadData.getScore())
            .lead(leadId)
            .timestamp(convertEpochMilliToIst(System.currentTimeMillis()))
            .imageId(imageId)
            .ip(selfieUploadData.getIp())
            .build();

    if (!validateLivelinessScoreParameters(livelinessScoreDataTableDTO)) {
      return Mono.error(
          new BaseException("Validations Failed", null, HttpStatus.PRECONDITION_FAILED));
    }
    return m2PWrapperApi.uploadScoreAgainstLead(livelinessScoreDataTableDTO, leadId);
  }

  public Flux<?> getKycIdentifiersAgainstLead(String leadId) {
    return m2PWrapperApi.getKycIdentifiersAgainstLead(leadId);
  }

  private M2pLeadRequestDTO getM2PLeadRequest(Lead leadData, String officeName) {
    return LeadDataUtil.getM2pLeadRequest(leadData, officeName);
  }

  private M2pLeadUpdateDTO getM2PUpdateLeadRequest(LeadUpdate leadData) {
    return LeadDataUtil.getM2pLeadUpdateRequest(leadData);
  }

  public Mono<?> uploadDocumentsAgainstLead(
      LeadBulkDocumentsUploadRequest leadBulkDocumentsUploadRequest, String leadId) {
    return Flux.fromIterable(leadBulkDocumentsUploadRequest.getDocuments())
        .flatMap(
            doc ->
                FileValidatorUtil.validateFile(
                        doc.getDocument().getFilePath(), doc.getDocument().getFileType())
                    .map(valid -> new AbstractMap.SimpleEntry<>(doc, valid)))
        .collectList()
        .flatMap(
            results -> {
              List<LeadBulkDocumentsUploadRequest.DocumentDetailsDTO> invalidDocs =
                  results.stream()
                      .filter(entry -> !entry.getValue())
                      .map(Map.Entry::getKey)
                      .toList();

              if (!invalidDocs.isEmpty()) {
                return Mono.error(
                    new RuntimeException(
                        "Validation failed for documents: "
                            + invalidDocs.stream()
                                .map(
                                    doc ->
                                        doc.getDocument().getFileName()
                                            + " ("
                                            + (doc.getDocument().getFileType() != null
                                                ? doc.getDocument().getFileType()
                                                : MediaType.APPLICATION_PDF_VALUE)
                                            + ")")
                                .toList()));
              }
              return m2PWrapperApi.uploadDocumentsAgainstLead(
                  leadId, leadBulkDocumentsUploadRequest);
            });
  }

  public Mono<M2pClientCreationResponseDTO> leadDedupeCheck(DedupeLeadRequest dedupeLeadRequest) {
    return m2PWrapperApi.processM2pDedupeRequest(dedupeLeadRequest.getPanNumber());
  }

  public Mono<?> getLoanLoanExternalId(String leadId) {
    return m2PWrapperApi.getLeadLoanExternalId(leadId);
  }

  public Mono<?> getLoanIdsByLeadId(String leadId, String productCode) {
    return partnerMasterService
        .findByProductCode(productCode)
        .flatMap(
            partnerMasterEntity -> {
              String m2pProductId = partnerMasterEntity.getM2pProductId();
              if (m2pProductId == null) {
                log.error(
                    "[{}] m2p_product_id not found for {}",
                    "FETCH_ALL_LOANS_BY_LEAD_ID",
                    productCode);
                return Mono.error(
                    new BaseException(NOT_FOUND, M2P_PRODUCT_ID_NOT_FOUND, HttpStatus.NOT_FOUND));
              }
              // temp code: hot fix
              if ("29".equals(m2pProductId)) {
                // Call both 25 and 29 in parallel and merge arrays
                Mono<List<?>> loans25 =
                    m2PWrapperApi.getLoanIdsByLeadId(leadId, "25").map(res -> (List<?>) res);
                Mono<List<?>> loans29 =
                    m2PWrapperApi.getLoanIdsByLeadId(leadId, "29").map(res -> (List<?>) res);

                return Mono.zip(loans25, loans29)
                    .map(
                        tuple -> {
                          List<?> list25 = tuple.getT1();
                          List<?> list29 = tuple.getT2();

                          List<Object> merged = new ArrayList<>();
                          merged.addAll(list25);
                          merged.addAll(list29);
                          return merged;
                        });
              }

              // Normal case
              return m2PWrapperApi.getLoanIdsByLeadId(leadId, m2pProductId);
            });
  }

  public Mono<M2pPanAadhaarDetailsDTO> getPanAadhaarDetailsByClientId(String leadId) {
    return m2PWrapperApi.getPanAadhaarDetailsByClientId(leadId);
  }

  public Flux<M2pBankDetailsResponseDTO> fetchBankAccountDetails(String leadId) {
    return m2PWrapperApi.fetchBankAccountDetails(leadId);
  }

  public Mono<?> addBankAccount(M2pBankDetailsRequestDTO m2pRequestDTO, String leadId) {
    return m2PWrapperApi.addBankAccountDetails(leadId, m2pRequestDTO);
  }

  public Mono<?> getLeadInfo(String mobileNumber) {
    return m2PWrapperApi.getLeadInfo(mobileNumber);
  }

  public Flux<LeadIdResponse> getLeadInfoWithDOB(String mobileNumber, String dateOfBirth) {
    return m2PWrapperApi.getLeadInfoWithDOB(mobileNumber, dateOfBirth);
  }

  public Flux<LeadIdResponse> getLeadInfoWithDOBAndPAN(
      String mobileNumber, String dateOfBirth, String panLast4Digits) {
    return m2PWrapperApi.getLeadInfoWithDOBAndPAN(mobileNumber, dateOfBirth, panLast4Digits);
  }

  public Mono<?> getRepaymentScheduleWithoutLoan(
      RepaymentScheduleRequest repaymentScheduleRequest, String productCode, String leadId) {
    return m2PWrapperApi
        .getProductDetailsByProductCode(productCode)
        .flatMap(
            productDetails -> {
              if (null == productDetails.getLosWorkflowMapping()) {
                return Mono.error(
                    new BaseException(PRODUCT_MAPPING_NOT_FOUND, null, HttpStatus.BAD_REQUEST));
              }
              if (null == productDetails.getLosProductOfficeMapping()
                  || productDetails.getLosProductOfficeMapping().isEmpty()) {
                return Mono.error(
                    new BaseException(OFFICE_MAPPING_NOT_FOUND, null, HttpStatus.NOT_FOUND));
              }
              LeadDataUtil.populateRepaymentScheduleRequest(
                  repaymentScheduleRequest, productDetails, leadId);
              return m2PWrapperApi.getRepaymentScheduleWithoutLoan(repaymentScheduleRequest);
            });
  }

  @SuppressWarnings("unchecked")
  public Mono<?> updateLeadOnCkycSuccess(String loanId) {

    return loanClientLookupService
        .getClientIdForLoan(loanId, "CKYC_UPDATE")
        .flatMap(
            clientId -> {
              String leadId = String.valueOf(clientId);

              return kycService
                  .getCkycInfo(leadId)
                  .flatMap(
                      ckycInfo -> {
                        if (ckycInfo == null || ckycInfo.getAddressInfo() == null) {
                          return Mono.error(
                              new BaseException(
                                  "CKYC information or address details not found",
                                  null,
                                  HttpStatus.NOT_FOUND));
                        }

                        return Mono.justOrEmpty(
                                ckycInfo.getAddressInfo().stream()
                                    .filter(
                                        address ->
                                            "PERMANENT".equalsIgnoreCase(address.getAddressType()))
                                    .findFirst())
                            .switchIfEmpty(
                                Mono.error(
                                    new BaseException(
                                        "Permanent address not found", null, HttpStatus.NOT_FOUND)))
                            .flatMap(
                                addressInfo -> {
                                  AddressDetailsDTO addressDetailsDTO =
                                      AddressDetailsDTO.builder()
                                          .addressType(List.of(AddressType.PERMANENT))
                                          .addressLineOne(addressInfo.getAddressLine1())
                                          .addressLineTwo(addressInfo.getAddressLine2())
                                          .postalCode(addressInfo.getPincode())
                                          .landmark(addressInfo.getCity())
                                          .ownershipType("own")
                                          .build();

                                  LeadUpdate leadUpdate =
                                      LeadUpdate.builder()
                                          .addressDetails(List.of(addressDetailsDTO))
                                          .build();

                                  return updateLead(leadUpdate, leadId)
                                      .onErrorResume(
                                          e ->
                                              Mono.error(
                                                  new BaseException(
                                                      "Failed to update lead",
                                                      e.getMessage(),
                                                      HttpStatus.INTERNAL_SERVER_ERROR)));
                                });
                      })
                  .onErrorResume(
                      e ->
                          Mono.error(
                              new BaseException(
                                  "Error fetching CKYC information",
                                  e.getMessage(),
                                  HttpStatus.INTERNAL_SERVER_ERROR)));
            })
        .onErrorResume(
            e ->
                Mono.error(
                    new BaseException(
                        "Error fetching loan application",
                        e.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR)));
  }

  public Flux<M2pClientIdsOfNullUcic> updateAllUcic(String productCode) {
    return m2PWrapperApi
        .getClientIdsOfNullUcic() // Flux<M2pClientIdsOfNullUcic>
        .filter(
            clientIdOfNullUcic ->
                clientIdOfNullUcic.getClientId() > 0) // Ensure valid clientId (not default 0)
        .flatMap(
            clientIdOfNullUcic -> {
              int clientId = clientIdOfNullUcic.getClientId();
              log.info("Client {} doesn't have UCIC data.", clientId);

              return getLeadData(String.valueOf(clientId)) // Mono<ClientDetailsResponseDto>
                  .onErrorResume(
                      error -> {
                        log.error(
                            "Failed to fetch client details for clientId {}: {}",
                            clientId,
                            error.getMessage());
                        return Mono.empty(); // Skip this client and continue with the next one
                      })
                  .flatMap(
                      clientDetails -> {
                        String pan = clientDetails.getClientPandocumentkey();
                        if (pan == null || pan.isBlank()) {
                          log.warn("Client {} has no PAN data, skipping UCIC update.", clientId);
                          return Mono.empty(); // Return unchanged client data
                        }

                        M2pUcicUpdateDTO ucicRequest =
                            M2pUcicUpdateDTO.builder()
                                .ucic(LeadDataUtil.generateUcic(pan))
                                .locale(EN)
                                .dateFormat(DATE_FORMAT)
                                .build();

                        return m2PWrapperApi
                            .updateUcic(ucicRequest, String.valueOf(clientId))
                            .doOnSuccess(
                                res -> log.info("UCIC data updated for client {}", clientId))
                            .thenReturn(
                                clientIdOfNullUcic); // Return original object after processing
                      });
            });
  }

  public Flux<M2pNpaLoanApplicationsDto> getNpaLoansByClientId(String leadId) {
    return m2PWrapperApi.getNpaLoansByClientId(leadId);
  }

  public Flux<M2pMaxDpdResponseDto> getMaxDpdByClientId(String leadId) {
    return m2PWrapperApi.getMaxDpdByClientId(leadId);
  }

  public Mono<?> updateVerifyBankDetails(String clientId, Object data) {
    return m2PWrapperApi.updateVerifyBankDetails(clientId, data);
  }

  public Mono<?> updatePanDetails(String loanId, PanDetails panDetails) {
    return m2PWrapperApi.updatePanDeatils(loanId, panDetails);
  }
}
