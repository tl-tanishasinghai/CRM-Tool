package com.trillionloans.los.service.disbursal;

import static com.trillionloans.los.constant.StringConstants.AUTO_DISB_CALLBACK_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;
import static com.trillionloans.los.constant.StringConstants.STATUS;
import static com.trillionloans.los.constant.StringConstants.SUCCESS;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;

import com.google.gson.Gson;
import com.trillionloans.los.constant.DisbursalStatus;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.CallbackLogEntity;
import com.trillionloans.los.model.request.AutoDisbursalCallbackRequest;
import com.trillionloans.los.service.db.CallbackStoreService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import io.r2dbc.postgresql.codec.Json;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

/**
 * Service to manage disbursal workflows, including auto disbursement and partner callbacks.
 * Integrates with multiple product configurations and handles dynamic flow invocation.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class DisbursalFacadeService {

  private final CallbackStoreService callbackStoreService;
  private final ProductConfigMasterService productConfigMasterService;
  private final DisbursalService disbursalService;
  private final Gson gson;

  /**
   * Triggers the registration of an auto disbursement callback.
   *
   * @param requestBody The request payload for the callback.
   * @param productCode The product identifier.
   * @return A reactive Mono indicating success or failure.
   */
  public Mono<?> registerAutoDisbursementStatus(
      AutoDisbursalCallbackRequest requestBody, String productCode) {
    return triggerProductControlFlow(
        requestBody, productCode, AUTO_DISB_CALLBACK_IDENTIFIER, false);
  }

  /**
   * Dynamically invokes the appropriate method based on product control flow configuration.
   *
   * @param requestBody The request payload for the operation.
   * @param productCode The product identifier.
   * @param flowIdentifier The flow identifier from product configuration.
   * @param isRetry Indicates if this is a retry operation.
   * @param <T> Generic type of the request payload.
   * @return A reactive Mono indicating success or failure.
   */
  public <T> Mono<?> triggerProductControlFlow(
      T requestBody, String productCode, String flowIdentifier, Boolean isRetry) {

    // fetch product configuration
    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);
    return productConfigTuple.flatMap(
        productControlConfigData -> {
          String partnerCode = productControlConfigData.getT1();
          ProductControl.Flow flowData =
              productConfigMasterService.getFlowFromProductConfig(
                  productControlConfigData.getT2(), flowIdentifier);

          if (Objects.isNull(flowData)) {
            return Mono.error(
                new BaseException(
                    SOMETHING_WENT_WRONG_CONFIG,
                    SOMETHING_WENT_WRONG_CONFIG,
                    HttpStatus.INTERNAL_SERVER_ERROR));
          }

          // use reflection to invoke the method for the flow
          String functionName = flowData.getFunctionName();
          try {
            Method method = getMethod(requestBody, functionName);
            return (Mono<?>)
                method.invoke(this, requestBody, productCode, partnerCode, flowData, isRetry);
          } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            return Mono.error(e);
          }
        });
  }

  /**
   * Retrieves the method to invoke dynamically using reflection.
   *
   * @param requestBody The request payload.
   * @param functionName The name of the method to invoke.
   * @param <T> Generic type of the request payload.
   * @return The reflected method.
   * @throws NoSuchMethodException If the method is not found.
   */
  private <T> Method getMethod(T requestBody, String functionName) throws NoSuchMethodException {
    Class<?> requestBodyClass = requestBody.getClass();
    if (requestBodyClass == LinkedHashMap.class) {
      requestBodyClass = Object.class;
    }
    return this.getClass()
        .getMethod(
            functionName,
            requestBodyClass,
            String.class,
            String.class,
            ProductControl.Flow.class,
            Boolean.class);
  }

  /**
   * Registers the status of an auto disbursement callback to a partner without M2P CTA flow.
   *
   * @param requestBody The request payload for the callback.
   * @param productCode The product identifier.
   * @param partnerCode The partner identifier.
   * @param flowData The product control flow configuration.
   * @param isRetry Indicates if this is a retry operation.
   * @return A Mono containing the success status map.
   */
  public Mono<Map<String, String>> registerAutoDisbursementStatusToPartnerWithoutM2pCta(
      AutoDisbursalCallbackRequest requestBody,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData,
      Boolean isRetry) {

    CallbackLogEntity callback =
        CallbackLogEntity.builder()
            .createdAt(LocalDateTime.now())
            .type(flowData.getIdentifier())
            .isRetry(isRetry)
            .request(Json.of(gson.toJson(requestBody)))
            .productCode(productCode)
            .referenceId(requestBody.getSystemExternalId())
            .response(Json.of(gson.toJson(Map.of(STATUS, SUCCESS))))
            .build();

    return callbackStoreService
        .save(callback)
        .then(
            Mono.deferContextual(
                context -> {
                  String status = requestBody.getStatus();
                  if ("FAILED".equalsIgnoreCase(status)) {
                    log.info(
                        "[AUTO_DISB_V2] auto disbursement failed for loan application id: {},"
                            + " initiating manual disbursement",
                        requestBody.getSystemExternalId());
                    disbursalService
                        .processFailedAutoDisbursal(requestBody.getSystemExternalId(), productCode)
                        .subscribeOn(Schedulers.parallel())
                        .contextWrite(
                            ctx ->
                                ctx.put(TRACE_ID, context.get(TRACE_ID))
                                    .put(PARTNER_ID, context.get(PARTNER_ID)))
                        .subscribe();
                  } else {
                    log.info(
                        "[AUTO_DISB_V2] auto disbursement succeeded for loan application id: {}."
                            + " updating transaction, marking loan as disbursed, and notifying"
                            + " partner",
                        requestBody.getSystemExternalId());
                    disbursalService
                        .updateTransactionStatusInDisbursalRegistry(
                            requestBody.getSystemExternalId(),
                            DisbursalStatus.SUCCESS,
                            "AUTO",
                            productCode,
                            null,
                            null,
                            null)
                        .then(
                            disbursalService.markLoanDisbursed(
                                requestBody, requestBody.getSystemExternalId(), productCode))
                        .subscribeOn(Schedulers.parallel())
                        .contextWrite(
                            ctx ->
                                ctx.put(TRACE_ID, context.get(TRACE_ID))
                                    .put(PARTNER_ID, context.get(PARTNER_ID)))
                        .subscribe();
                  }
                  return Mono.just(Map.of(STATUS, SUCCESS));
                }));
  }
}
