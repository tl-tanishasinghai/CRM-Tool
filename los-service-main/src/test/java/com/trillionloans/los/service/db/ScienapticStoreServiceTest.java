package com.trillionloans.los.service.db;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.los.model.entity.ScienapticEntity;
import com.trillionloans.los.repository.ScienapticStoreRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

@ContextConfiguration(classes = {ScienapticStoreService.class})
@ExtendWith(SpringExtension.class)
@DisabledInAotMode
class ScienapticStoreServiceTest {
  @MockBean private ScienapticStoreRepository scienapticStoreRepository;

  @Autowired private ScienapticStoreService scienapticStoreService;

  /** Method under test: {@link ScienapticStoreService#save(ScienapticEntity)} */
  @Test
  void testSave() {
    // Arrange
    ScienapticEntity.ScienapticEntityBuilder breTypeResult =
        ScienapticEntity.builder().breType("SANCTION");
    ScienapticEntity buildResult =
        breTypeResult
            .createdAt(LocalDate.of(2024, 9, 24).atStartOfDay())
            .externalId("42")
            .id(1L)
            .request(null)
            .response(null)
            .build();
    Mono<ScienapticEntity> justResult = Mono.just(buildResult);
    when(scienapticStoreRepository.save(Mockito.<ScienapticEntity>any())).thenReturn(justResult);

    // Act
    Mono<ScienapticEntity> actualSaveResult = scienapticStoreService.save(new ScienapticEntity());

    // Assert
    verify(scienapticStoreRepository).save(isA(ScienapticEntity.class));
    assertSame(justResult, actualSaveResult);
  }
}
