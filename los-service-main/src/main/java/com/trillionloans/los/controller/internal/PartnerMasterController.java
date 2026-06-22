package com.trillionloans.los.controller.internal;

import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.model.dto.internal.Partner;
import com.trillionloans.los.model.dto.internal.PartnerUpdate;
import com.trillionloans.los.model.entity.PartnerMasterEntity;
import com.trillionloans.los.service.db.PartnerMasterService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/internal/partners")
@AllArgsConstructor
@RestController
@Hidden
@Validated
public class PartnerMasterController {

  private final PartnerMasterService partnerMasterService;

  @PostMapping("/partner")
  public Mono<ResponseEntity<Mono<Map<String, Object>>>> addPartner(
      @SecureInput @Valid @RequestBody Partner partner) {
    return Mono.just(
        ResponseEntity.status(HttpStatus.CREATED)
            .body(
                partnerMasterService
                    .createPartner(partner)
                    .map(product -> Map.of(PARTNER_ID, product.getPartnerId()))));
  }

  @PatchMapping("/partner/{partnerId}")
  public Mono<ResponseEntity<Mono<Map<String, Object>>>> updatePartner(
      @SecureInput @Valid @RequestBody PartnerUpdate partnerUpdate,
      @PathVariable(name = PARTNER_ID) String partnerId) {
    return Mono.just(
        ResponseEntity.ok(
            partnerMasterService
                .updatePartner(partnerId, partnerUpdate)
                .map(product -> Map.of(PARTNER_ID, product.getPartnerId()))));
  }

  @GetMapping("/partner")
  public Mono<ResponseEntity<PartnerMasterEntity>> fetchPartnerDetail(
      @RequestParam(name = "productCode") String productCode) {
    return partnerMasterService.findByProductCode(productCode).map(ResponseEntity::ok);
  }
}
