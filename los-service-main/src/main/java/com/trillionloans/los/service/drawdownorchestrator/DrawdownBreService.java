package com.trillionloans.los.service.drawdownorchestrator;

import static com.trillionloans.los.constant.StringConstants.BRE_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.DRAWDOWN_BRE;

import com.trillionloans.los.api.partner.RiskServiceApi;
import com.trillionloans.los.exception.drawdown.DrawdownValidationException;
import com.trillionloans.los.model.dto.BreRequest;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.request.DrawdownRequest;
import com.trillionloans.los.model.response.creditline.DrawdownBreResponse;
import com.trillionloans.los.repository.AnchorMasterRepository;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawdownBreService {

  private final ProductConfigMasterService productConfigMasterService;
  private final AnchorMasterRepository anchorMasterRepository;
  private final RiskServiceApi riskServiceApi;

  public Mono<DrawdownBreResponse> triggerDrawdownBre(
      DrawdownRequest request,
      String lineId,
      String clientId,
      String leadId,
      String partnerId,
      String productCode,
      String drawdownId) {

    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), BRE_IDENTIFIER);
              boolean mockDrawdownBreEnabled = DrawdownUtil.isMockDrawdownBreEnabled(flowData);

              if (mockDrawdownBreEnabled) {
                log.info(
                    "[{}] triggerDrawdownBre MOCK enabled by product config, skipping BRE. leadId:"
                        + " {}, lineId: {}",
                    DRAWDOWN_BRE,
                    leadId,
                    lineId);
                return Mono.<DrawdownBreResponse>empty();
              }

              return Mono.just(productControlConfigData);
            })
        .flatMap(
            unused -> {
              log.info(
                  "[{}] Initiating BRE for lineId: {} and anchorId: {}",
                  DRAWDOWN_BRE,
                  lineId,
                  request.getAnchorId());

              if (DrawdownUtil.PRODUCT_KCL.equalsIgnoreCase(StringUtils.trimToEmpty(productCode))) {
                log.info(
                    "[{}] Skipping anchor master lookup for productCode: {} and lineId: {}",
                    DRAWDOWN_BRE,
                    productCode,
                    lineId);
                return DrawdownUtil.mapToKclDrawdownBreRequest(
                        clientId, leadId, lineId, drawdownId, request)
                    .flatMap(breRequest -> executeBre(breRequest, lineId, partnerId, productCode))
                    .doOnError(
                        e ->
                            log.error(
                                "{} Error in BRE trigger flow: {}",
                                DRAWDOWN_BRE,
                                e.getMessage(),
                                e));
              }

              return anchorMasterRepository
                  .findByAnchorId(request.getAnchorId())
                  .switchIfEmpty(
                      Mono.error(
                          new DrawdownValidationException(
                              "LeadId or Anchor Master data not found for anchor: "
                                  + request.getAnchorId())))
                  .flatMap(
                      anchor -> {
                        log.info(
                            "[{}] Drawdown bre required data retrieved. LeadId: {}, anchorId: {}",
                            DRAWDOWN_BRE,
                            leadId,
                            request.getAnchorId());
                        return DrawdownUtil.mapToFundDrawdownBreRequest(
                            clientId, leadId, lineId, drawdownId, anchor, request);
                      })
                  .flatMap(breRequest -> executeBre(breRequest, lineId, partnerId, productCode))
                  .doOnError(
                      e ->
                          log.error(
                              "{} Error in BRE trigger flow: {}", DRAWDOWN_BRE, e.getMessage(), e));
            });
  }

  public Mono<DrawdownBreResponse> executeBre(
      Map<String, Object> breRequestBody, String lineId, String partnerId, String productCode) {
    log.info(
        "[{}] executeBre invoked for lineId: {}, partnerId: {}, productCode: {}",
        DRAWDOWN_BRE,
        lineId,
        partnerId,
        productCode);

    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), BRE_IDENTIFIER);

              if (DrawdownUtil.isMockDrawdownBreEnabled(flowData)) {
                log.info(
                    "[{}] executeBre skipped for lineId: {} (mockDrawdownBre enabled)",
                    DRAWDOWN_BRE,
                    lineId);
                return Mono.empty();
              }

              if (flowData == null) {
                log.warn(
                    "[{}] BRE flow configuration not found for productCode: {}, identifier: {}",
                    DRAWDOWN_BRE,
                    productCode,
                    BRE_IDENTIFIER);
                return Mono.error(
                    new DrawdownValidationException(
                        "BRE flow configuration not found for productCode: "
                            + productCode
                            + ", identifier: "
                            + BRE_IDENTIFIER));
              }

              BreRequest breRequest =
                  BreRequest.builder()
                      .requestBody(breRequestBody)
                      .partnerCode(partnerId)
                      .productCode(productCode)
                      .breFlows(Collections.singletonList(flowData))
                      .build();

              log.info("[{}] Calling risk service BRE API for lineId: {}", DRAWDOWN_BRE, lineId);
              return riskServiceApi.triggerDrawdownBre(lineId, breRequest, partnerId);
            })
        .doOnSuccess(
            response ->
                log.info(
                    "[{}] executeBre completed for lineId: {}, status: {}",
                    DRAWDOWN_BRE,
                    lineId,
                    response != null ? response.getStatus() : null))
        .doOnError(
            e ->
                log.error(
                    "[{}] executeBre failed for lineId: {}: {}",
                    DRAWDOWN_BRE,
                    lineId,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                    e));
  }
}
