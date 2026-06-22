package com.trillionloans.los.controller.internal;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.config.annotations.ControllerEvent;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.request.m2p.CreditLineStatusCallbackRequest;
import com.trillionloans.los.model.request.m2p.M2pCkycrCallbackRequest;
import com.trillionloans.los.model.request.m2p.M2pDisbursementCallBackRequest;
import com.trillionloans.los.model.request.m2p.M2pKycCallBackWithAmlRequest;
import com.trillionloans.los.model.request.m2p.M2pLoanApprovalCallBackRequest;
import com.trillionloans.los.model.request.m2p.M2pLoanClosureCallBackRequest;
import com.trillionloans.los.service.CreditLineService;
import com.trillionloans.los.service.M2pFacadeService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

@RequestMapping("/partners/api/v1")
@RestController
@Hidden
@AllArgsConstructor
@Validated
public class M2pFacadeController {
  private final M2pFacadeService m2pFacadeService;
  private final CreditLineService creditLineService;

  /**
   * Registers the disbursement status callback.
   *
   * @param requestBody The request body containing the disbursement status details.
   * @return A Mono wrapping a ResponseEntity containing the result of the operation.
   */
  @PostMapping("/callback/disbursement-status")
  @ControllerEvent(event = Event.DISBURSEMENT_CALLBACK)
  public Mono<ResponseEntity<?>> registerDisbursementStatus(
      @SecureInput @RequestBody M2pDisbursementCallBackRequest requestBody) {
    return Mono.deferContextual(
        ctx -> {
          m2pFacadeService
              .incrementPortfolioRiskParameters(requestBody)
              .contextWrite(Context.of(ctx))
              .subscribeOn(Schedulers.boundedElastic())
              .subscribe();
          return Mono.just(
              ResponseEntity.ok(m2pFacadeService.registerDisbursementStatus(requestBody)));
        });
  }

  /**
   * Registers the KYC status callback.
   *
   * @param kycCallBackRequest The request body containing the KYC status details.
   * @return A Mono wrapping a ResponseEntity containing another Mono that holds the result of the
   *     operation.
   */
  @PostMapping("/callback/kyc-status")
  @ControllerEvent(event = Event.KYC_CALLBACK)
  public Mono<ResponseEntity<Mono<?>>> registerKycStatus(
      @SecureInput @RequestBody M2pKycCallBackWithAmlRequest kycCallBackRequest) {
    return Mono.just(ResponseEntity.ok(m2pFacadeService.registerKycStatus(kycCallBackRequest)));
  }

  /**
   * Registers the OKYC status callback.
   *
   * @param kycCallBackRequest The request body containing the OKYC status details.
   * @return A Mono wrapping a ResponseEntity containing another Mono that holds the result of the
   *     operation.
   */
  @PostMapping("/callback/okyc-status")
  @ControllerEvent(event = Event.OKYC_CALLBACK)
  public Mono<ResponseEntity<Mono<?>>> registerOKycStatus(
      @SecureInput @RequestBody M2pKycCallBackWithAmlRequest kycCallBackRequest) {
    return Mono.just(ResponseEntity.ok(m2pFacadeService.registerOKycStatus(kycCallBackRequest)));
  }

  /**
   * Registers the eSign status callback.
   *
   * @param requestBody The request body containing the eSign status details.
   * @return A Mono wrapping a ResponseEntity containing another Mono that holds the result of the
   *     operation.
   */
  @PostMapping("/callback/eSign-status")
  @ControllerEvent(event = Event.ESIGN_CALLBACK)
  public Mono<ResponseEntity<Mono<?>>> registerESignStatus(
      @SecureInput @RequestBody M2pKycCallBackWithAmlRequest requestBody) {
    return Mono.just(ResponseEntity.ok(m2pFacadeService.registerESignStatus(requestBody)));
  }

  /**
   * Registers the CKYC status callback.
   *
   * @param kycCallBackRequest The request body containing the CKYC status details.
   * @return A Mono wrapping a ResponseEntity containing another Mono that holds the result of the
   *     operation.
   */
  @PostMapping("/callback/ckyc-status")
  @ControllerEvent(event = Event.CKYC_CALLBACK)
  public Mono<ResponseEntity<Mono<?>>> registerCKycStatus(
      @SecureInput @RequestBody M2pKycCallBackWithAmlRequest kycCallBackRequest) {
    return Mono.just(ResponseEntity.ok(m2pFacadeService.registerCKycStatus(kycCallBackRequest)));
  }

