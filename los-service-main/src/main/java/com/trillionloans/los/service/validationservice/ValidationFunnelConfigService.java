package com.trillionloans.los.service.validationservice;

import static com.trillionloans.los.constant.StringConstants.LOAN_CREATE_CTA_IDENTIFIER;

import com.trillionloans.los.config.ValidationFunnelConfiguration;
import com.trillionloans.los.config.ValidationFunnelProperties;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@Slf4j
@Component
@AllArgsConstructor
public class ValidationFunnelConfigService {
  private final ValidationFunnelProperties properties;
  private final ProductConfigMasterService productConfigMasterService;

  public Mono<Boolean> isDobWaterfallFunnelActiveMono(String productCode) {
    return getValidationFunnelConfig(productCode)
        .map(
            config ->
                properties.isMasterFlag()
                    && config != null
                    && config.isValidationFunnelFlagEnabled()
                    && config.isDobWaterfallFunnelFeatureFlagEnabled())
        .defaultIfEmpty(false);
  }

  public Mono<Boolean> isValidationFunnelActiveAndRejectionIsOn(String productCode) {
    return getValidationFunnelConfig(productCode)
        .map(
            config -> {
              boolean funnelFeatureFlagWithRejectionOn =
                  Objects.nonNull(config)
                      && config.isValidationFunnelFlagEnabled()
                      && config.isEnableValidationFunnelKycRejection();
              return properties.isMasterFlag() && funnelFeatureFlagWithRejectionOn;
            });
  }

  public Mono<Boolean> isValidationFunnelActive(String productCode) {
    return getValidationFunnelConfig(productCode)
        .map(
            config -> {
              boolean funnelFeatureFlag =
                  Objects.nonNull(config) && config.isValidationFunnelFlagEnabled();
              return properties.isMasterFlag() && funnelFeatureFlag;
            });
  }

  public Mono<ValidationFunnelConfiguration> getValidationFunnelConfig(String productCode) {
    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);

    return productConfigTuple
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow loanCreateFlowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), LOAN_CREATE_CTA_IDENTIFIER);

              if (Objects.isNull(loanCreateFlowData)
                  || Objects.isNull(loanCreateFlowData.getValidationFunnelConfiguration())) {

                log.info(
                    "[VALIDATION_SERVICE] No flow data found for VALIDATION_SERVICE. Returning"
                        + " default disabled config.");
                return Mono.just(buildDefaultDisabledConfig());
              }

              return Mono.just(loanCreateFlowData.getValidationFunnelConfiguration());
            })
        .onErrorResume(
            ex -> {
              log.error(
                  "[VALIDATION_SERVICE] Error while fetching config. Returning default disabled"
                      + " config.",
                  ex);
              return Mono.just(buildDefaultDisabledConfig());
            });
  }

  private ValidationFunnelConfiguration buildDefaultDisabledConfig() {

    ValidationFunnelConfiguration.NsdlPanValidationConfig nsdlConfig =
        ValidationFunnelConfiguration.NsdlPanValidationConfig.builder()
            .panValidationFeatureFlag(false)
            .panStatusCheckEnabled(false)
            .panStatusIsCritical(true)
            .nameMatchCheckEnabled(false)
            .nameMatchIsCritical(false)
            .dobMatchCheckEnabled(false)
            .dobMatchIsCritical(false)
            .panStatusExpected("E")
            .nameMatchStatusExpected(null)
            .dobMatchStatusExpected(null)
            .seedingStatusCheckEnabled(false)
            .seedingStatusExpectedValue(null)
            .seedingMatchIsCritical(false)
            .build();

    ValidationFunnelConfiguration.KarzaPanValidationConfig karzaConfig =
        ValidationFunnelConfiguration.KarzaPanValidationConfig.builder()
            .karzaPanValidationFeatureFlag(false)
            .build();

    ValidationFunnelConfiguration.KarzaNameSimilarityConfig nameSimConfig =
        ValidationFunnelConfiguration.KarzaNameSimilarityConfig.builder()
            .karzaNameSimilarityFeatureFlag(false)
            .score(0)
            .allowPartialMatch(false)
            .suppressReorderPenalty(false)
            .build();

    ValidationFunnelConfiguration.KarzaPanAadharLinkageConfig karzaLinkConfig =
        ValidationFunnelConfiguration.KarzaPanAadharLinkageConfig.builder()
            .karzaPanAadharLinkageFeatureFlag(false)
            .build();

    return ValidationFunnelConfiguration.builder()
        .validationFunnelFlagEnabled(false)
        .nsdlPanValidationConfig(nsdlConfig)
        .karzaPanValidationConfig(karzaConfig)
        .karzaNameSimilarityConfig(nameSimConfig)
        .karzaPanAadharLinkageConfig(karzaLinkConfig)
        .dobWaterfallFunnelFeatureFlagEnabled(false)
        .enableValidationFunnelKycRejection(false)
        .build();
  }
}
