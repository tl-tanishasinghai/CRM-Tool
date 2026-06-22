package com.trillionloans.lms.service.db;

import static com.trillionloans.lms.constant.StringConstants.REDIS_OPS;
import static com.trillionloans.lms.constant.StringConstants.SOMETHING_WENT_WRONG_CACHE;

import com.google.gson.Gson;
import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.model.entity.PartnerMasterEntity;
import com.trillionloans.lms.repository.PartnerMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class PartnerMasterService {

  private final PartnerMasterRepository partnerMasterRepository;
  private final RedisCacheService redisCacheService;
  private final Gson gson;
  private static final String REDIS_KEY = "PARTNER_CONFIG:";

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
                      redisCacheService
                          .putKey(REDIS_KEY + partnerId, gson.toJson(entity))
                          .subscribe();
                    }))
        .onErrorResume(
            error -> {
              log.error("[{}] error while fetching partner entity: {}", REDIS_OPS, partnerId);
              return Mono.error(
                  new BaseException(
                      SOMETHING_WENT_WRONG_CACHE,
                      SOMETHING_WENT_WRONG_CACHE,
                      HttpStatus.INTERNAL_SERVER_ERROR));
            });
  }
}
