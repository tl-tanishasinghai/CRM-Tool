package com.trillionloans.los.repository;

import static com.trillionloans.los.constant.StringConstants.CALLBACK_RETRY;

import com.trillionloans.los.model.dto.internal.CallBackLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Implementation of the {@link CallbackRepositoryCustomQueryExecutor} interface. This class is
 * responsible for executing custom queries to fetch callback logs based on various search criteria.
 */
@Repository
@Slf4j
public class CallbackRepositoryCustomQueryExecutorImpl
    implements CallbackRepositoryCustomQueryExecutor {

  private final DatabaseClient databaseClient;

  /**
   * Constructs a {@link CallbackRepositoryCustomQueryExecutorImpl} with the specified
   * DatabaseClient.
   *
   * @param databaseClient the DatabaseClient used to execute SQL queries
   */
  public CallbackRepositoryCustomQueryExecutorImpl(DatabaseClient databaseClient) {
    this.databaseClient = databaseClient;
  }

  /**
   * Finds all callback logs that match the specified criteria.
   *
   * @param productCodes the list of product codes to filter by
   * @param types the list of callback types to filter by
   * @param ids the list of callback log IDs to filter by
   * @param startDate the start date for filtering logs
   * @param endDate the end date for filtering logs
   * @param referenceIds the list of reference IDs to filter by
   * @param exceptionCheck flag indicating whether to check for exceptions
   * @return a Flux of CallBackLog matching the search criteria
   */
  @Override
  public Flux<CallBackLog> findAllByCriteria(
      List<String> productCodes,
      List<String> types,
      List<Long> ids,
      LocalDateTime startDate,
      LocalDateTime endDate,
      List<String> referenceIds,
      Boolean exceptionCheck) {

    // build the SQL query based on the provided criteria
    StringBuilder query =
        getFetchQuery(productCodes, types, ids, startDate, endDate, referenceIds, exceptionCheck);
    var querySpec = databaseClient.sql(query.toString());
    log.info("[{}] callback retry sql query prepared: {}", CALLBACK_RETRY, query);

    // bind parameters to the query for dynamic filtering
    if (productCodes != null && !productCodes.isEmpty()) {
      querySpec = querySpec.bind("productCodes", productCodes);
    }
    if (types != null && !types.isEmpty()) {
      querySpec = querySpec.bind("types", types);
    }
    if (ids != null && !ids.isEmpty()) {
      querySpec = querySpec.bind("ids", ids);
    }
    if (startDate != null) {
      querySpec = querySpec.bind("startDate", startDate);
    }
    if (endDate != null) {
      querySpec = querySpec.bind("endDate", endDate);
    }
    if (referenceIds != null && !referenceIds.isEmpty()) {
      querySpec = querySpec.bind("referenceIds", referenceIds);
    }

    // execute the query and map the results to CallBackLog objects
    return querySpec.fetch().all().map(this::mapRowToCallBackLog);
  }

  /**
   * Maps a database row to a CallBackLog object.
   *
   * @param row a map representing a row from the database
   * @return a CallBackLog object populated with data from the row
   */
  private CallBackLog mapRowToCallBackLog(Map<String, Object> row) {
    CallBackLog callBackLog = new CallBackLog();
    callBackLog.setProductCode(String.valueOf(row.get("product_code")));
    callBackLog.setType(String.valueOf(row.get("type")));
    callBackLog.setRequest(row.get("request"));
    callBackLog.setReferenceId(String.valueOf(row.get("reference_id")));
    callBackLog.setResponse(row.get("response"));
    callBackLog.setException(String.valueOf(row.get("exception")));
    callBackLog.setUri(String.valueOf(row.get("uri")));
    callBackLog.setCreatedAt(String.valueOf(row.get("created_at")));
    return callBackLog;
  }

  /**
   * Constructs the SQL query for fetching callback logs based on the provided criteria.
   *
   * @param productCodes the list of product codes to filter by
   * @param types the list of callback types to filter by
   * @param ids the list of callback log IDs to filter by
   * @param startDate the start date for filtering logs
   * @param endDate the end date for filtering logs
   * @param referenceIds the list of reference IDs to filter by
   * @param exceptionCheck flag indicating whether to check for exceptions
   * @return a StringBuilder containing the constructed SQL query
   */
  private StringBuilder getFetchQuery(
      List<String> productCodes,
      List<String> types,
      List<Long> ids,
      LocalDateTime startDate,
      LocalDateTime endDate,
      List<String> referenceIds,
      Boolean exceptionCheck) {

    StringBuilder query = new StringBuilder("SELECT * FROM callback WHERE 1 = 1");

    // append conditions based on provided criteria
    if (productCodes != null && !productCodes.isEmpty()) {
      query.append(" AND product_code IN (:productCodes)");
    }
    if (types != null && !types.isEmpty()) {
      query.append(" AND type IN (:types)");
    }
    if (ids != null && !ids.isEmpty()) {
      query.append(" AND id IN (:ids)");
    }
    if (startDate != null) {
      query.append(" AND created_at >= :startDate");
    }
    if (endDate != null) {
      query.append(" AND created_at <= :endDate");
    }
    if (referenceIds != null && !referenceIds.isEmpty()) {
      query.append(" AND reference_id IN (:referenceIds)");
    } else {
      query.append(" AND reference_id IS NOT NULL");
    }
    if (exceptionCheck != null) {
      if (exceptionCheck) {
        query.append(" AND exception IS NOT NULL");
      } else {
        query.append(" AND exception IS NULL");
      }
    }
    query.append(" AND (is_retry IS NULL OR is_retry = false)");
    return query;
  }
}
