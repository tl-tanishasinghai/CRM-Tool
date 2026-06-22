package com.trillionloans.lms.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.trillionloans.lms.api.partner.PartnerApi;
import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.model.dto.internal.ProductControl;
import com.trillionloans.lms.service.db.ProductConfigMasterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

@ContextConfiguration(classes = {FacadeService.class, Gson.class})
@ExtendWith(SpringExtension.class)
class FacadeServiceTest {
  @Autowired private FacadeService facadeService;

  @MockBean private PartnerApi partnerApi;

  @MockBean private ProductConfigMasterService productConfigMasterService;

  /** Method under test: {@link FacadeService#triggerProductControlFlow(Object, String, String)} */
  @Test
  void testTriggerProductControlFlow() {
    Mono<Tuple2<String, ProductControl>> justResult =
        Mono.<Tuple2<String, ProductControl>>just(mock(Tuple2.class));
    when(productConfigMasterService.getProductConfigMasterData(Mockito.<String>any()))
        .thenReturn(justResult);
    facadeService.triggerProductControlFlow("Request Body", "Product Code", "42");
    verify(productConfigMasterService).getProductConfigMasterData(Mockito.<String>any());
  }

  /** Method under test: {@link FacadeService#triggerProductControlFlow(Object, String, String)} */
  @Test
  void testTriggerProductControlFlow2() {
    when(productConfigMasterService.getProductConfigMasterData(Mockito.<String>any()))
        .thenThrow(new BaseException("An error occurred", "Client Response", null));
    assertThrows(
        BaseException.class,
        () -> facadeService.triggerProductControlFlow("Request Body", "Product Code", "42"));
    verify(productConfigMasterService).getProductConfigMasterData(Mockito.<String>any());
  }
}
