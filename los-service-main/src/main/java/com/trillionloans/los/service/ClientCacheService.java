package com.trillionloans.los.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.RelationshipType;
import com.trillionloans.los.model.ClientCacheDTO;
import com.trillionloans.los.model.dto.ClientDetailsDTO;
import com.trillionloans.los.model.dto.ClientIdentifierDetailsDTO;
import com.trillionloans.los.model.dto.FamilyDetailsDTO;
import com.trillionloans.los.model.request.Lead;
import com.trillionloans.los.model.response.ClientDetailsResponseDto;
import com.trillionloans.los.service.db.RedisCacheService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service responsible for caching and retrieving client details.
 *
 * <p>This service provides:
 *
 * <ul>
 *   <li>Write-through caching of client details in Redis (with TTL).
 *   <li>Conversion utilities for transforming data from lead service into a cache-friendly format.
 * </ul>
 *
 * <p>Default TTL for cached entries is 24 hours (86400 seconds), configurable via {@code
 * cache.client-details.ttl-seconds} in application properties.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientCacheService {
  private final M2PWrapperApi m2PWrapperApi;
  private final RedisCacheService redisCacheService;

  @Value("${cache.client-details.ttl-seconds:86400}")
  private long clientDetailsTtlSeconds;

  public static final String CLIENT_DETAILS_REDIS_KEY_PREFIX = "client_details";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** Cache client details silently (write-through cache) */
  public Mono<Void> cacheClientDetailsConsumedInPanValidation(ClientCacheDTO clientCacheDTO) {
    String cacheKey =
        buildCacheKey(
            clientCacheDTO.getProductCode(), String.valueOf(clientCacheDTO.getClientId()));

    log.info(
        "[CLIENT_CACHE] Attempting to cache client details. clientId={},"
            + " productCode={}, cacheKey={}, ttl={}s",
        clientCacheDTO.getClientId(),
        clientCacheDTO.getProductCode(),
        cacheKey,
        clientDetailsTtlSeconds);

    return redisCacheService
        .cacheObjectSilently(cacheKey, clientCacheDTO, clientDetailsTtlSeconds, false)
        .doOnSuccess(
            unused ->
                log.info(
                    "[CLIENT_CACHE] Successfully cached client details."
                        + " clientId={}, cacheKey={}",
                    clientCacheDTO.getClientId(),
                    cacheKey))
        .doOnError(
            e ->
                log.error(
                    "[CLIENT_CACHE] Failed to cache client details."
                        + " clientId={}, cacheKey={}, error={}",
                    clientCacheDTO.getClientId(),
                    cacheKey,
                    e.getMessage()));
  }

  /**
   * Fetch client details with cache → fallback to LeadService. Strict client details fetch flow: 1.
   * Try Redis - If hit → deserialize and return - If Redis error → log warning, fallback to
   * LeadService 2. If Redis empty → fallback to LeadService 3. If LeadService also fails → hard
   * fail
   */
  public Mono<ClientCacheDTO> fetchClientDetails(
      String leadId, String productCode, String logPrefix) {
    log.info(
        "[{}][CLIENT_CACHE] Fetching client details. leadId={}, productCode={}",
        logPrefix,
        leadId,
        productCode);

    String cacheKey = buildCacheKey(productCode, leadId);

    return Mono.deferContextual(
        ctx ->
            redisCacheService
                .getKey(cacheKey)
                .flatMap(
                    cacheJson -> {
                      try {
                        log.info(
                            "[{}][CLIENT_CACHE][CACHE_HIT] - Found client details in Redis for"
                                + " key={}",
                            logPrefix,
                            cacheKey);

                        ClientCacheDTO cacheData =
                            OBJECT_MAPPER.readValue(cacheJson, ClientCacheDTO.class);
                        return Mono.just(cacheData);
                      } catch (Exception e) {
                        log.error(
                            "[{}][CLIENT_CACHE] DESERIALIZATION_ERROR Failed to parse Redis value"
                                + " for key={}, error={}",
                            logPrefix,
                            cacheKey,
                            e.getMessage());
                        return fetchFromLeadService(leadId, productCode);
                      }
                    })
                .switchIfEmpty(
                    Mono.defer(
                        () -> {
                          log.info(
                              "[{}][CLIENT_CACHE][CACHE_MISS] - No Redis entry found for key={}."
                                  + " Falling back to LeadService",
                              logPrefix,
                              cacheKey);
                          return fetchFromLeadService(leadId, productCode);
                        }))
                .onErrorResume(
                    ex -> {
                      log.warn(
                          "[{}][CLIENT_CACHE][CACHE_ERROR] Redis unavailable, falling back to"
                              + " LeadService. error={}",
                          logPrefix,
                          ex.getMessage());

                      return fetchFromLeadService(leadId, productCode)
                          .onErrorResume(
                              dbEx -> {
                                log.error(
                                    "[{}][CLIENT_CACHE][DB_ERROR] - LeadService unavailable."
                                        + " leadId={}, productCode={}",
                                    logPrefix,
                                    leadId,
                                    productCode,
                                    dbEx);
                                return Mono.error(dbEx);
                              });
                    }));
  }

  private String buildCacheKey(String productCode, String clientId) {
    return CLIENT_DETAILS_REDIS_KEY_PREFIX + ":" + productCode + ":" + clientId;
  }

  public Mono<ClientCacheDTO> fetchFromLeadService(String leadId, String productCode) {
    return this.getLeadData(leadId).map(client -> toClientCacheDTO(client, productCode));
  }

  private Mono<ClientDetailsResponseDto> getLeadData(String leadId) {
    return m2PWrapperApi.getLeadData(leadId);
  }

  private static ClientCacheDTO toClientCacheDTO(
      ClientDetailsResponseDto client, String productCode) {
    // convert ClientDetailsDTO → ClientCacheDTO

    if (client == null) {
      return null;
    }
    return ClientCacheDTO.builder()
        .clientId(client.getClientId())
        .productCode(productCode)
        .firstName(client.getFirstName())
        .middleName(client.getMiddleName())
        .lastName(client.getLastName())
        .fatherFirstName(client.getFfirstName())
        .fatherLastName(client.getFlastName())
        .dateOfBirth(client.getDateOfBirth())
        .panNumber(client.getClientPandocumentkey())
        .build();
  }

  public ClientCacheDTO buildClientCacheObject(
      String productCode, Lead leadData, Integer clientId) {

    if (leadData == null) {
      leadData = new Lead();
    }

    ClientDetailsDTO clientDetails = leadData.getClientDetails();
    if (clientDetails == null) {
      clientDetails = new ClientDetailsDTO();
    }

    // Name
    String firstName = Optional.ofNullable(clientDetails.getFirstName()).orElse(StringUtils.EMPTY);
    String middleName =
        Optional.ofNullable(clientDetails.getMiddleName()).orElse(StringUtils.EMPTY);
    String lastName = Optional.ofNullable(clientDetails.getLastName()).orElse(StringUtils.EMPTY);

    // Date of birth
    String dateOfBirth =
        Optional.ofNullable(clientDetails.getDateOfBirth()).orElse(StringUtils.EMPTY);

    // Father's name
    List<FamilyDetailsDTO> familyDetails =
        Optional.ofNullable(leadData.getFamilyDetails()).orElse(Collections.emptyList());

    String fatherFirstName =
        familyDetails.stream()
            .filter(
                member ->
                    member != null && RelationshipType.FATHER.equals(member.getRelationship()))
            .map(member -> Optional.ofNullable(member.getFirstName()).orElse(StringUtils.EMPTY))
            .findFirst()
            .orElse(StringUtils.EMPTY);

    String fatherLastName =
        familyDetails.stream()
            .filter(
                member ->
                    member != null && RelationshipType.FATHER.equals(member.getRelationship()))
            .map(member -> Optional.ofNullable(member.getLastName()).orElse(StringUtils.EMPTY))
            .findFirst()
            .orElse(StringUtils.EMPTY);

    // PAN number
    List<ClientIdentifierDetailsDTO> identifiers =
        Optional.ofNullable(leadData.getClientIdentifierDetails()).orElse(Collections.emptyList());

    String panNumber =
        identifiers.stream()
            .filter(c -> c != null && "PAN".equals(c.getDocumentType()))
            .map(c -> Optional.ofNullable(c.getDocumentKey()).orElse(StringUtils.EMPTY))
            .findFirst()
            .orElse(StringUtils.EMPTY);

    // Build a cache object
    return ClientCacheDTO.builder()
        .clientId(clientId)
        .productCode(productCode)
        .firstName(firstName)
        .middleName(middleName)
        .lastName(lastName)
        .fatherFirstName(fatherFirstName)
        .fatherLastName(fatherLastName)
        .dateOfBirth(dateOfBirth)
        .panNumber(panNumber)
        .build();
  }
}
