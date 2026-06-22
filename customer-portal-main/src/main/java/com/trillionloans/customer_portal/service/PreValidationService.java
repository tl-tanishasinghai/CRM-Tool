package com.trillionloans.customer_portal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.customer_portal.api.internal.LmsApi;
import com.trillionloans.customer_portal.api.internal.LosApi;
import com.trillionloans.customer_portal.constant.ValidationKey;
import com.trillionloans.customer_portal.model.dto.LeadIdResponse;
import com.trillionloans.customer_portal.model.dto.LoanApplicationIdResponse;
import com.trillionloans.customer_portal.model.dto.LoanDetailsResponse;
import com.trillionloans.customer_portal.model.dto.MobileNumberAttributes;
import com.trillionloans.customer_portal.repository.RedisRepositoryImpl;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * PreValidationService is responsible for validating dynamic path variables (like
 * loanAccountNumber, loanApplicationId, or leadId) against the mobile number extracted from the
 * user's JWT token.
 *
 * <p>we enforce validation at the WebFilter layer using this service before the request reaches the
 * controller.
 *
 * <p><b>How it works:</b>
 *
 * <ul>
 *   <li>1. Checks if the path variable (e.g., loanAccountNumber) is valid using predefined regex
 *       patterns.
 *   <li>2. Attempts to fetch the user's linked attributes (loan accounts, lead IDs, etc.) from
 *       Redis.
 *   <li>3. If not in Redis, makes external calls to LOS and LMS to get the latest data and stores
 *       it in Redis.
 *   <li>4. Returns a Mono<Boolean> indicating whether the given value belongs to the authenticated
 *       user.
 * </ul>
 *
 * <p><b>Where it's used:</b> This service is primarily invoked from {@link
 * com.trillionloans.customer_portal.configuration.filter.DynamicPathVariableFilter} which
 * intercepts requests and validates path variables before routing them further.
 *
 * <p><b>Things to keep in mind:</b>
 *
 * <ul>
 *   <li>Always validate formats first to avoid unnecessary Redis/API calls.
 *   <li>If Redis returns no data, ensure fresh data is fetched and cached properly.
 *   <li>Look at {@link com.trillionloans.customer_portal.constant.ValidationKey} to add support for
 *       more path variable keys.
 * </ul>
 *
 * @see com.trillionloans.customer_portal.configuration.filter.DynamicPathVariableFilter
 * @see com.trillionloans.customer_portal.constant.ValidationKey
 * @see com.trillionloans.customer_portal.repository.RedisRepositoryImpl
 */
@Service
@Slf4j
public class PreValidationService {

  private final RedisRepositoryImpl redisRepository;
  private final LosApi losApi;
  private final LmsApi lmsApi;
  private final ObjectMapper objectMapper;

  public PreValidationService(RedisRepositoryImpl redisRepository, LosApi losApi, LmsApi lmsApi) {
    this.redisRepository = redisRepository;
    this.losApi = losApi;
    this.lmsApi = lmsApi;
    this.objectMapper = new ObjectMapper();
  }

  @Value("${redis.cache.time}")
  private long cacheTime;

  /**
   * Validates whether a given key-value pair (e.g. loanAccountNumber = "1234") belongs to the given
   * mobile number. Steps: 1. Validate format of the value 2. Check Redis 3. If not found, fetch
   * from source APIs (LOS/LMS), cache it, and retry
   */
  public Mono<Boolean> validatePathVariables(
      String key, String value, String mobileNumber, String dateOfBirth, String panLast4Digits) {
    ValidationKey validationKey;
    try {
      validationKey = ValidationKey.fromKey(key);
    } catch (IllegalArgumentException e) {
      return Mono.error(e);
    }

    if (!validationKey.isValid(value)) {
      return Mono.error(new IllegalArgumentException("Invalid format for " + key));
    }
    String preValidateRedisKey = mobileNumber + ":" + dateOfBirth + ":" + panLast4Digits;
    return getRedisValue(validationKey, value, mobileNumber, preValidateRedisKey, false)
        .switchIfEmpty(
            cacheAndValidate(validationKey, value, mobileNumber, dateOfBirth, panLast4Digits));
  }

