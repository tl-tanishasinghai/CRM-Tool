package com.trillionloans.los.repository;

import com.trillionloans.los.model.dto.internal.CallBackLog;
import java.time.LocalDateTime;
import java.util.List;
import reactor.core.publisher.Flux;

public interface CallbackRepositoryCustomQueryExecutor {
  Flux<CallBackLog> findAllByCriteria(
      List<String> productCodes,
      List<String> types,
      List<Long> ids,
      LocalDateTime startDate,
      LocalDateTime endDate,
      List<String> referenceIds,
      Boolean exceptionCheck);
}
