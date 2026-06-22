package com.trillionloans.los.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {CaffeineRepository.class})
@ExtendWith(SpringExtension.class)
@DisabledInAotMode
class CaffeineRepositoryTest {
  @MockBean private Cache<String, Object> cache;

  @Autowired private CaffeineRepository caffeineRepository;

  /** Method under test: {@link CaffeineRepository#getKey(String)} */
  @Test
  void testGetKey() throws AssertionError {
    when(cache.getIfPresent(Mockito.<String>any())).thenReturn("If Present");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(caffeineRepository.getKey("Key"));
    createResult
        .assertNext(
            o -> {
              assertEquals("If Present", o);
              return;
            })
        .expectComplete()
        .verify();
    verify(cache).getIfPresent(eq("Key"));
  }

  /** Method under test: {@link CaffeineRepository#putKey(String, Object)} */
  @Test
  void testPutKey() {
    doNothing().when(cache).put(Mockito.<String>any(), Mockito.<Object>any());

    caffeineRepository.putKey("Key", "Value");

    verify(cache).put(eq("Key"), isA(Object.class));
  }

  /** Method under test: {@link CaffeineRepository#removeKey(String)} */
  @Test
  void testRemoveKey() {
    doNothing().when(cache).invalidate(Mockito.<String>any());

    caffeineRepository.removeKey("Key");

    verify(cache).invalidate(eq("Key"));
  }

  /** Method under test: {@link CaffeineRepository#removeAll()} */
  @Test
  void testRemoveAll() {
    doNothing().when(cache).invalidateAll();

    caffeineRepository.removeAll();

    verify(cache).invalidateAll();
  }
}
