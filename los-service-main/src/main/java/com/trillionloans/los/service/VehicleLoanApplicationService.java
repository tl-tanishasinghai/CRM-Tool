package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;
import static com.trillionloans.los.constant.StringConstants.VEHICLE_DETAILS_FAIL;
import static com.trillionloans.los.constant.StringConstants.VEHICLE_DETAILS_POST_CTA_IDENTIFIER;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.request.VehicleDetailsRequest;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.util.LoanDataUtil;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Service class for handling vehicle loan application related operations. */
@Service
@AllArgsConstructor
@Slf4j
public class VehicleLoanApplicationService {
  private final M2PWrapperApi m2PWrapperApi;
  private final ProductConfigMasterService productConfigMasterService;

  /**
   * Stamps vehicle details into the loan processing system at M2P's end.
   *
   * @param vehicleDetailsRequest The request containing vehicle details.
   * @param loanId The unique identifier for the loan.
   * @param productCode The product code associated with the loan product.
   * @return A Mono representing the completion of the vehicle details stamping process.
   */
  public Mono<?> stampVehicleDetails(
      VehicleDetailsRequest vehicleDetailsRequest, String loanId, String productCode) {
    return m2PWrapperApi
        .stampVehicleDetails(
            LoanDataUtil.getM2pVehicleDetailsRequestDTO(vehicleDetailsRequest), loanId)
        .onErrorResume(Mono::error)
        .flatMap(
            data -> {
              if (Objects.isNull(data)) {
                return Mono.error(
                    new BaseException(
                        VEHICLE_DETAILS_FAIL, null, HttpStatus.INTERNAL_SERVER_ERROR));
              }
              // checking for the product config for cta call with m2p
              return productConfigMasterService
                  .getProductConfigMasterData(productCode)
                  .flatMap(
                      productControlConfigData -> {
                        ProductControl.Flow flowData =
                            productConfigMasterService.getFlowFromProductConfig(
                                productControlConfigData.getT2(),
                                VEHICLE_DETAILS_POST_CTA_IDENTIFIER);
                        if (Objects.isNull(flowData)) {
                          return Mono.error(
                              new BaseException(
                                  SOMETHING_WENT_WRONG_CONFIG,
                                  SOMETHING_WENT_WRONG_CONFIG,
                                  HttpStatus.INTERNAL_SERVER_ERROR));
                        }
                        // checking for the boolean flag, if cta call is required
                        if (flowData.isCtaCallFlag()) {
                          return m2PWrapperApi
                              .registerCta(loanId, flowData.getCtaName())
                              .flatMap(ctaResponse -> Mono.just(data));
                        }
                        return Mono.just(data);
                      });
            });
  }
}
