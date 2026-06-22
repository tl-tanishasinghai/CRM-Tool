package com.trillionloans.lms.service;

import static com.trillionloans.lms.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;

import com.google.gson.Gson;
import com.trillionloans.lms.api.partner.PartnerApi;
import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.model.dto.internal.ProductControl;
import com.trillionloans.lms.service.db.ProductConfigMasterService;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@AllArgsConstructor
@Service
public class FacadeService {
  private final PartnerApi partnerApi;
  private final ProductConfigMasterService productConfigMasterService;
  private final Gson gson;

  public <T> Mono<?> triggerProductControlFlow(
      T requestBody, String productCode, String flowIdentifier) {

    // fetching product configuration from database based on product code
    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);
    return productConfigTuple.flatMap(
        productControlConfigData -> {

          // extracting data from product configuration
          String partnerCode = productControlConfigData.getT1();
          ProductControl.Flow flowData =
              productConfigMasterService.getFlowFromProductConfig(
                  productControlConfigData.getT2(), flowIdentifier);

          // npe checks for product configuration data
          if (Objects.isNull(flowData)) {
            return Mono.error(
                new BaseException(
                    SOMETHING_WENT_WRONG_CONFIG,
                    SOMETHING_WENT_WRONG_CONFIG,
                    HttpStatus.INTERNAL_SERVER_ERROR));
          }

          // extracting parameters for driving the callback-cta flow for partners
          String functionName = flowData.getFunctionName();
          try {
            // trying reflection api for holding the method
            // based on the function name found in product configuration
            Method method = getMethod(requestBody, functionName);

            // invoking the method for callback-cta flow
            return (Mono<?>) method.invoke(this, requestBody, productCode, partnerCode, flowData);
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
        .getMethod(
            functionName, requestBodyClass, String.class, String.class, ProductControl.Flow.class);
  }
}
