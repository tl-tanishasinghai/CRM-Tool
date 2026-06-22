package com.trillionloans.los.controller.internal;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.model.dto.internal.Product;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/internal/products")
@AllArgsConstructor
@RestController
@Hidden
@Validated
public class ProductMasterController {
  private final ProductConfigMasterService productConfigMasterService;

  @PostMapping()
  public Mono<ResponseEntity<?>> addProduct(@SecureInput @Valid @RequestBody Product product) {
    return Mono.just(
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                productConfigMasterService
                    .createProduct(product)
                    .map(productData -> Map.of(PRODUCT_CODE, productData.getProductCode()))));
  }

  @DeleteMapping("/{productCode}")
  public Mono<ResponseEntity<Mono<String>>> deleteMapping(
      @SecureInput @Valid @PathVariable(name = "productCode") String productCode) {
    return Mono.just(ResponseEntity.ok(productConfigMasterService.deleteProduct(productCode)));
  }
}