  /**
   * Registers the manual KYC status callback.
   *
   * @param kycCallBackRequest The request body containing the manual KYC status details.
   * @return A Mono wrapping a ResponseEntity containing another Mono that holds the result of the
   *     operation.
   */
  @PostMapping("/callback/manual-kyc-status")
  @ControllerEvent(event = Event.MANUAL_KYC_CALLBACK)
  public Mono<ResponseEntity<Mono<?>>> registerManualKycStatus(
      @SecureInput @RequestBody M2pKycCallBackWithAmlRequest kycCallBackRequest) {
    return Mono.just(
        ResponseEntity.ok(m2pFacadeService.registerManualKycStatus(kycCallBackRequest)));
  }

  /**
   * Registers the rejection status callback.
   *
   * @param callbackRequest The request body containing the rejection status details.
   * @return A Mono wrapping a ResponseEntity containing another Mono that holds the result of the
   *     operation.
   */
  @PostMapping("/callback/rejection-status")
  @ControllerEvent(event = Event.REJECTION_CALLBACK)
  public Mono<ResponseEntity<Mono<?>>> registerRejectionStatus(
      @SecureInput @RequestBody M2pKycCallBackWithAmlRequest callbackRequest) {
    return Mono.just(ResponseEntity.ok(m2pFacadeService.registerRejectionStatus(callbackRequest)));
  }

  /**
   * Registers the FI status callback.
   *
   * @param callbackRequest The request body containing the FI status details.
   * @return A Mono wrapping a ResponseEntity containing another Mono that holds the result of the
   *     operation.
   */
  @PostMapping("/callback/fi-status")
  @ControllerEvent(event = Event.FI_CALLBACK)
  public Mono<ResponseEntity<Mono<?>>> registerFiStatus(
      @SecureInput @RequestBody M2pKycCallBackWithAmlRequest callbackRequest) {
    return Mono.just(ResponseEntity.ok(m2pFacadeService.registerFiStatus(callbackRequest)));
  }

  /**
   * Registers the FI status callback.
   *
   * @param callbackRequest The request body containing the FI status details.
   * @return A Mono wrapping a ResponseEntity containing another Mono that holds the result of the
   *     operation.
   */
  @PostMapping("/callback/ckycr-status")
  @ControllerEvent(event = Event.CKYCR_CALLBACK)
  public Mono<ResponseEntity<Mono<?>>> registerCkycrStatus(
      @SecureInput @RequestBody M2pCkycrCallbackRequest callbackRequest) {
    return Mono.just(ResponseEntity.ok(m2pFacadeService.registerCkycrStatus(callbackRequest)));
  }

  /**
   * Registers the loan approval status callback.
   *
   * @param loanApprovalCallBackRequest The request body containing the loan approval status
   *     details.
   * @return A Mono wrapping a ResponseEntity containing another Mono that holds the result of the
   *     operation.
   */
  @PostMapping("/callback/loan-approval-status")
  public Mono<ResponseEntity<Mono<?>>> registerLoanApprovalStatus(
      @SecureInput @RequestBody M2pLoanApprovalCallBackRequest loanApprovalCallBackRequest) {
    return Mono.just(
        ResponseEntity.ok(
            m2pFacadeService.registerLoanApprovalStatus(loanApprovalCallBackRequest)));
  }

  /**
   * Registers the loan closure status callback.
   *
   * @param requestBody The request body containing the loan closure status details.
   * @return A Mono wrapping a ResponseEntity containing the result of the operation.
   */
  @PostMapping("/callback/loan-closure-status")
  public Mono<ResponseEntity<?>> registerLoanClosureStatus(
      @SecureInput @RequestBody M2pLoanClosureCallBackRequest requestBody) {
    return Mono.deferContextual(
        ctx -> {
          m2pFacadeService
              .decrementPortfolioRiskParameters(requestBody)
              .subscribeOn(Schedulers.boundedElastic())
              .contextWrite(Context.of(ctx))
              .subscribe();
          return Mono.just(
              ResponseEntity.ok(m2pFacadeService.registerLoanClosureStatus(requestBody)));
        });
  }

  /**
   * Registers the credit line status callback. On SUCCESS status, generates, approves, and
   * activates the credit line, then sends callback to partner.
   *
   * @param requestBody The request body containing the credit line status details.
   * @return A Mono wrapping a ResponseEntity containing the result of the operation.
   */
  @PostMapping("/callback/credit-line-status")
  @ControllerEvent(event = Event.CREDIT_LINE_STATUS_CALLBACK)
  public Mono<ResponseEntity<Mono<?>>> registerCreditLineStatusCallback(
      @SecureInput @RequestBody CreditLineStatusCallbackRequest requestBody) {
    return Mono.just(
        ResponseEntity.ok(
            creditLineService.processCreditLineStatusCallback(
                requestBody, requestBody.getProductkey())));
  }
}
