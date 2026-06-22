package com.trillionloans.los.service.db;

import static com.trillionloans.los.constant.StringConstants.REDIS_OPS;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CACHE;

import com.google.gson.Gson;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.internal.Product;
import com.trillionloans.los.model.dto.internal.ProductConfigurationRecord;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.ProductConfigMasterEntity;
import com.trillionloans.los.repository.ProductConfigMasterRepository;
import io.r2dbc.postgresql.codec.Json;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Service
@Slf4j
@AllArgsConstructor
public class ProductConfigMasterService {

  private final ProductConfigMasterRepository productConfigMasterRepository;
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
              return Mono.justOrEmpty(gson.fromJson(entity, ProductConfigurationRecord.class));
            })
        .switchIfEmpty(
            productConfigMasterRepository
                .findProductConfigMasterByProductCode(productCode)
                .flatMap(
                    productConfigMasterEntity ->
                        Mono.just(
                            ProductConfigurationRecord.builder()
                                .partnerCode(productConfigMasterEntity.getPartnerCode())
                                .productControl(
                                    gson.fromJson(
                                        productConfigMasterEntity.getProductControl().asString(),
                                        ProductControl.class))
                                .build()))
                .doOnSuccess(
                    productConfigurationRecord -> {
                      log.info("[{}] cache miss for key: {}", REDIS_OPS, productCode);
                      redisCacheService
                          .putKey(REDIS_KEY + productCode, gson.toJson(productConfigurationRecord))
                          .subscribe();
                    }))
        .onErrorResume(
            error -> {
              log.error(
                  "[{}] {} for key: {}", REDIS_OPS, SOMETHING_WENT_WRONG_CACHE, productCode, error);
              return Mono.error(
                  new BaseException(
                      SOMETHING_WENT_WRONG_CACHE,
                      SOMETHING_WENT_WRONG_CACHE,
                      HttpStatus.INTERNAL_SERVER_ERROR));
            });
  }

  public Mono<ProductConfigMasterEntity> findByProductCodeFromDatabase(String productCode) {
    return productConfigMasterRepository
        .findProductConfigMasterByProductCode(productCode)
        .flatMap(Mono::just)
        .switchIfEmpty(
            Mono.defer(
                () -> Mono.error(new BaseException(null, null, HttpStatus.INTERNAL_SERVER_ERROR))))
        .onErrorResume(Mono::error);
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

  public Mono<ProductControl> findProductConfigForProductCodeFromDatabase(String productCode) {
    return productConfigMasterRepository
        .findProductConfigMasterByProductCode(productCode)
        .flatMap(
            data ->
                Mono.just(
                    gson.fromJson(data.getProductControl().asString(), ProductControl.class)));
  }

  public Mono<String> findPartnerCodeForProductCodeFromDatabase(String productCode) {
    return productConfigMasterRepository
        .findProductConfigMasterByProductCode(productCode)
        .flatMap(data -> Mono.just(data.getPartnerCode()));
  }

  public Mono<ProductConfigMasterEntity> createProduct(Product product) {
    return productConfigMasterRepository
        .findProductConfigMasterByProductCode(product.getProductCode())
        .flatMap(
            existingProduct ->
                Mono.error(
                    new BaseException(
                        "product already exists!", product.getProductCode(), HttpStatus.CONFLICT)))
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  ProductConfigMasterEntity productConfigMasterEntity =
                      new ProductConfigMasterEntity();
                  productConfigMasterEntity.setProductCode(product.getProductCode());
                  productConfigMasterEntity.setPartnerCode(product.getPartnerCode());
                  productConfigMasterEntity.setProductControl(Json.of(product.getProductControl()));
                  return productConfigMasterRepository.save(productConfigMasterEntity);
                }))
        .doOnSuccess(
            entity -> log.info("product {} created successfully!", product.getProductCode()))
        .cast(ProductConfigMasterEntity.class);
  }

  public Mono<String> deleteProduct(String productCode) {
    return productConfigMasterRepository
        .deleteByProductCode(productCode)
        .then(Mono.defer(() -> Mono.just("product deleted")));
  }
}
