package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.BRE_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.BRE_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.RiskServiceApi;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.model.dto.BreDTO;
import com.trillionloans.los.model.dto.BreRequest;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.response.m2p.AddressFetchResponse;
import com.trillionloans.los.service.db.PartnerMasterService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@RequiredArgsConstructor
@Service
@Slf4j
public class ClientFacadeService {

  private final M2PWrapperApi m2PWrapperApi;
  private final BreProcessService breProcessService;
  private final ProductConfigMasterService productConfigMasterService;
  private final RiskServiceApi riskServiceApi;
  private final Environment environment;
  private final PartnerMasterService partnerMasterService;
  private static final String BRE_FAIL = "bre data fetch failed";

  public Mono<?> registerBreByProduct(BreDTO requestBody, String productCode) {
    return triggerProductControlFlow(requestBody, productCode, BRE_IDENTIFIER);
  }

  public <T> Mono<?> triggerProductControlFlow(
      T requestBody, String productCode, String flowIdentifier) {

    // fetching product configuration from database based on product code
    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);
    return productConfigTuple.flatMap(
        productControlConfigData -> {

          // extracting data from product configuration
          String partnerCode = productControlConfigData.getT1();
          ProductControl.Flow breFlowData =
              productConfigMasterService.getFlowFromProductConfig(
                  productControlConfigData.getT2(), flowIdentifier);

          ProductControl.Flow breCallbackFlowData =
              productConfigMasterService.getFlowFromProductConfig(
                  productControlConfigData.getT2(), BRE_CALLBACK_IDENTIFIER);

          List<ProductControl.Flow> breFlows = new ArrayList<>();
          breFlows.add(breFlowData);
          breFlows.add(breCallbackFlowData);

          // npe checks for product configuration data
          if (Objects.isNull(breFlowData)) {
            return Mono.error(
                new BaseException(
                    SOMETHING_WENT_WRONG_CONFIG,
                    SOMETHING_WENT_WRONG_CONFIG,
                    HttpStatus.INTERNAL_SERVER_ERROR));
          }

          // extracting parameters for driving the callback-cta flow for partners
          String functionName = breFlowData.getFunctionName();
          try {
            // trying reflection api for holding the method
            // based on the function name found in product configuration
            Method method = getMethod(requestBody, functionName);

            // invoking the method for callback-cta flow
            return (Mono<?>) method.invoke(this, requestBody, productCode, partnerCode, breFlows);
          } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return Mono.error(e);
          }
        });
  }

  private <T> Method getMethod(T requestBody, String functionName) throws NoSuchMethodException {
    Class<?> requestBodyClass = requestBody.getClass();
    if (requestBodyClass == LinkedHashMap.class) {
      requestBodyClass = Object.class;
    }
    return this.getClass()
        .getMethod(functionName, requestBodyClass, String.class, String.class, List.class);
  }

  public Mono<?> registerSyncBre(
      BreDTO breDTO, String productCode, String partnerCode, ProductControl.Flow flowData) {
    return breProcessService.postBreDataML(
        breDTO.getRequestBody(), breDTO.getLoanId(), productCode);
  }

  public Mono<?> registerAsyncBre(
      BreDTO breDTO, String productCode, String partnerCode, List<ProductControl.Flow> breFlows) {

    ProductControl.Flow flowData =
        breFlows.stream()
            .filter(flow -> flow.getIdentifier().equals(BRE_IDENTIFIER))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Flow data not found"));

    boolean riskServiceFlagAtProductLevel =
        flowData.getConditions() != null
            && flowData.getConditions().containsKey("riskServiceFlag")
            && Boolean.TRUE.equals(flowData.getConditions().get("riskServiceFlag"));

    // if risk service is enabled, call the risk service api
    if ("enable".equals(environment.getProperty("risk.service")) && riskServiceFlagAtProductLevel) {
      log.info("Risk service is enabled, calling risk service API");
      // create a BreRequest object
      BreRequest breRequest =
          BreRequest.builder()
              .requestBody(breDTO.getRequestBody())
              .partnerCode(partnerCode)
              .productCode(productCode)
              .breFlows(breFlows)
              .build();

      return partnerMasterService
          .findByProductCode(productCode)
          .switchIfEmpty(
              Mono.error(
                  new NotFoundException("Partner not found for product code: " + productCode)))
          .flatMap(
              partnerMasterEntity ->
                  riskServiceApi.registerAsyncBre(
                      breDTO.getLoanId(), breRequest, partnerMasterEntity.getPartnerId()));
    }

    return breProcessService.postBreDataProductWise(
        breDTO.getRequestBody(), breDTO.getLoanId(), productCode, flowData, partnerCode);
  }

  public Flux<AddressFetchResponse> fetchClientAddresses(String clientId) {
    return m2PWrapperApi.fetchClientAddresses(clientId);
  }
}
