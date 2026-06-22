package com.trillionloans.los.service.db;

import static com.trillionloans.los.constant.StringConstants.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.internal.Partner;
import com.trillionloans.los.model.dto.internal.PartnerUpdate;
import com.trillionloans.los.model.entity.PartnerMasterEntity;
import com.trillionloans.los.repository.PartnerMasterRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class PartnerMasterService {

  private final PartnerMasterRepository partnerMasterRepository;
  private final RedisCacheService redisCacheService;
  private final Gson gson;
  private static final String REDIS_KEY = "PARTNER_CONFIG:";
  private static final String SECONDARY_CACHE_REDIS_KEY = "PRODUCT_PARTNER_CONFIG:";

  public Mono<PartnerMasterEntity> findByPartnerIdAndStatus(String partnerId, String status) {
    return redisCacheService
        .getKey(REDIS_KEY + partnerId)
        .flatMap(
            entity -> {
              log.info("[{}] cache hit for key: {}", REDIS_OPS, partnerId);
              return Mono.just(gson.fromJson(entity, PartnerMasterEntity.class));
            })
        .switchIfEmpty(
            partnerMasterRepository
                .findByPartnerIdAndStatus(partnerId, status)
                .switchIfEmpty(
                    Mono.defer(
                        () ->
                            Mono.error(
                                new BaseException(
                                    SOMETHING_WENT_WRONG_CACHE,
                                    SOMETHING_WENT_WRONG_CACHE,
                                    HttpStatus.INTERNAL_SERVER_ERROR))))
                .doOnSuccess(
                    entity -> {
                      log.info("[{}] cache miss for key: {}", REDIS_OPS, partnerId);
                      // Primary cache
                      redisCacheService
                          .putKey(REDIS_KEY + partnerId, gson.toJson(entity))
                          .subscribe();

                      // Secondary cache: PRODUCT_PARTNER_CONFIG:<productCode>
                      String productPartnerKey =
                          SECONDARY_CACHE_REDIS_KEY + entity.getProductCode();

                      redisCacheService
                          .getKey(productPartnerKey)
                          .defaultIfEmpty("[]")
                          .flatMap(
                              existing -> {
                                List<String> partnerIds =
                                    gson.fromJson(
                                        existing, new TypeToken<List<String>>() {}.getType());

                                if (!partnerIds.contains(entity.getPartnerId())) {
                                  partnerIds.add(entity.getPartnerId());
                                  log.debug(
                                      "[{}] Adding partnerId {} to key {}",
                                      REDIS_OPS,
                                      entity.getPartnerId(),
                                      productPartnerKey);
                                } else {
                                  log.debug(
                                      "[{}] partnerId {} already exists in key {}",
                                      REDIS_OPS,
                                      entity.getPartnerId(),
                                      productPartnerKey);
                                }

                                return redisCacheService
                                    .putKey(productPartnerKey, gson.toJson(partnerIds))
                                    .doOnSuccess(
                                        v ->
                                            log.debug(
                                                "[{}] Updated Redis key {} successfully",
                                                REDIS_OPS,
                                                productPartnerKey))
                                    .doOnError(
                                        err ->
                                            log.error(
                                                "[{}] Error updating Redis key {}: {}",
                                                REDIS_OPS,
                                                productPartnerKey,
                                                err.getMessage(),
                                                err));
                              })
                          .subscribe();
                    }))
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] error while fetching partner entity: {}", REDIS_OPS, partnerId, error);
              return Mono.error(
                  new BaseException(
                      SOMETHING_WENT_WRONG_CACHE,
                      SOMETHING_WENT_WRONG_CACHE,
                      HttpStatus.INTERNAL_SERVER_ERROR));
            });
  }

  public Mono<PartnerMasterEntity> createPartner(Partner partner) {
    return partnerMasterRepository
        .findByPartnerId(partner.getPartnerId())
        .flatMap(
            existingPartner ->
                Mono.error(
                    new BaseException(
                        "Partner already exists!", partner.getPartnerId(), HttpStatus.CONFLICT)))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  PartnerMasterEntity partnerEntity = new PartnerMasterEntity();
                  partnerEntity.setPartnerId(partner.getPartnerId());
                  partnerEntity.setPartnerName(partner.getPartnerName());
                  partnerEntity.setProductCode(partner.getProductCode());
                  partnerEntity.setProductName(partner.getProductName());
                  partnerEntity.setProductType(partner.getProductType());
                  partnerEntity.setStatus(partner.getStatus().getDisplayName());
                  return partnerMasterRepository.save(partnerEntity);
                }))
        .doOnSuccess(entity -> log.info("partner {} created successfully!", partner.getPartnerId()))
        .cast(PartnerMasterEntity.class);
  }

  public Mono<PartnerMasterEntity> updatePartner(String partnerId, PartnerUpdate partnerUpdate) {
    return partnerMasterRepository
        .findByPartnerId(partnerId)
        .switchIfEmpty(
            Mono.error(new BaseException("partner not found!", partnerId, HttpStatus.NOT_FOUND)))
        .flatMap(
            partner -> {
              if (!ObjectUtils.isEmpty(partnerUpdate.getPartnerName()))
                partner.setPartnerName(partnerUpdate.getPartnerName());
              if (!ObjectUtils.isEmpty(partnerUpdate.getProductCode()))
                partner.setProductCode(partnerUpdate.getProductCode());
              if (!ObjectUtils.isEmpty(partnerUpdate.getProductName()))
                partner.setProductName(partnerUpdate.getProductName());
              if (!ObjectUtils.isEmpty(partnerUpdate.getProductType()))
                partner.setProductType(partnerUpdate.getProductType());
              if (!ObjectUtils.isEmpty(partnerUpdate.getStatus()))
                partner.setStatus(partnerUpdate.getStatus());
              return partnerMasterRepository.save(partner);
            })
        .doOnSuccess(entity -> log.info("partner {} updated successfully!", partnerId));
  }

  public Mono<PartnerMasterEntity> findByProductCode(String productCode) {
    return partnerMasterRepository
        .findByProductCode(productCode)
        .switchIfEmpty(Mono.error(new BaseException(NOT_FOUND, productCode, HttpStatus.NOT_FOUND)))
        .doOnSuccess(
            partner ->
                log.info(
                    "[{}] Partner fetched successfully for product code {}!",
                    PARTNER_MASTER_OPERATION,
                    productCode));
  }

  public Mono<PartnerMasterEntity> findByM2pProductId(String m2pProductId) {
    return partnerMasterRepository
        .findByM2pProductId(m2pProductId)
        .switchIfEmpty(Mono.error(new BaseException(NOT_FOUND, m2pProductId, HttpStatus.NOT_FOUND)))
        .doOnSuccess(
            partner ->
                log.info(
                    "[{}] Partner fetched successfully for product Id M2P {}!",
                    PARTNER_MASTER_OPERATION,
                    m2pProductId));
  }

  public Mono<String> getProductIdByCode(String productCode) {
    String redisKey = SECONDARY_CACHE_REDIS_KEY + productCode;

    return redisCacheService
        .getKey(redisKey)
        .defaultIfEmpty("[]") // fallback to empty list
        .flatMap(
            cached -> {
              List<String> partnerIds =
                  gson.fromJson(cached, new TypeToken<List<String>>() {}.getType());

              if (partnerIds.isEmpty()) {
                // Fallback to DB if nothing in Redis
                return partnerMasterRepository
                    .findByProductCode(productCode)
                    .map(PartnerMasterEntity::getPartnerId);
              } else {
                // Return the first partner ID from cache
                return Mono.just(partnerIds.get(0));
              }
            });
  }
}
