package com.trillionloans.lms.service.db;

import static com.trillionloans.lms.constant.StringConstants.REDIS_OPS;
import static com.trillionloans.lms.constant.StringConstants.SOMETHING_WENT_WRONG_CACHE;
import static com.trillionloans.lms.constant.StringConstants.STRAPI_OPS;

import com.google.gson.Gson;
import com.trillionloans.lms.api.strapi.StrapiApiClient;
import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.model.dto.internal.ProductConfigurationRecord;
import com.trillionloans.lms.model.dto.internal.ProductControl;
import com.trillionloans.lms.model.dto.strapi.StrapiProductConfigDto;
import com.trillionloans.lms.util.StrapiProductControlMapper;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
@AllArgsConstructor
@Slf4j
public class ProductConfigMasterService {

  private final StrapiApiClient strapiApiClient;
  private final StrapiProductControlMapper strapiMapper;
  private final Gson gson;
  private final RedisCacheService redisCacheService;
  private static final String REDIS_KEY = "PRODUCT_CONFIG:";

  public Mono<ProductConfigurationRecord> findProductConfigByProductCodeFromCacheOrDatabase(
      String productCode) {
    return redisCacheService
        .getKey(REDIS_KEY + productCode)
        .flatMap(
            entity -> {
              log.info("[{}] cache hit for key: {}", REDIS_OPS, productCode);
              return Mono.just(gson.fromJson(entity, ProductConfigurationRecord.class));
            })
        .switchIfEmpty(
            strapiApiClient
                .findProductConfigByCode(productCode)
                .switchIfEmpty(
                    Mono.fromSupplier(
                        () -> {
                          log.warn(
                              "[{}] no product-configuration found in Strapi for product_code={},"
                                  + " proceeding with empty config",
                              STRAPI_OPS,
                              productCode);
                          return new StrapiProductConfigDto();
                        }))
                .flatMap(
                    productConfigDto -> {
                      ProductControl productControl =
                          strapiMapper.toProductControl(productConfigDto, null);
                      return Mono.just(
                          ProductConfigurationRecord.builder()
                              .partnerCode(productConfigDto.getPartnerCode())
                              .productControl(productControl)
                              .build());
                    })
                .doOnSuccess(
                    productConfigurationRecord -> {
                      log.info("[{}] cache miss for key: {}", REDIS_OPS, productCode);
                      redisCacheService
                          .putKey(REDIS_KEY + productCode, gson.toJson(productConfigurationRecord))
                          .subscribe();
                    }))
        .onErrorResume(
            error -> {
              log.error("[{}] {} for key: {}", REDIS_OPS, SOMETHING_WENT_WRONG_CACHE, productCode);
              return Mono.error(
                  new BaseException(
                      SOMETHING_WENT_WRONG_CACHE,
                      SOMETHING_WENT_WRONG_CACHE,
                      HttpStatus.INTERNAL_SERVER_ERROR));
            });
  }

  public ProductControl.Flow getFlowFromProductConfig(
      ProductControl productControl, String identifier) {
    List<ProductControl.Flow> flows = productControl.getFlows();
    return flows.stream()
        .filter(flow -> identifier.equals(flow.getIdentifier()))
        .findFirst()
        .orElse(null);
  }

  public Mono<Tuple2<String, ProductControl>> getProductConfigMasterData(String productCode) {
    return findProductConfigByProductCodeFromCacheOrDatabase(productCode)
        .flatMap(
            data ->
                Mono.zip(Mono.just(data.getPartnerCode()), Mono.just(data.getProductControl())));
  }
}
