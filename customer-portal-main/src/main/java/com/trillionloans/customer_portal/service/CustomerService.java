package com.trillionloans.customer_portal.service;

import static com.trillionloans.customer_portal.constant.StringConstants.DD_MM_YYYY;
import static com.trillionloans.customer_portal.constant.StringConstants.DD_MM_YYYY_DASH;
import static com.trillionloans.customer_portal.constant.StringConstants.TRACE_ID;
import static com.trillionloans.customer_portal.constant.StringConstants.UNKNOWN_LOAN_STATUS;
import static com.trillionloans.customer_portal.util.DateTimeUtil.getDateAsString;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.customer_portal.api.external.FreshdeskApi;
import com.trillionloans.customer_portal.api.internal.LmsApi;
import com.trillionloans.customer_portal.api.internal.LosApi;
import com.trillionloans.customer_portal.constant.CollectionType;
import com.trillionloans.customer_portal.constant.DocumentTagValue;
import com.trillionloans.customer_portal.constant.LoanStatus;
import com.trillionloans.customer_portal.constant.ResponseStatus;
import com.trillionloans.customer_portal.exception.NotFoundException;
import com.trillionloans.customer_portal.exception.ServerErrorException;
import com.trillionloans.customer_portal.model.dto.CategoryListResponse;
import com.trillionloans.customer_portal.model.dto.ClientConsentDTO;
import com.trillionloans.customer_portal.model.dto.ClientDetailsCpResponseDto;
import com.trillionloans.customer_portal.model.dto.DueDetailsResponse;
import com.trillionloans.customer_portal.model.dto.ForeclosureDetailsResponseDto;
import com.trillionloans.customer_portal.model.dto.LeadDetailsDTO;
import com.trillionloans.customer_portal.model.dto.LeadIdResponse;
import com.trillionloans.customer_portal.model.dto.LoanDetailsDTO;
import com.trillionloans.customer_portal.model.dto.LoanDetailsResponse;
import com.trillionloans.customer_portal.model.dto.RPSInfo;
import com.trillionloans.customer_portal.model.dto.RPSResponseWithDPD;
import com.trillionloans.customer_portal.model.dto.RepaymentDetails;
import com.trillionloans.customer_portal.model.dto.SimplifiedTransactionResponse;
import com.trillionloans.customer_portal.model.dto.SubmitFormRequest;
import com.trillionloans.customer_portal.model.dto.TransactionDetailResponse;
import com.trillionloans.customer_portal.model.internal.RpsPdfBuilder;
import com.trillionloans.customer_portal.model.response.LeadExistsResponse;
import com.trillionloans.customer_portal.model.response.ResponseDTO;
import com.trillionloans.customer_portal.model.response.RpsResponseDto.ResponseRpsDTO;
import com.trillionloans.customer_portal.util.CacheEncryptionService;
import com.trillionloans.customer_portal.util.CommonUtil;
import com.trillionloans.customer_portal.util.DateTimeUtil;
import com.trillionloans.customer_portal.util.LeadDetailsUtil;
import com.trillionloans.customer_portal.util.LoanDetailsUtil;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class CustomerService {

  private final LosApi losApi;
  private final LmsApi lmsApi;
  private final RpsPdfBuilder rpsPdfBuilder;
  private final PaymentService paymentService;

  @Value("${img-path.trillion-logo}")
  private String img;

  private final FreshdeskApi freshdeskApi;
  private final RedisCacheService redisCacheService;

  private final CacheEncryptionService cacheEncryptionService;

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Value("${redis.cache.details-ttl}")
  private long detailsCacheTTL;


  @Value("${disable.product-id:}")
  private String disableProductIds;

  // Lazy-initialized cache for disabled product IDs
  private volatile Set<String> disabledProductIdSet;

  @Autowired
  public CustomerService(
      LosApi losApi,
      LmsApi lmsApi,
      FreshdeskApi freshdeskApi,
      RpsPdfBuilder rpsPdfBuilder,
      RedisCacheService redisCacheService,
      CacheEncryptionService cacheEncryptionService,
      PaymentService paymentService) {
    this.losApi = losApi;
    this.lmsApi = lmsApi;
    this.rpsPdfBuilder = rpsPdfBuilder;
    this.freshdeskApi = freshdeskApi;
    this.redisCacheService = redisCacheService;
    this.cacheEncryptionService = cacheEncryptionService;
    this.paymentService = paymentService;
  }

  public Mono<LeadDetailsDTO> getDetails(String leadId) {
    return fetchFromCache(leadId)
        .switchIfEmpty(
            losApi
                .fetchLeadDetails(leadId)
                .map(LeadDetailsUtil::transformLeadDetails)
                .flatMap(response -> asyncCacheLeadDetails(response).thenReturn(response)));
  }

  public Mono<Void> asyncCacheLeadDetails(LeadDetailsDTO details) {
    return Mono.fromCallable(
            () -> {
              String json = OBJECT_MAPPER.writeValueAsString(details);
              return cacheEncryptionService.encrypt(json);
            })
        .flatMap(
            encrypted ->
                redisCacheService.cacheObjectSilently(
                    String.valueOf(details.getLeadId()), encrypted, detailsCacheTTL))
        .doOnSubscribe(
            s ->
                log.info(
                    "[CLIENT_DETAILS_CACHE] Attempting to cache leadId={}", details.getLeadId()))
        .doOnSuccess(
            v ->
                log.info(
                    "[CLIENT_DETAILS_CACHE] Successfully cached leadId={}", details.getLeadId()))
        .doOnError(
            e ->
                log.error(
                    "[CLIENT_DETAILS_CACHE] Failed caching leadId={}, error={}",
                    details.getLeadId(),
                    e.getMessage()))
        .then();
  }

  public Mono<LeadDetailsDTO> fetchFromCache(String clientId) {
    log.info("[CLIENT_DETAILS_FETCH] Attempting to fetch from Redis, key={}", clientId);

    return redisCacheService
        .getKey(clientId)
        .flatMap(
            cachedResponse -> {
              if (cachedResponse == null || cachedResponse.isEmpty()) {
                log.info("[CLIENT_DETAILS_FETCH] Cache miss for key={}", clientId);
                return Mono.empty();
              }

              try {
                String decrypted = cacheEncryptionService.decrypt(cachedResponse);
                LeadDetailsDTO dto = OBJECT_MAPPER.readValue(decrypted, LeadDetailsDTO.class);
                log.info("[CLIENT_DETAILS_FETCH] Cache hit for key={}", clientId);
                return Mono.just(dto);
              } catch (Exception e) {
                log.warn(
                    "[CLIENT_DETAILS_FETCH] Failed to parse cached response for key={}, error={}",
                    clientId,
                    e.getMessage());
                return Mono.empty();
              }
            })
        .onErrorResume(
            e -> {
              log.warn(
                  "[CLIENT_DETAILS_FETCH] Error fetching from Redis, key={}, error={}",
                  clientId,
                  e.getMessage());
              return Mono.empty();
            });
  }

  public Mono<Void> validateRequest(SubmitFormRequest request, LeadDetailsDTO leadDetails) {

    if (!request.getEmail().equalsIgnoreCase(leadDetails.getEmail())) {
      return Mono.error(new IllegalArgumentException("Email mismatch"));
    }

    String formMobile =
        request
            .getRegisteredMobileNumber()
            .replaceAll("^(\\+91|91)", "") // removed +91
            .replaceAll("^0+", "");

    if (!formMobile.equals(leadDetails.getMobileNo())) {
      return Mono.error(new IllegalArgumentException("Mobile number mismatch"));
    }

    if (request.getPanCard() != null
        && !request.getPanCard().equalsIgnoreCase(leadDetails.getPanNumber())) {
      return Mono.error(new IllegalArgumentException("PAN mismatch"));
    }
    String loanId = request.getLoanId();
    List<String> loanAccounts = leadDetails.getLoanAccounts();

    boolean requestHasLoanId = !StringUtils.isEmpty(loanId);
    boolean leadHasLoanAccounts = !CollectionUtils.isEmpty(loanAccounts);

    if (requestHasLoanId) {

      if (!leadHasLoanAccounts || !loanAccounts.contains(loanId)) {
        return Mono.error(new IllegalArgumentException("Loan ID not found for this client"));
      }
    } else {
      if (leadHasLoanAccounts) {
        return Mono.error(new IllegalArgumentException("Loan ID is required for this client"));
      }
    }

    return Mono.empty();
  }

  public Mono<LeadDetailsDTO> resolveClientDetails(String clientId) {
    return fetchFromCache(clientId)
        .switchIfEmpty(
            losApi
                .fetchLeadDetails(clientId)
                .map(LeadDetailsUtil::transformLeadDetails)
                .flatMap(details -> asyncCacheLeadDetails(details).thenReturn(details)));
  }

  public Mono<ResponseDTO<String>> submitForm(SubmitFormRequest request, String leadId) {
    return Mono.deferContextual(
        context ->
            resolveClientDetails(leadId)
                .flatMap(
                    leadDetails ->
                        validateRequest(request, leadDetails)
                            .then(freshdeskApi.createTicket(request)))
                .map(
                    response ->
                        ResponseDTO.<String>builder()
                            .status(ResponseStatus.SUCCESS)
                            .message("Form submitted successfully!")
                            .traceId(context.get(TRACE_ID))
                            .data("Ticket created with ID: " + response.getId())
                            .build())
                .onErrorResume(
                    error -> {
                      log.error(
                          "[FRESHDESK_PROCESS] [ERROR] Failed to process request [traceId={}],"
                              + " error: {}",
                          context.get(TRACE_ID),
                          error.getMessage(),
                          error);

                      // Check if the error is a validation error
                      String message =
                          (error instanceof IllegalArgumentException)
                              ? error.getMessage()
                              : "We were unable to process your request at the moment.";

                      return Mono.just(
                          ResponseDTO.<String>builder()
                              .status(ResponseStatus.FAIL)
                              .message(message)
                              .traceId(context.get(TRACE_ID))
                              .data(null)
                              .build());
                    }));
  }

  public Mono<CategoryListResponse> getCategories() {
    return freshdeskApi.getTopLevelCategories();
  }

  public Mono<byte[]> getSOA(String loanAccountNumber) {
    String dateOfGeneration = getDateAsString(LocalDate.now());
    return lmsApi.fetchSOA(loanAccountNumber, dateOfGeneration);
  }

  public Mono<byte[]> getNOC(String loanAccountNumber) {
    return lmsApi.fetchNOC(loanAccountNumber);
  }

  public Mono<byte[]> getDocument(String loanApplicationId, String documentId) {
    return losApi.fetchDocumentAgainstId(loanApplicationId, documentId);
  }

  public Flux<?> getAllDocumentDetails(String loanApplicationId) {
    return losApi
        .fetchAllDocumentDetailsAgainstLoanAppId(loanApplicationId)
        .filter(
            document ->
                isTagValueIncluded(document.getTagValue())) // Filter based on the enum values
        .switchIfEmpty(
            Flux.error(
                new NotFoundException(
                    "No documents found for loanApplicationId: " + loanApplicationId)));
  }

  public Flux<?> fetchLeadIdAgainstMobileNumber(String mobileNumber) {
    return losApi
        .fetchLeadDetailAgainstMobileNumber(mobileNumber)
        .switchIfEmpty(
            Mono.error(new NotFoundException("No lead found for mobileNumber: " + mobileNumber)))
        .sort((lead1, lead2) -> Long.compare(lead2.getEntityId(), lead1.getEntityId()))
        .next()
        .map(lead -> Map.of("leadId", lead.getEntityId()))
        .flux();
  }

  public Mono<LeadExistsResponse> checkLeadExists(String mobileNumber) {
    return losApi
        .fetchLeadDetailAgainstMobileNumber(mobileNumber)
        .hasElements()
        .map(LeadExistsResponse::new);
  }

  public Mono<SimplifiedTransactionResponse> getTransactions(String loanAccountNumber) {
    return getTransactionDetails(loanAccountNumber)
      .flatMap(
        lmsTransactions -> {
          return paymentService.getLatestCollection(loanAccountNumber)
            .flatMap(
              latestCollectionDetails -> {
                // Return transactions with collection details
                return Mono.just(SimplifiedTransactionResponse.builder()
                  .transactions(lmsTransactions.getTransactions())
                  .latestCollectionDetails(latestCollectionDetails)
                  .build());
              })
            .onErrorResume(
              error -> {
                log.warn("failed to get latest collection details for loanAccountNumber={}, error={}", loanAccountNumber, error.getMessage());
                // If collection details fail, return transactions with null collection details
                return Mono.just(SimplifiedTransactionResponse.builder()
                  .transactions(lmsTransactions.getTransactions())
                  .latestCollectionDetails(null)
                  .build());
              });
        });
  }

  public Mono<SimplifiedTransactionResponse> getTransactionDetails(String loanAccountNumber) {
    return lmsApi
        .fetchTransactionDetails(loanAccountNumber)
        .flatMap(
            transactionDetailResponse -> {
              List<TransactionDetailResponse.Transaction> transactions =
                  transactionDetailResponse.getTransactions();

              if (CommonUtil.nullOrEmpty(transactions)) {
                return Mono.error(
                    new NotFoundException(
                        "No transactions found for loan account number: " + loanAccountNumber));
              }
              List<Mono<SimplifiedTransactionResponse.Transaction>> transactionMonos =
                  transactionDetailResponse.getTransactions().stream()
                      .map(LoanDetailsUtil::processTransactionAsync)
                      .collect(Collectors.toList());
              return Flux.fromIterable(transactionMonos)
                  .flatMap(mono -> mono.filterWhen(transaction -> Mono.just(transaction != null)))
                  .collectList()
                  .flatMap(
                      latestTransactions -> {
                        if (latestTransactions.isEmpty()) {
                          return Mono.error(
                              new NotFoundException(
                                  "No valid transactions found for loan account number: "
                                      + loanAccountNumber));
                        }

                        return Mono.just(
                            SimplifiedTransactionResponse.builder()
                                .transactions(latestTransactions)
                                .build());
                      });
            });
  }

  public Flux<LoanDetailsDTO> getLoanDetails(String leadId, LoanStatus status) {
    return getAllLoanDetails(leadId, status)
      .flatMap(this::processLoanDetails, 8) // Increased parallelism from 6 to 8
      .sort(createLoanComparator()); // Stream sorting without intermediate collection
  }

  private Comparator<LoanDetailsDTO> createLoanComparator() {
    // Comparator for status: "Active" first, nulls last
    Comparator<LoanDetailsDTO> statusComparator = (loan1, loan2) -> {
      String status1 = loan1.getStatus();
      String status2 = loan2.getStatus();

      if (status1 == null && status2 == null) return 0;
      if (status1 == null) return 1; // nulls last
      if (status2 == null) return -1; // nulls last

      boolean isActive1 = "Active".equalsIgnoreCase(status1);
      boolean isActive2 = "Active".equalsIgnoreCase(status2);

      if (isActive1 && !isActive2) return -1; // Active first
      if (!isActive1 && isActive2) return 1;
      return 0;
    };

    // Comparator for dpdDays: higher values first, nulls last
    Comparator<LoanDetailsDTO> dpdComparator = (loan1, loan2) -> {
      Integer dpd1 = loan1.getDpdDays();
      Integer dpd2 = loan2.getDpdDays();

      if (dpd1 == null && dpd2 == null) return 0;
      if (dpd1 == null) return 1; // nulls last
      if (dpd2 == null) return -1; // nulls last

      return Integer.compare(dpd2, dpd1); // descending order (higher first)
    };

    // Comparator for isDirectPaymentEnabled: true first, nulls last
    Comparator<LoanDetailsDTO> directPaymentComparator = (loan1, loan2) -> {
      Boolean isEnabled1 = loan1.getCollectionDetails() != null
        ? loan1.getCollectionDetails().getIsDirectPaymentEnabled()
        : null;
      Boolean isEnabled2 = loan2.getCollectionDetails() != null
        ? loan2.getCollectionDetails().getIsDirectPaymentEnabled()
        : null;

      if (isEnabled1 == null && isEnabled2 == null) return 0;
      if (isEnabled1 == null) return 1; // nulls last
      if (isEnabled2 == null) return -1; // nulls last

      if (isEnabled1 && !isEnabled2) return -1; // true first
      if (!isEnabled1 && isEnabled2) return 1;
      return 0;
    };

    // Combine all comparators: status -> dpdDays -> isDirectPaymentEnabled
    return statusComparator
      .thenComparing(directPaymentComparator)
      .thenComparing(dpdComparator);
  }

  private Mono<LoanDetailsDTO> processLoanDetails(LoanDetailsDTO loan) {
    // process Last(recent) Transaction Date and Amount
    Mono<Void> transactionsStep =
      getTransactionDetails(loan.getLoanAccountNumber())
        .doOnNext(
          transactionResp ->
            setLastPaymentFromTransactions(loan, transactionResp.getTransactions()))
        .then()
        .onErrorResume(e -> Mono.empty()); // continue without setting lastPaymentDone

    // Process Next Due Date and Next Due Amount
    /*
      1. Next Due Date:
         - Retrieved from the RPS API.
         - Navigate through: response -> repaymentSchedule -> periods.
         - Select the first period where dueDate is after today's date and the installment is not marked complete.

      2. Next Due Amount:
         - This is NOT the standard EMI amount.
         - It includes interest plus any missed (overdue) amounts.
         - To fetch this, we call the "Due As On Date" API using today's date and extract the relevant data.
    */

    Mono<Void> nextDueStep =
      getRPSInfo(loan.getLoanAccountNumber())
        .doOnNext(response -> loan.setDpdDays(response.getDpdDays()))
        .flatMap(
          response -> {
            if (CommonUtil.nullOrEmpty(response.getNextDueDate())) {
              // Set nextDueSplit to zeros if no next due date
              loan.setNextDueSplit(
                RepaymentDetails.builder()
                  .principalDue(0.0)
                  .interestDue(0.0)
                  .chargesDue(0.0)
                  .build()
              );
              return Mono.empty();
            }

            return getNextDueAmount(
              loan.getLoanAccountNumber(),
              DateTimeUtil.parseDateFromPattern(
                response.getNextDueDate(), DD_MM_YYYY, DD_MM_YYYY_DASH))
              .doOnNext(
                dueAmount -> {
                  loan.setNextPaymentDue(
                    LoanDetailsDTO.NextPaymentDue.builder()
                      .date(response.getNextDueDate())
                      .amount(dueAmount.getTotalDue())
                      .build());
                  Double feeCharges = dueAmount.getFeeChargesDue() != null ? dueAmount.getFeeChargesDue() : 0.0;
                  Double penaltyCharges = dueAmount.getPenaltyChargesDue() != null ? dueAmount.getPenaltyChargesDue() : 0.0;
                  Double principleDue = dueAmount.getPrinicpalDue() != null ? dueAmount.getPrinicpalDue() : 0.0;
                  Double interestDue = dueAmount.getInterestDue() != null ? dueAmount.getInterestDue() : 0.0;
                  loan.setNextDueSplit(
                    RepaymentDetails.builder()
                      .principalDue(principleDue)
                      .interestDue(interestDue)
                      .chargesDue(feeCharges + penaltyCharges)
                      .build()
                  );
                });
          })
        .then()
        .onErrorResume(e -> Mono.empty());

    // Process Collection Details
    Mono<Void> collectionStep =
      paymentService.getCollectionDetails(loan.getLoanAccountNumber())
        .doOnNext(loan::setCollectionDetails)
        .then()
        .onErrorResume(e -> Mono.empty()); // continue without setting collectionDetails

    // Process Foreclosure Details
    Mono<Void> foreclosureStep =
      getForeclosureDetails(loan.getLoanAccountNumber(),
        LocalDate.now().format(DateTimeFormatter.ofPattern(DD_MM_YYYY_DASH)),
        false,
        false)
        .doOnNext(
          foreclosureDetails -> {
            Double feeCharges = foreclosureDetails.getFeeChargesPortion() != null ? foreclosureDetails.getFeeChargesPortion() : 0.0;
            Double penaltyCharges = foreclosureDetails.getPenaltyChargesPortion() != null ? foreclosureDetails.getPenaltyChargesPortion() : 0.0;
            Double principleDue = foreclosureDetails.getPrincipalPortion() != null ? foreclosureDetails.getPrincipalPortion() : 0.0;
            Double interestDue = foreclosureDetails.getInterestPortion() != null ? foreclosureDetails.getInterestPortion() : 0.0;
            loan.setForeclosureSplit(
              RepaymentDetails.builder()
                .principalDue(principleDue)
                .interestDue(interestDue)
                .chargesDue(feeCharges + penaltyCharges)
                .build()
            );
          })
        .then()
        .onErrorResume(e -> Mono.empty()); // continue without setting foreclosureSplit

    // Process currentDue split Details
    Mono<Void> currentDueStep =
      getNextDueAmount(
        loan.getLoanAccountNumber(),
        LocalDate.now().format(DateTimeFormatter.ofPattern(DD_MM_YYYY_DASH)))
        .doOnNext(
          currentDueDetails -> {
            Double feeCharges = currentDueDetails.getFeeChargesDue() != null ? currentDueDetails.getFeeChargesDue() : 0.0;
            Double penaltyCharges = currentDueDetails.getPenaltyChargesDue() != null ? currentDueDetails.getPenaltyChargesDue() : 0.0;
            Double principleDue = currentDueDetails.getPrinicpalDue() != null ? currentDueDetails.getPrinicpalDue() : 0.0;
            Double interestDue = currentDueDetails.getInterestDue() != null ? currentDueDetails.getInterestDue() : 0.0;
            loan.setCurrentDueSplit(
              RepaymentDetails.builder()
                .principalDue(principleDue)
                .interestDue(interestDue)
                .chargesDue(feeCharges + penaltyCharges)
                .build()
            );
          })
        .then()
        .onErrorResume(e -> Mono.empty()); // continue without setting foreclosureSplit

    return Mono.when(transactionsStep, nextDueStep, collectionStep, foreclosureStep, currentDueStep)
      .thenReturn(loan)
      .map(processedLoan -> {
        // Override nextDueSplit if NEXT_DUE_AMOUNT is not in enabledCollections
        if (processedLoan.getCollectionDetails() != null
          && processedLoan.getCollectionDetails().getEnabledCollections() != null
          && !processedLoan.getCollectionDetails().getEnabledCollections().contains(CollectionType.NEXT_DUE_AMOUNT.getDisplayName())) {
          processedLoan.setNextDueSplit(
            RepaymentDetails.builder()
              .principalDue(0.0)
              .interestDue(0.0)
              .chargesDue(0.0)
              .build()
          );
        }
        // Override currentDueSplit if CURRENT_DUE_AMOUNT is not in enabledCollections
        if (processedLoan.getCollectionDetails() != null
          && processedLoan.getCollectionDetails().getEnabledCollections() != null
          && !processedLoan.getCollectionDetails().getEnabledCollections().contains(CollectionType.CURRENT_DUE_AMOUNT.getDisplayName())) {
          processedLoan.setCurrentDueSplit(
            RepaymentDetails.builder()
              .principalDue(0.0)
              .interestDue(0.0)
              .chargesDue(0.0)
              .build()
          );
        }
        // Override foreclosureSplit if FULL_AMOUNT is not in enabledCollections
        if (processedLoan.getCollectionDetails() != null
          && processedLoan.getCollectionDetails().getEnabledCollections() != null
          && !processedLoan.getCollectionDetails().getEnabledCollections().contains(CollectionType.FULL_AMOUNT.getDisplayName())) {
          processedLoan.setForeclosureSplit(
            RepaymentDetails.builder()
              .principalDue(0.0)
              .interestDue(0.0)
              .chargesDue(0.0)
              .build()
          );
        }
        return processedLoan;
      });
  }

  public Mono<ForeclosureDetailsResponseDto> getForeclosureDetails(String loanAccountNumber, String transactionDate,
                                                                   Boolean isTotalOutstandingInterest, Boolean includePreClosureReason) {
    return lmsApi.getForeclosureDetails(loanAccountNumber, transactionDate, isTotalOutstandingInterest, includePreClosureReason);
  }


  private boolean isProductDisabled(LoanDetailsResponse loan) {
    if (StringUtils.isBlank(disableProductIds) || loan.getProductId() == null) {
      return false; // No disabled products or no product ID
    }

    // Lazy initialization of disabled product IDs set with double-check locking
    if (disabledProductIdSet == null) {
      synchronized (this) {
        if (disabledProductIdSet == null) {
          disabledProductIdSet = Arrays.stream(disableProductIds.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
        }
      }
    }

    // Split the comma-separated disabled product IDs and check if it contains the product ID
    String productId = loan.getProductId().toString();
    return disabledProductIdSet.contains(productId); // O(1) lookup
  }

  private Mono<RPSInfo> getRPSInfo(String loanAccountNumber) {
    return getRPSWithDPD(loanAccountNumber)
        .flatMap(
            response -> {
              Mono<String> nextDueDateMono =
                  Flux.fromIterable(response.getRepaymentSchedule().getPeriods())
                      .filter(
                          period -> {
                            LocalDate dueDate = DateTimeUtil.parseDate(period.getDueDate());
                            return !CommonUtil.nullOrEmpty(dueDate)
                                && dueDate.isAfter(LocalDate.now())
                                && !Boolean.TRUE.equals(period.getComplete());
                          })
                      .next()
                      .map(
                          period -> {
                            LocalDate date = DateTimeUtil.parseDate(period.getDueDate());
                            return DateTimeUtil.formatDate(date, DD_MM_YYYY);
                          });

              return nextDueDateMono
                  .map(nextDueDate -> new RPSInfo(nextDueDate, response.getDpdDays()))
                  .switchIfEmpty(Mono.just(new RPSInfo(null, response.getDpdDays())));
            });
  }

  private void setLastPaymentFromTransactions(
      LoanDetailsDTO loan, List<SimplifiedTransactionResponse.Transaction> transactions) {
    if (CommonUtil.nullOrEmpty(transactions)) return;

    SimplifiedTransactionResponse.Transaction latest = transactions.get(0);
    loan.setLastPaymentDone(
        LoanDetailsDTO.LastPaymentDone.builder()
            .date(latest.getDate())
            .amount(latest.getAmount())
            .build());
  }

  public Mono<DueDetailsResponse> getNextDueAmount(String loanAccountNumber, String nextDueDate) {
    return lmsApi.fetchDueAsOnDate(loanAccountNumber, nextDueDate);
  }

  public Mono<RPSResponseWithDPD> getRPSWithDPD(String loanAccountNumber) {
    return lmsApi.fetchRPSWithDPD(loanAccountNumber);
  }

  public Flux<LoanDetailsDTO> getAllLoanDetails(String leadId, LoanStatus status) {
    return lmsApi
        .fetchAllLoansDetails(leadId)
        .switchIfEmpty(Mono.error(new NotFoundException("No loans found for leadId: " + leadId)))
        .map(loan -> {
          overrideLoanStatus(loan);
          return loan;
        })
        .filter(loan -> !isProductDisabled(loan)) // Filter out disabled products
        .filter(loan -> filterLoanByStatus(loan, status))
      .map(LoanDetailsUtil::mapLoanToDto)
      .switchIfEmpty(Flux.error(
        new NotFoundException("No loans left after filtering for leadId: " + leadId)));
  }

  private void overrideLoanStatus(LoanDetailsResponse loan) {
    if (loan == null || loan.getStatus() == null) return;

    int currentStatusCode = loan.getStatus().intValue();

    if (currentStatusCode == LoanStatus.WRITTEN_OFF.getCode()) {
      loan.setStatus(BigInteger.valueOf(LoanStatus.ACTIVE.getCode()));
    } else if (currentStatusCode == LoanStatus.OVERPAID.getCode()) {
      loan.setStatus(BigInteger.valueOf(LoanStatus.CLOSED.getCode()));
    }
  }

  private boolean filterLoanByStatus(LoanDetailsResponse loan, LoanStatus selectedStatus) {
    if (CommonUtil.nullOrEmpty(loan.getStatus())
        || BigInteger.valueOf(200).equals(loan.getStatus())) {
      return false; // exclude loan having status 200 or null
    }

    String statusName = LoanStatus.getStatusNameByCode(loan.getStatus().intValue());
    if (UNKNOWN_LOAN_STATUS.equalsIgnoreCase(statusName)) {
      return false; // exclude unknown statuses
    }

    if (selectedStatus == null) return true; // no filtering requested, allow

    return selectedStatus.name().equalsIgnoreCase(statusName);
  }

  private boolean isTagValueIncluded(String tagValue) {
    try {
      DocumentTagValue.valueOf(tagValue == null ? "null" : tagValue);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public Flux<LeadIdResponse> fetchLeadIdAgainstMobileNumberAndDOB(
      String mobileNumber, String dateOfBirth) {
    return losApi.fetchLeadDetailAgainstMobileNumberAndDOB(mobileNumber, dateOfBirth);
  }

  public Flux<LeadIdResponse> fetchLeadIdAgainstMobileNumberDOBAndPAN(
      String mobileNumber, String dateOfBirth, String panLast4Digits) {
    return losApi.fetchLeadDetailAgainstMobileNumberDOBAndPAN(
        mobileNumber, dateOfBirth, panLast4Digits);
  }

  public Mono<byte[]> getRPS(String loanAccountNumber, String leadId) {
    log.info(
        "[RPS] Generating RPS PDF for loanAccountNumber={}, leadId={}", loanAccountNumber, leadId);

    Mono<ClientDetailsCpResponseDto> loanDetails =
        losApi
            .getCpRpsLeadData(leadId, loanAccountNumber)
            .cast(ClientDetailsCpResponseDto.class)
            .flatMap(
                response -> {
                  if (response == null || response.getLoanAccountNumber() == null) {
                    log.warn(
                        "[RPS] No loan details found for leadId={}, accountNo={}",
                        leadId,
                        loanAccountNumber);
                    return Mono.error(
                        new NotFoundException(
                            "No loan details found for leadId="
                                + leadId
                                + ", accountNo="
                                + loanAccountNumber));
                  }
                  return Mono.just(response);
                });
    Mono<ResponseRpsDTO> responseRpsDTOMono =
        lmsApi
            .fetchRPS(loanAccountNumber)
            .doOnError(
                e ->
                    log.error(
                        "[RPS][fetchRPS] Failed for loanAccountNumber={}, error={}",
                        loanAccountNumber,
                        e.getMessage()));

    return Mono.zip(responseRpsDTOMono, loanDetails)
        .flatMap(
            tuple ->
                Mono.fromCallable(
                        () ->
                            rpsPdfBuilder.generatePdf(
                                tuple.getT1(), tuple.getT2(), loanAccountNumber))
                    .subscribeOn(Schedulers.boundedElastic()))
        .onErrorResume(
            e -> {
              log.error(
                  "[RPS] Failed to generate RPS PDF for loanAccountNumber={}, leadId={},"
                      + " error={}",
                  loanAccountNumber,
                  leadId,
                  e.getMessage());

              return Mono.error(
                  new ServerErrorException(
                      "Failed to generate RPS PDF",
                      e.getMessage(),
                      HttpStatus.INTERNAL_SERVER_ERROR));
            });
  }

  public Mono<?> getConsentByClientId(String clientID) {
    return lmsApi.getConsentByClientId(clientID);
  }

  public Mono<?> saveConsent(String clientID, ClientConsentDTO clientConsentDTO) {
    return lmsApi.saveConsentByClientId(clientID, clientConsentDTO);
  }
}
