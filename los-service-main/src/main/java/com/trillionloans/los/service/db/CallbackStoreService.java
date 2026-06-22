package com.trillionloans.los.service.db;

import com.trillionloans.los.model.dto.internal.CallBackLog;
import com.trillionloans.los.model.entity.CallbackLogEntity;
import com.trillionloans.los.model.request.CallbackSearchCriteria;
import com.trillionloans.los.repository.CallbackRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Service for managing callback logs. */
@Service
@AllArgsConstructor
public class CallbackStoreService {

  private final CallbackRepository callbackRepository; // Repository for callback log operations

  /**
   * Saves a callback log entity to the database.
   *
   * @param callback the callback log entity to save
   * @return a Mono containing the saved callback log entity
   */
  public Mono<CallbackLogEntity> save(CallbackLogEntity callback) {
    return callbackRepository.save(callback); // Save the callback log entity
  }

  /**
   * Finds all callback logs based on the specified search criteria.
   *
   * @param criteria the criteria to filter callback logs
   * @return a Flux containing the matching callback logs
   */
  public Flux<CallBackLog> findAllByCriteria(CallbackSearchCriteria criteria) {
    // parse the start date from the criteria, defaulting to null if not provided
    LocalDateTime startDate =
        (criteria.getStartDate() != null)
            ? LocalDateTime.parse(
                criteria.getStartDate() + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            : null;

    // parse the end date from the criteria, defaulting to null if not provided
    LocalDateTime endDate =
        (criteria.getEndDate() != null)
            ? LocalDateTime.parse(
                criteria.getEndDate() + "T23:59:59", DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            : null;

    // retrieve product codes from criteria, defaulting to an empty list if not provided
    List<String> productCodes =
        (criteria.getProductCodes() != null) ? criteria.getProductCodes() : Collections.emptyList();

    // retrieve types from criteria, defaulting to an empty list if not provided
    List<String> types =
        (criteria.getTypes() != null) ? criteria.getTypes() : Collections.emptyList();

    // retrieve IDs from criteria, defaulting to an empty list if not provided
    List<Long> ids = (criteria.getIds() != null) ? criteria.getIds() : Collections.emptyList();

    // retrieve reference IDs from criteria, defaulting to an empty list if not provided
    List<String> referenceIds =
        (criteria.getReferenceIds() != null) ? criteria.getReferenceIds() : Collections.emptyList();

    // determine if exception check is enabled
    Boolean exceptionCheck =
        !Objects.isNull(criteria.getExceptionCheck()) && criteria.getExceptionCheck();

    // call the repository method to find callback logs based on the criteria
    return callbackRepository.findAllByCriteria(
        productCodes, types, ids, startDate, endDate, referenceIds, exceptionCheck);
  }
}
