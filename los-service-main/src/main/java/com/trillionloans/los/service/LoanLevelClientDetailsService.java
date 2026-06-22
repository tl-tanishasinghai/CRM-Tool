package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;
import static com.trillionloans.los.constant.StringConstants.LOAN_LEVEL_CLIENT_DETAILS_REDIS_KEY_PREFIX;
import static com.trillionloans.los.util.JsonUtils.toJsonB;
import static com.trillionloans.los.util.LeadDataUtil.extractXmlFromBase64;
import static com.trillionloans.los.util.LoanLevelClientDetailUtil.buildClientDetailsUpdateDto;
import static com.trillionloans.los.util.LoanLevelClientDetailUtil.buildLoanLevelClientCacheObject;
import static com.trillionloans.los.util.LoanLevelClientDetailUtil.buildMClientUpdateDto;
import static com.trillionloans.los.util.LoanLevelClientDetailUtil.calculateClientDetailFieldChanges;
import static com.trillionloans.los.util.LoanLevelClientDetailUtil.mapToLoanLevelDetail;
import static com.trillionloans.los.util.LoanLevelClientDetailUtil.toLoanLevelClientCacheDTO;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.dto.internal.FieldChange;
import com.trillionloans.los.model.dto.internal.LoanLevelClientDetailsCacheDTO;
import com.trillionloans.los.model.entity.ClientCreationRequestDetail;
import com.trillionloans.los.model.entity.LoanLevelClientDetail;
import com.trillionloans.los.model.entity.MClientUpdateAuditLog;
import com.trillionloans.los.model.partner.m2p.M2pClientDetailsUpdateDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadUpdateDTO;
import com.trillionloans.los.model.request.Lead;
import com.trillionloans.los.model.response.ClientDetailsResponseDto;
import com.trillionloans.los.model.response.m2p.M2pLoanCreationResponseDTO;
import com.trillionloans.los.repository.ClientCreationRequestRepository;
import com.trillionloans.los.repository.LoanLevelClientDetailRepository;
import com.trillionloans.los.repository.MClientUpdateAuditRepository;
import com.trillionloans.los.service.db.RedisCacheService;
import com.trillionloans.los.util.LoanLevelClientDetailUtil;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanLevelClientDetailsService {
  private final ClientCreationRequestRepository clientCreationRequestRepository;
  private final LoanLevelClientDetailRepository loanLevelClientDetailRepository;
  private final MClientUpdateAuditRepository mClientUpdateAuditRepository;
  private final M2PWrapperApi m2PWrapperApi;
  private final RedisCacheService redisCacheService;
  private static final ZoneId IST_ZONE = ZoneId.of(ASIA_KOLKATA);

  @Value("${cache.loan-level-client-details.ttl-seconds:86400}")
  private long loanLevelClientDetailsTtlSeconds;

  @Value("${cache.loan-level-client-details.encryption:true}")
  private boolean encryption;

  public Mono<ClientCreationRequestDetail> persistClientCreationRequest(
      Lead lead, String productCode, String clientId) {
      ClientCreationRequestDetail clientCreationRequestDetail =
          ClientCreationRequestDetail.builder()
              .clientId(clientId)
              .productCode(productCode)
              .firstName(lead.getClientDetails().getFirstName())
              .middleName(lead.getClientDetails().getMiddleName())
              .lastName(lead.getClientDetails().getLastName())
              .gender(lead.getClientDetails().getGender().name())
              .dateOfBirth(lead.getClientDetails().getDateOfBirth())
              .email(lead.getClientDetails().getEmail())
              .mobileNo(lead.getClientDetails().getMobileNo())
              .alternateMobileNo(lead.getClientDetails().getAlternateMobileNo())
              .education(lead.getClientDetails().getEducation())
              .externalId(lead.getClientDetails().getExternalId())
              .addressDetails(toJsonB(lead.getAddressDetails()))
              .familyDetails(toJsonB(lead.getFamilyDetails()))
              .clientIdentifierDetails(toJsonB(lead.getClientIdentifierDetails()))
              .bankDetails(toJsonB(lead.getBankDetails()))
              .employmentDetails(toJsonB(lead.getEmploymentDetails()))
              .additionalDetails(toJsonB(lead.getAdditionalDetails()))
              .createdAt(LocalDateTime.now(IST_ZONE))
              .updatedAt(LocalDateTime.now(IST_ZONE))
              .build();

      return clientCreationRequestRepository.save(clientCreationRequestDetail);
  }

  public Mono<LoanLevelClientDetailsCacheDTO> persistToLoanLevelClientDetailsTableAndRedis(
      M2pLoanCreationResponseDTO response, String clientId, String productCode) {
    String loanApplicationId = Objects.toString(response.getResourceId(), null);
    String loanApplicationReferenceNo =
        Objects.toString(
            response.getAdditionalResponseData().getLoanApplicationReferenceNo(), null);
    return clientCreationRequestRepository
        .findLatestByClientIdAndProductCode(clientId, productCode)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "[ERROR][LOAN_LEVEL_CLIENT_DATA][DATABASE] no entry found in"
                          + " client_creation_request_details table for clientId: {}, productCode:"
                          + " {}. Skipping loan level persistence.",
                      clientId,
                      productCode);
                  return Mono.empty();
                }))
        .flatMap(
            clientCreationRequestDetail -> {
              LoanLevelClientDetail loanLevelClientDetail =
                  mapToLoanLevelDetail(
                      clientCreationRequestDetail, loanApplicationId, loanApplicationReferenceNo);
              return loanLevelClientDetailRepository
                  .save(loanLevelClientDetail)
                  .flatMap(
                      savedEntity -> {
                        log.info(
                            "[LOAN_LEVEL_CLIENT_DATA][DATABASE] loan level client details persisted"
                                + " for clientId: {}, loanApplicationId: {}, productCode: {}",
                            clientId,
                            loanApplicationId,
                            productCode);
                          LoanLevelClientDetailsCacheDTO cacheObj = buildLoanLevelClientCacheObject(savedEntity);
                          return cacheLoanLevelClientDetails(cacheObj)
                                  .thenReturn(cacheObj);
                      })
                  .doOnError(
                      err ->
                          log.error(
                              "[ERROR][LOAN_LEVEL_CLIENT_DATA][DATABASE] failed to persist client"
                                  + " details to loan_level_client_details table for clientId: {},"
                                  + " productCode: {}, error: {}",
                              clientId,
                              productCode,
                              err.getMessage()));
            });
  }

  /** Cache client details silently (write-through cache) */
  public Mono<Void> cacheLoanLevelClientDetails(
      LoanLevelClientDetailsCacheDTO loanLevelClientDetailsCacheDTO) {
    String cacheKey =
        buildLoanLevelCacheKey(
            loanLevelClientDetailsCacheDTO.getProductCode(),
            String.valueOf(loanLevelClientDetailsCacheDTO.getLoanApplicationId()));

    return redisCacheService
        .cacheObjectSilently(
            cacheKey, loanLevelClientDetailsCacheDTO, loanLevelClientDetailsTtlSeconds, encryption)
        .doOnSuccess(
            unused ->
                log.info(
                    "[LOAN_LEVEL_CLIENT_DATA][CLIENT_CACHE] successfully cached client details."
                        + " clientId={}, loanApplicationId={} ,cacheKey={}",
                    loanLevelClientDetailsCacheDTO.getClientId(),
                    loanLevelClientDetailsCacheDTO.getLoanApplicationId(),
                    cacheKey))
        .doOnError(
            e ->
                log.error(
                    "[ERROR][LOAN_LEVEL_CLIENT_DATA][CLIENT_CACHE] failed to cache client details."
                        + " clientId={}, loanApplicationId={}, cacheKey={}, error={}",
                    loanLevelClientDetailsCacheDTO.getClientId(),
                    loanLevelClientDetailsCacheDTO.getLoanApplicationId(),
                    cacheKey,
                    e.getMessage()));
  }

  /**
   * Fetch client details with cache → fallback to LeadService. client details fetch flow: 1. Try
   * Redis - If hit → deserialize and return - If Redis error → log warning, fallback to Database,
   * if not found in Database, fallback to M2P.
   */
  public Mono<LoanLevelClientDetailsCacheDTO> fetchLoanLevelClientDetails(
      String clientId, String loanApplicationId, String productCode) {
    log.info(
        "[LOAN_LEVEL_CLIENT_DATA][CLIENT_CACHE] fetching client details. loanApplicationId={},"
            + " productCode={}",
        loanApplicationId,
        productCode);

    String cacheKey = buildLoanLevelCacheKey(productCode, loanApplicationId);

    return Mono.defer(
        () -> {
          return redisCacheService
              .getObjectSilently(cacheKey, LoanLevelClientDetailsCacheDTO.class, encryption)
              .switchIfEmpty(
                  Mono.defer(
                      () -> {
                        log.info(
                            "[LOAN_LEVEL_CLIENT_DATA][CLIENT_CACHE][CACHE_MISS] no Redis entry"
                                + " found for key={}. Falling back on database.",
                            cacheKey);
                        return fetchClientDetailsFromDbWithM2pFallback(
                            clientId, loanApplicationId, productCode);
                      }))
              .onErrorResume(
                  ex -> {
                    log.error(
                        "[ERROR][LOAN_LEVEL_CLIENT_DATA][CLIENT_CACHE][CACHE_ERROR] Redis"
                            + " unavailable, falling back on database for loanApplicationId: {},"
                            + " clientId: {}. error={}",
                        loanApplicationId,
                        clientId,
                        ex.getMessage());

                    return fetchClientDetailsFromDbWithM2pFallback(
                            clientId, loanApplicationId, productCode)
                        .onErrorResume(
                            dbEx -> {
                              log.error(
                                  "[ERROR][LOAN_LEVEL_CLIENT_DATA][CLIENT_CACHE][DB_ERROR] database"
                                      + " unavailable. loanApplicationId:{}, clientId={},"
                                      + " productCode={}",
                                  loanApplicationId,
                                  clientId,
                                  productCode,
                                  dbEx);
                              return Mono.error(dbEx);
                            });
                  });
        });
  }

  private String buildLoanLevelCacheKey(String productCode, String loanApplicationId) {
    return LOAN_LEVEL_CLIENT_DETAILS_REDIS_KEY_PREFIX + ":" + productCode + ":" + loanApplicationId;
  }

  public Mono<LoanLevelClientDetailsCacheDTO> fetchClientDetailsFromDbWithM2pFallback(
      String clientId, String loanApplicationId, String productCode) {

    return fetchClientDetailsFromDb(loanApplicationId)
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "[LOAN_LEVEL_CLIENT_DATA][DB] no details for loanApplicationId: {}, clientId:"
                          + " {}. falling back to LeadService.",
                      loanApplicationId,
                      clientId);
                  return fetchClientDetailsFromLeadService(
                      clientId, loanApplicationId, productCode);
                }))
        .onErrorResume(
            e -> {
              log.error(
                  "[ERROR][LOAN_LEVEL_CLIENT_DATA][DB_FAILURE] error fetching client details from"
                      + " database for loanApplicationId: {}, clientId: {}, error: {}. Falling back"
                      + " to LeadService.",
                  loanApplicationId,
                  clientId,
                  e.getMessage());
              return fetchClientDetailsFromLeadService(clientId, loanApplicationId, productCode);
            });
  }

  public Mono<LoanLevelClientDetailsCacheDTO> fetchClientDetailsFromDb(String loanApplicationId) {
    return loanLevelClientDetailRepository
        .findByLoanApplicationId(loanApplicationId)
        .map(LoanLevelClientDetailUtil::buildLoanLevelClientCacheObject)
        .doOnNext(
            loanLevelClientDetailsCacheDTO ->
                cacheLoanLevelClientDetails(loanLevelClientDetailsCacheDTO)
                    .subscribe(
                        unused -> {},
                        error ->
                            log.error(
                                "[ERROR][LOAN_LEVEL_CLIENT_DATA][CLIENT_CACHE] async redis warm-up"
                                    + " failed for loanApplicationId={}. error={}",
                                loanApplicationId,
                                error.getMessage())));
  }

  public Mono<LoanLevelClientDetail> findLatestByClientIdAndProductCode(
      String clientId, String productCode) {
    return loanLevelClientDetailRepository.findLatestByClientIdAndProductCode(
        clientId, productCode);
  }

  public Mono<LoanLevelClientDetail> fetchLoanLevelClientDetailsFromDb(String loanApplicationId) {
    return loanLevelClientDetailRepository.findByLoanApplicationId(loanApplicationId);
  }

  public Mono<LoanLevelClientDetailsCacheDTO> fetchClientDetailsFromLeadService(
      String clientId, String loanApplicationId, String productCode) {
    return this.getLeadData(clientId)
        .map(client -> toLoanLevelClientCacheDTO(client, loanApplicationId, productCode));
  }

  private Mono<ClientDetailsResponseDto> getLeadData(String leadId) {
    return m2PWrapperApi.getLeadData(leadId);
  }

  public Mono<Void> updateAadhaarDetailsInDb(String loanApplicationId, String xmlRequestString) {
    return extractXmlFromBase64(xmlRequestString)
        .flatMap(
            clientXmlDetailsDTO ->
                loanLevelClientDetailRepository
                    .findByLoanApplicationId(loanApplicationId)
                    .flatMap(
                        existingRecord -> {
                          existingRecord.setUpdatedAt(LocalDateTime.now(IST_ZONE));
                          existingRecord.setAadhaarName(clientXmlDetailsDTO.getName());
                          existingRecord.setAadhaarDob(clientXmlDetailsDTO.getDob());
                          existingRecord.setAadhaarDependent(clientXmlDetailsDTO.getDependent());
                          existingRecord.setAadhaarCareOf(clientXmlDetailsDTO.getCareOf());

                          // Address Details
                          existingRecord.setAadhaarHouse(clientXmlDetailsDTO.getHouse());
                          existingRecord.setAadhaarStreet(clientXmlDetailsDTO.getStreet());
                          existingRecord.setAadhaarLandmark(clientXmlDetailsDTO.getLandmark());
                          existingRecord.setAadhaarLocality(clientXmlDetailsDTO.getLocality());
                          existingRecord.setAadhaarVtc(clientXmlDetailsDTO.getVtc());
                          existingRecord.setAadhaarSubDistrict(
                              clientXmlDetailsDTO.getSubdistrict());
                          existingRecord.setAadhaarDistrict(clientXmlDetailsDTO.getDistrict());
                          existingRecord.setAadhaarState(clientXmlDetailsDTO.getState());
                          existingRecord.setAadhaarPincode(clientXmlDetailsDTO.getPincode());
                          existingRecord.setAadhaarCountry(clientXmlDetailsDTO.getCountry());

                          return loanLevelClientDetailRepository.save(existingRecord);
                        }))
        .doOnSuccess(
            s ->
                log.info(
                    "[LOAN_LEVEL_CLIENT_DATA][AADHAAR_DETAILS_ENRICHMENT] successfully updated"
                        + " loan_level_client_details table for loanApplicationId: {}",
                    loanApplicationId))
        .doOnError(
            e ->
                log.error(
                    "[ERROR][LOAN_LEVEL_CLIENT_DATA][AADHAAR_DETAILS_ENRICHMENT] failed database"
                        + " update for loanApplicationId: {}. error: {}.",
                    loanApplicationId,
                    e.getMessage()))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "[ERROR][LOAN_LEVEL_CLIENT_DATA][AADHAAR_DETAILS_ENRICHMENT] no record found"
                          + " for loanApplicationId: {} in loan_level_client_details table.",
                      loanApplicationId);
                  return Mono.empty();
                }))
        .then();
  }

  public Mono<Void> updateMClientOnSuccessfulKYC(
      String clientId, String loanApplicationId, String productCode) {
    return loanLevelClientDetailRepository
        .findByLoanApplicationId(loanApplicationId)
        .flatMap(
            loanLevelClientDetail ->
                m2PWrapperApi
                    .getLeadFullData(clientId)
                    .flatMap(
                        currentLead -> {
                          // Check & Update only if any field is different
                          Map<String, Object> changesMap =
                              calculateClientDetailFieldChanges(loanLevelClientDetail, currentLead);

                          if (changesMap.isEmpty()) {
                            log.info(
                                "[LOAN_LEVEL_CLIENT_DATA][M_CLIENT_UPDATE] data is already"
                                    + " up-to-date for clientId: {}, loanApplicationId: {}."
                                    + " Skipping m_client update.",
                                clientId,
                                loanApplicationId);
                            return Mono.empty();
                          }

                          log.info(
                              "[LOAN_LEVEL_CLIENT_DATA][M_CLIENT_UPDATE] data mismatch detected"
                                  + " for clientId: {}, loanApplicationId: {}. Triggering"
                                  + " m_client update.",
                              clientId,
                              loanApplicationId);

                          List<FieldChange> leadAuditTrail = new ArrayList<>();
                          List<FieldChange> clientDetailsAuditTrail = new ArrayList<>();

                          M2pLeadUpdateDTO m2pUpdateDto =
                              buildMClientUpdateDto(
                                  loanLevelClientDetail, currentLead, changesMap, leadAuditTrail);
                          M2pClientDetailsUpdateDTO m2pClientDetailsUpdateDTO =
                              buildClientDetailsUpdateDto(
                                  loanLevelClientDetail,
                                  currentLead,
                                  changesMap,
                                  clientDetailsAuditTrail);

                          Mono<Boolean> leadUpdateMono =
                              m2PWrapperApi
                                  .updateLead(m2pUpdateDto, clientId)
                                  .map(response -> true)
                                  .doOnError(
                                      e ->
                                          log.error(
                                              "[ERROR][LOAN_LEVEL_CLIENT_DATA][M_CLIENT_UPDATE]"
                                                  + " lead update failed for clientId: {}, error:"
                                                  + " {}",
                                              clientId,
                                              e.getMessage()))
                                  .onErrorReturn(false);

                          Mono<Boolean> clientDetailsUpdateMono =
                              (m2pClientDetailsUpdateDTO != null)
                                  ? m2PWrapperApi
                                      .updateClientDetails(m2pClientDetailsUpdateDTO, clientId)
                                      .map(response -> true)
                                      .doOnError(
                                          e ->
                                              log.error(
                                                  "[ERROR][LOAN_LEVEL_CLIENT_DATA][M_CLIENT_UPDATE]"
                                                      + " client details update failed for"
                                                      + " clientId: {}, error: {}",
                                                  clientId,
                                                  e.getMessage()))
                                      .onErrorReturn(false)
                                  : Mono.just(true);

                          return Mono.zip(leadUpdateMono, clientDetailsUpdateMono)
                              .flatMap(
                                  response -> {
                                    // log m_client update in database
                                    boolean leadUpdateSuccess = response.getT1();
                                    boolean clientDetailsUpdateSuccess = response.getT2();

                                    List<FieldChange> successfulChanges = new ArrayList<>();
                                    if (leadUpdateSuccess) successfulChanges.addAll(leadAuditTrail);
                                    if (clientDetailsUpdateSuccess
                                        && m2pClientDetailsUpdateDTO != null) {
                                      successfulChanges.addAll(clientDetailsAuditTrail);
                                    }

                                    if (!successfulChanges.isEmpty()) {
                                      MClientUpdateAuditLog mClientAuditEntry =
                                          MClientUpdateAuditLog.builder()
                                              .clientId(clientId)
                                              .productCode(productCode)
                                              .loanApplicationId(loanApplicationId)
                                              .firstName(
                                                  currentLead.getClientDetails().getFirstName())
                                              .middleName(
                                                  currentLead.getClientDetails().getMiddleName())
                                              .lastName(
                                                  currentLead.getClientDetails().getLastName())
                                              .gender(
                                                  currentLead.getClientDetails().getGender().name())
                                              .dateOfBirth(
                                                  currentLead.getClientDetails().getDateOfBirth())
                                              .email(currentLead.getClientDetails().getEmail())
                                              .mobileNo(
                                                  currentLead.getClientDetails().getMobileNo())
                                              .alternateMobileNo(
                                                  currentLead
                                                      .getClientDetails()
                                                      .getAlternateMobileNo())
                                              .education(
                                                  currentLead.getClientDetails().getEducation())
                                              .externalId(
                                                  currentLead.getClientDetails().getExternalId())
                                              .addressDetails(
                                                  toJsonB(currentLead.getAddressDetails()))
                                              .familyDetails(
                                                  toJsonB(currentLead.getFamilyDetails()))
                                              .clientIdentifierDetails(
                                                  toJsonB(currentLead.getClientIdentifierDetails()))
                                              .bankDetails(toJsonB(currentLead.getBankDetails()))
                                              .employmentDetails(
                                                  toJsonB(currentLead.getEmploymentDetails()))
                                              .additionalDetails(
                                                  toJsonB(currentLead.getAdditionalDetails()))
                                              .changedFields(toJsonB(successfulChanges))
                                              .createdAt(LocalDateTime.now(IST_ZONE))
                                              .updatedAt(LocalDateTime.now(IST_ZONE))
                                              .build();

                                      return mClientUpdateAuditRepository
                                          .save(mClientAuditEntry)
                                          .doOnSuccess(
                                              s ->
                                                  log.info(
                                                      "[LOAN_LEVEL_CLIENT_DATA][M_CLIENT_UPDATE_LOG]"
                                                          + " mClient update audit log persisted in"
                                                          + " database for clientId: {}",
                                                      clientId));
                                    } else {
                                      log.error(
                                          "[ERROR][LOAN_LEVEL_CLIENT_DATA][M_CLIENT_UPDATE] both"
                                              + " lead update and client details update failed."
                                              + " Skipping audit log for clientId: {}",
                                          clientId);
                                      return Mono.error(
                                          new RuntimeException("M-Client update failed"));
                                    }
                                  });
                        }))
        .doOnError(
            e ->
                log.error(
                    "[ERROR][LOAN_LEVEL_CLIENT_DATA][M_CLIENT_UPDATE] failed to update m_client for"
                        + " clientId: {}, loanApplicationId: {}, error: {}.",
                    clientId,
                    loanApplicationId,
                    e.getMessage()))
        .then();
  }
}
