package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.BRE;
import static com.trillionloans.los.constant.StringConstants.BRE_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.PRE_ACTV_QC;
import static com.trillionloans.los.constant.StringConstants.PRE_DISBURSAL_VALIDATION;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.QcCheckStatus;
import com.trillionloans.los.model.dto.QcCheckResult;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.db.QcCheckStoreService;
import io.netty.util.internal.StringUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class QcScfRuleService {
  private final M2PWrapperApi m2PWrapperApi;
  private final QcCheckStoreService qcCheckStoreService;
  private final ProductConfigMasterService productConfigMasterService;
  private final LoanApplicationService loanApplicationService;

  // method for limit qc for supply chain
  public Mono<QcCheckResult> processPreLimitActivationQcChecks(
      String leadId, String productCode, String limitId) {

    return m2PWrapperApi
        .getLoanApplicationByLoanIdV2(leadId, null)
        .flatMap(
            loan -> {
              if (loan.getClientId() == null) {
                log.error(
                    "could not resolve clientId from loan application for leadId: {}.", leadId);
                return Mono.just(QcCheckResult.rejected("Client id not found for lead"));
              }
              String clientId = String.valueOf(loan.getClientId());
              return productConfigMasterService
                  .getProductConfigMasterData(productCode)
                  .flatMap(
                      productControlData -> {
                        ProductControl productControl = productControlData.getT2();

                        ProductControl.Flow flowData =
                            productConfigMasterService.getFlowFromProductConfig(
                                productControl, BRE_IDENTIFIER);

                        if (flowData == null) {
                          log.warn(
                              "[{}] Flow data not found for identifier={}, productCode={}",
                              PRE_ACTV_QC,
                              BRE,
                              productCode);
                          return Mono.just(QcCheckResult.approved());
                        }

                        boolean isPanAadhaarLinkageEnabled =
                            flowData.getActivationValidationCondition() != null
                                && Boolean.TRUE.equals(
                                    flowData
                                        .getActivationValidationCondition()
                                        .get("validatePanAadhaarLinkage"));
                        boolean isValidationEnabled =
                            flowData.getActivationValidationCondition() != null
                                && Boolean.TRUE.equals(
                                    flowData
                                        .getActivationValidationCondition()
                                        .get("validateCheck"));

                        if (!isPanAadhaarLinkageEnabled && !isValidationEnabled) {
                          return Mono.just(QcCheckResult.approved());
                        }

                        Mono<QcCheckResult> panAadhaarMono =
                            panAadhaarLinkageQcMono(
                                leadId, productCode, clientId, isPanAadhaarLinkageEnabled);
                        Mono<QcCheckResult> activationMono =
                            activationValidationQcMono(
                                leadId, productCode, limitId, clientId, isValidationEnabled);

                        return Mono.zip(
                            panAadhaarMono, activationMono, this::mergePreActivationQcResults);
                      });
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.error(
                      "could not find existing loan application data for leadId: {}.", leadId);
                  return Mono.just(QcCheckResult.rejected("Loan application not found"));
                }));
  }

  private QcCheckResult mergePreActivationQcResults(QcCheckResult pan, QcCheckResult activation) {
    if (pan.getStatus() == QcCheckStatus.REJECTED) {
      return pan;
    }
    if (activation.getStatus() == QcCheckStatus.REJECTED) {
      return activation;
    }
    return QcCheckResult.approved();
  }

  private Mono<QcCheckResult> panAadhaarLinkageQcMono(
      String leadId, String productCode, String clientId, boolean enabled) {
    if (!enabled) {
      return Mono.just(QcCheckResult.approved());
    }
    return loanApplicationService
        .getPanAadhaarLinkageWithClientIdForPreActivationQc(productCode, leadId)
        .flatMap(
            linkage -> {
              if (linkage.linked()) {
                return Mono.just(QcCheckResult.approved());
              }
              String breachClientId = linkage.clientId() != null ? linkage.clientId() : clientId;
              log.warn(
                  "[{}] Pan-Aadhaar not linked for leadId: {}, clientId: {}, productCode: {}",
                  PRE_ACTV_QC,
                  leadId,
                  breachClientId,
                  productCode);
              qcCheckStoreService
                  .asyncSaveBreach(
                      leadId,
                      breachClientId,
                      "Pan-Aadhaar-Not-Linked",
                      null,
                      null,
                      PRE_ACTV_QC,
                      productCode)
                  .subscribe();
              return Mono.just(QcCheckResult.rejected("Pan-Aadhaar linkage check failed"));
            })
        .onErrorResume(
            err -> {
              log.error(
                  "[{}] Pan-Aadhaar linkage check failed for leadId: {}, productCode: {}",
                  PRE_ACTV_QC,
                  leadId,
                  productCode,
                  err);
              qcCheckStoreService
                  .asyncSaveBreach(
                      leadId,
                      clientId,
                      "Pan-Aadhaar-Not-Linked",
                      null,
                      null,
                      PRE_ACTV_QC,
                      productCode)
                  .subscribe();
              return Mono.just(QcCheckResult.rejected("Pan-Aadhaar linkage check failed"));
            });
  }

  private Mono<QcCheckResult> activationValidationQcMono(
      String leadId, String productCode, String limitId, String clientId, boolean enabled) {
    if (!enabled) {
      return Mono.just(QcCheckResult.approved());
    }
    return m2PWrapperApi
        .getActivationCheckData(leadId)
        .flatMap(
            activationChecksData -> {
              String breachClientId =
                  activationChecksData.getClientId() != null
                      ? activationChecksData.getClientId()
                      : clientId;
              if (Boolean.TRUE.equals(activationChecksData.getHasLoan())) {
                log.error(
                    "[{}] [QC_BREACH] loan already exist in disbursed initiated or active state,"
                        + " lead id: {}, client id: {}",
                    PRE_DISBURSAL_VALIDATION,
                    leadId,
                    breachClientId);

                qcCheckStoreService
                    .asyncSaveBreach(
                        leadId,
                        breachClientId,
                        "hasActiveLoan",
                        null,
                        null,
                        PRE_ACTV_QC,
                        productCode)
                    .subscribe();

                return Mono.just(QcCheckResult.rejected("has Loan check failed"));
              }
              if ((limitId != null && !limitId.equals(activationChecksData.getActiveLimitId()))
                  || !StringUtil.isNullOrEmpty(activationChecksData.getActiveLimitId())) {
                log.error(
                    "[{}] [QC_BREACH] limit already exist in disbursed initiated or active state"
                        + " for another limitId, {}, client id: {}",
                    PRE_DISBURSAL_VALIDATION,
                    limitId,
                    breachClientId);

                qcCheckStoreService
                    .asyncSaveBreach(
                        leadId,
                        breachClientId,
                        "hasActiveLimit",
                        null,
                        null,
                        PRE_ACTV_QC,
                        productCode)
                    .subscribe();

                return Mono.just(QcCheckResult.rejected("has Limit check failed"));
              }

              return Mono.just(QcCheckResult.approved());
            })
        .defaultIfEmpty(QcCheckResult.approved());
  }
}
