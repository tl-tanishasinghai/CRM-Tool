package com.trillionloans.lms.service.db;

import com.trillionloans.lms.model.entity.LoanDocumentMappingEntity;
import com.trillionloans.lms.repository.LoanDocumentMappingRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class LoanDocumentMappingService {
  private final LoanDocumentMappingRepository loanDocumentMappingRepository;

  public Mono<LoanDocumentMappingEntity> save(String lan, String path) {
    LoanDocumentMappingEntity loanDocumentMappingEntity =
        LoanDocumentMappingEntity.builder()
            .documentPath(path)
            .loanAccountNumber(lan)
            .createdAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")))
            .build();
    return loanDocumentMappingRepository.save(loanDocumentMappingEntity);
  }
}
