package com.trillionloans.los.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.gson.Gson;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.request.VehicleDetailsRequest;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@ContextConfiguration(classes = {VehicleLoanApplicationService.class, Gson.class})
@ExtendWith(SpringExtension.class)
class VehicleLoanApplicationServiceTest {
  @Autowired private VehicleLoanApplicationService vehicleLoanApplicationService;
  @MockBean private M2PWrapperApi m2PWrapperApi;
  @MockBean private ProductConfigMasterService productConfigMasterService;

  @Test
  void testStampVehicleDetails() {
    VehicleDetailsRequest vehicleDetailsRequest = new VehicleDetailsRequest();
    Mono<Tuple2<String, ProductControl>> justResult =
        Mono.<Tuple2<String, ProductControl>>just(mock(Tuple2.class));
    Mono<?> m2pResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.stampVehicleDetails(Mockito.any(), Mockito.anyString()))
        .thenReturn(justResult);
    Mockito.<Mono<?>>when(m2PWrapperApi.registerCta(Mockito.any(), Mockito.anyString()))
        .thenReturn(m2pResult);
    vehicleLoanApplicationService.stampVehicleDetails(vehicleDetailsRequest, "567", "OTOVL");
    verify(m2PWrapperApi).stampVehicleDetails(Mockito.any(), Mockito.anyString());
  }

  @Test
  void testStampVehicleDetails_2() {
    VehicleDetailsRequest vehicleDetailsRequest = new VehicleDetailsRequest();
    Mono<?> m2pResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.stampVehicleDetails(Mockito.any(), Mockito.anyString()))
        .thenThrow(new ClientSideException("error", "error", HttpStatus.BAD_REQUEST));
    Mockito.<Mono<?>>when(m2PWrapperApi.registerCta(Mockito.any(), Mockito.anyString()))
        .thenReturn(m2pResult);
    assertThrows(
        ClientSideException.class,
        () ->
            vehicleLoanApplicationService.stampVehicleDetails(
                vehicleDetailsRequest, "42", "OTOVL"));
    verify(m2PWrapperApi).stampVehicleDetails(Mockito.any(), Mockito.<String>any());
  }
}
