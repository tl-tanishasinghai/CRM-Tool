package com.trillionloans.los.service.db;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;

import com.trillionloans.los.model.entity.QcCheckEntity;
import com.trillionloans.los.repository.QcCheckRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class QcCheckStoreService {
  private final QcCheckRepository qcCheckRepository;

  public Mono<QcCheckEntity> asyncSaveBreach(
      String loanApplicationId,
      String clientId,
      String field,
      Object breValue,
      Object loanValue,
      String checkType,
      String productCode) {
    QcCheckEntity entity =
        QcCheckEntity.builder()
            .loanApplicationId(loanApplicationId)
            .clientId(clientId)
            .checkType(checkType)
            .breValue(String.valueOf(breValue))
            .loanValue(String.valueOf(loanValue))
            .conflictField(field)
            .productCode(productCode)
            .createdAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)))
            .build();
    return qcCheckRepository.save(entity);
  }
}
