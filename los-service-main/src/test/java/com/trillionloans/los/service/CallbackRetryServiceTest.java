package com.trillionloans.los.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.trillionloans.los.model.dto.internal.CallBackLog;
import com.trillionloans.los.model.request.CallbackSearchCriteria;
import com.trillionloans.los.service.db.CallbackStoreService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {CallbackRetryService.class, Gson.class})
@ExtendWith(SpringExtension.class)
class CallbackRetryServiceTest {
  @MockBean private CallbackStoreService callbackStoreService;
  @MockBean private M2pFacadeService m2pFacadeService;

  @Autowired private Gson gson;

  @Autowired private CallbackRetryService callbackRetryService;

  private CallbackSearchCriteria criteria;

  @BeforeEach
  public void setUp() {
    criteria = new CallbackSearchCriteria(); // Set up your criteria here
    criteria.setReferenceIds(List.of("ref-123"));
  }

  @Test
  void testRetryCallbacks_noLogsFound() {
    when(callbackStoreService.findAllByCriteria(criteria)).thenReturn(Flux.empty());

    StepVerifier.create(callbackRetryService.retryCallbacks(criteria))
        .expectNext(Collections.emptyList()) // Expecting an empty list
        .verifyComplete();

    verify(callbackStoreService, times(1)).findAllByCriteria(criteria);
  }

  @Test
  void testRetryCallbacks_requestNull() {
    CallBackLog callbackLog = new CallBackLog();
    callbackLog.setRequest(null);
    callbackLog.setType("DISB_CALLBACK_IDENTIFIER");
    callbackLog.setReferenceId("refId3");

    when(callbackStoreService.findAllByCriteria(criteria)).thenReturn(Flux.just(callbackLog));

    StepVerifier.create(callbackRetryService.retryCallbacks(criteria))
        .expectNext(
            List.of("refId3 - not valid - failed - DISB_CALLBACK_IDENTIFIER")) // Expecting a list
        .verifyComplete();

    verify(callbackStoreService, times(1)).findAllByCriteria(criteria);
    verify(m2pFacadeService, never()).triggerProductControlFlow(any(), any(), any(), any());
  }

  @Test
  void testRetryCallbacks_disbCallback_failure() {
    CallBackLog callbackLog = new CallBackLog();
    callbackLog.setRequest("{\"productCode\":\"1234\", \"loanApplicationId\":\"loanId3\"}");
    callbackLog.setType("DISB_CALLBACK_IDENTIFIER");
    callbackLog.setProductCode("1234");
    callbackLog.setReferenceId("refId3");

    when(callbackStoreService.findAllByCriteria(criteria)).thenReturn(Flux.just(callbackLog));
    when(m2pFacadeService.triggerProductControlFlow(
            any(), any(), eq("DISB_CALLBACK_IDENTIFIER"), any()))
        .thenReturn(Mono.error(new RuntimeException("Service Error")));

    StepVerifier.create(callbackRetryService.retryCallbacks(criteria))
        .expectNext(List.of("refId3 - not valid - failed - DISB_CALLBACK_IDENTIFIER"))
        .verifyComplete();

    verify(callbackStoreService, times(1)).findAllByCriteria(criteria);
  }

  @Test
  void testRetryCallbacks_noCriteriaMatch() {
    when(callbackStoreService.findAllByCriteria(criteria)).thenReturn(Flux.empty());

    StepVerifier.create(callbackRetryService.retryCallbacks(criteria))
        .expectNext(Collections.emptyList()) // Expecting an empty list
        .verifyComplete();

    verify(callbackStoreService, times(1)).findAllByCriteria(criteria);
  }
}
