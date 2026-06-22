package com.trillionloans.los.controller.internal;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.model.request.AutoDisbursalCallbackRequest;
import com.trillionloans.los.service.disbursal.DisbursalFacadeService;
import com.trillionloans.los.service.disbursal.DisbursalService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Handles internal APIs for auto disbursal operations. */
@RequestMapping("/partners/api/v1")
@Hidden
@AllArgsConstructor
@RestController
public class DisbursalFacadeController {

  private final DisbursalService disbursalService;
  private final DisbursalFacadeService disbursalFacadeService;

  /**
   * Checks and processes auto disbursal status for a product.
   *
   * @param productCode Product identifier.
   * @return Result of the status check.
   */
  @GetMapping("/disbursement/auto/check-status")
  public Mono<ResponseEntity<Mono<?>>> checkAndProcessAutoDisbursalStatus(
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(disbursalService.checkAndProcessAutoDisbursalStatus()));
  }

  /**
   * Registers the status of an auto disbursement callback.
   *
   * @param requestBody Callback request details.
   * @param productCode Product identifier.
   * @return Confirmation of callback registration.
   */
  @PostMapping("/callback/auto-disbursement-status")
  public Mono<ResponseEntity<?>> registerAutoDisbursementStatus(
      @RequestBody AutoDisbursalCallbackRequest requestBody,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            disbursalFacadeService.registerAutoDisbursementStatus(requestBody, productCode)));
  }
}