  /**
   * Checks whether the Redis cache contains the mapping between the path variable and the mobile
   * number.
   */
  private Mono<Boolean> getRedisValue(
      ValidationKey key,
      String value,
      String mobileNumber,
      String preValidateRedisKey,
      boolean emptyFlag) {
    if (key == ValidationKey.MOBILE_NUMBER) {
      return Mono.just(value.equals(mobileNumber));
    }

    return redisRepository
        .getKey(preValidateRedisKey)
        .flatMap(
            redisValue -> {
              try {
                MobileNumberAttributes attributes =
                    objectMapper.readValue(redisValue, MobileNumberAttributes.class);

                boolean match =
                    switch (key) {
                      case LOAN_APPLICATION_ID ->
                          attributes.getLoanApplicationIds().contains(value);
                      case LOAN_ACCOUNT_NUMBER ->
                          attributes.getLoanAccountNumbers().contains(value);
                      case LEAD_ID -> value.equals(attributes.getLeadId());
                      default -> false;
                    };

                // If no match and emptyFlag is false, return empty (retry using APIs)
                if (!match && !emptyFlag) {
                  return Mono.empty();
                }
                return Mono.just(match);

              } catch (JsonProcessingException e) {
                log.error("Error parsing Redis JSON for {}: {}", mobileNumber, e.getMessage());
                return Mono.error(new RuntimeException("Failed to parse Redis value", e));
              }
            });
  }

  /**
   * If Redis doesn't have the data, fetch user-specific details from LOS/LMS APIs, store in Redis,
   * and retry validation.
   */
  private Mono<Boolean> cacheAndValidate(
      ValidationKey key,
      String value,
      String mobileNumber,
      String dateOfBirth,
      String panLast4Digits) {
    return getAllDetailsAgainstMobileNumber(mobileNumber, dateOfBirth, panLast4Digits)
        .flatMap(
            detailResponse -> {
              try {
                String json = objectMapper.writeValueAsString(detailResponse);
                String preValidateRedisKey =
                    mobileNumber + ":" + dateOfBirth + ":" + panLast4Digits;
                return redisRepository
                    .putKey(preValidateRedisKey, json, Duration.ofMinutes(cacheTime))
                    .flatMap(
                        success -> {
                          if (Boolean.TRUE.equals(success)) {
                            return getRedisValue(
                                    key, value, mobileNumber, preValidateRedisKey, true)
                                .switchIfEmpty(Mono.just(false));
                          } else {
                            return Mono.error(
                                new RuntimeException("Failed to store data in Redis"));
                          }
                        });
              } catch (JsonProcessingException e) {
                log.error("Error serializing MobileNumberAttributes for Redis: {}", e.getMessage());
                return Mono.error(new RuntimeException("Failed to serialize object", e));
              }
            })
        .switchIfEmpty(Mono.just(false));
  }

  /**
   * Calls downstream LOS and LMS APIs to fetch all user-related identifiers based on the provided
   * mobile number.
   *
   * <p>This includes: - Primary leadId - All loan account numbers - All loan application IDs
   */
  private Mono<MobileNumberAttributes> getAllDetailsAgainstMobileNumber(
      String mobileNumber, String dateOfBirth, String panLast4Digits) {
    return losApi
        .fetchLeadDetailAgainstMobileNumberAndDOB(mobileNumber, dateOfBirth)
        .collectList()
        .flatMap(
            list -> {
              log.info(
                  "[PreValidation] fetched {} lead(s) for mobileNumber={}",
                  list.size(),
                  mobileNumber);
              if (list.size() == 1) {
                LeadIdResponse leadIdResponse = list.get(0);
                return Mono.just(leadIdResponse);
              } else if (list.size() > 1) {
                return losApi
                    .fetchLeadDetailAgainstMobileNumberDOBAndPAN(
                        mobileNumber, dateOfBirth, panLast4Digits)
                    .collectList()
                    .flatMap(
                        panList -> {
                          if (panList.size() == 1) {
                            LeadIdResponse leadIdResponse = panList.get(0);
                            return Mono.just(leadIdResponse);
                          } else {
                            return Mono.empty();
                          }
                        });
              } else {
                return Mono.empty();
              }
            })
        .flatMap(
            lead -> {
              String leadId = String.valueOf(lead.getEntityId());

              Mono<List<String>> loanAccountNumbersMono =
                  lmsApi
                      .fetchAllLoansDetails(leadId)
                      .map(LoanDetailsResponse::getLoanAccountNumber)
                      .filter(Objects::nonNull)
                      .map(num -> num.replaceFirst("^0+(?!$)", ""))
                      .collectList();

              Mono<List<String>> loanApplicationIdsMono =
                  losApi
                      .fetchAllLoanApplicationIdsAgainstLeadId(leadId)
                      .map(LoanApplicationIdResponse::getLoanApplicationId)
                      .filter(Objects::nonNull)
                      .map(num -> num.replaceFirst("^0+(?!$)", ""))
                      .collectList();

              return Mono.zip(loanAccountNumbersMono, loanApplicationIdsMono)
                  .map(tuple -> new MobileNumberAttributes(leadId, tuple.getT1(), tuple.getT2()));
            });
  }
}
