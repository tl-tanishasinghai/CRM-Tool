package com.trillionloans.los.service.db;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.trillionloans.los.model.entity.CallbackLogEntity;
import com.trillionloans.los.model.request.CallbackSearchCriteria;
import com.trillionloans.los.repository.CallbackRepository;
import java.util.Collections;
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

@ContextConfiguration(classes = {CallbackStoreService.class, Gson.class})
@ExtendWith(SpringExtension.class)
class CallbackStoreServiceTest {
  @MockBean private CallbackRepository callbackRepository;

  @Autowired private CallbackStoreService callbackStoreService;

  @BeforeEach
  void setUp() {
    callbackStoreService = new CallbackStoreService(callbackRepository);
  }

  @Test
  void testSaveCallback() {
    CallbackLogEntity callbackLog = new CallbackLogEntity();
    callbackLog.setId(1L);
    // Set other properties as needed

    when(callbackRepository.save(any())).thenReturn(Mono.just(callbackLog));

    StepVerifier.create(callbackStoreService.save(callbackLog))
        .expectNext(callbackLog)
        .verifyComplete();

    verify(callbackRepository, times(1)).save(callbackLog);
  }

  @Test
  void testFindAllByCriteria_noResults() {
    CallbackSearchCriteria criteria = new CallbackSearchCriteria();
    criteria.setProductCodes(Collections.singletonList("prod123"));

    when(callbackRepository.findAllByCriteria(any(), any(), any(), any(), any(), any(), any()))
        .thenReturn(Flux.empty());

    StepVerifier.create(callbackStoreService.findAllByCriteria(criteria))
        .expectNextCount(0)
        .verifyComplete();

    verify(callbackRepository, times(1))
        .findAllByCriteria(any(), any(), any(), any(), any(), any(), any());
  }
}
