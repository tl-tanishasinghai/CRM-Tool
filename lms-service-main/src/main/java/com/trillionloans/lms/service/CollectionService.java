package com.trillionloans.lms.service;

import static com.trillionloans.lms.constant.StringConstants.CLIENT_CONSENT;
import static com.trillionloans.lms.constant.StringConstants.TRACE_ID;

import com.trillionloans.lms.api.m2p.M2PApi;
import com.trillionloans.lms.constant.ResponseStatus;
import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.model.dto.LoanForeClosureDTO;
import com.trillionloans.lms.model.dto.M2pLoanForeClosureDTO;
import com.trillionloans.lms.model.dto.M2pPartRepaymentRequestDTO;
import com.trillionloans.lms.model.dto.PartRepaymentRequestDTO;
import com.trillionloans.lms.model.dto.ResponseDTO;
import com.trillionloans.lms.model.dto.internal.ClientConsentDTO;
import com.trillionloans.lms.model.dto.internal.M2pCreditLineForeClosureDTO;
import com.trillionloans.lms.model.request.*;
import com.trillionloans.lms.model.response.BulkForeclosureDetailsResponse;
import com.trillionloans.lms.model.response.CreditLineMarkRepaymentResponse;
import com.trillionloans.lms.model.response.ForeclosureDetailsResponseDto;
import com.trillionloans.lms.model.response.M2pExcessAmountResponse;
import com.trillionloans.lms.model.response.M2pFirstUnpaidTransaction;
import com.trillionloans.lms.model.response.M2pLanDetails;
import com.trillionloans.lms.model.response.TransactionDetailResponse;
import com.trillionloans.lms.service.db.ClientConsentService;
import com.trillionloans.lms.util.CreditLineUtil;
import com.trillionloans.lms.util.M2pCreditLineForeClosureMapper;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service class for handling collection-related business logic.
 *
 * @author sofiyan
 */
@Service
@Slf4j
public class CollectionService {

  private final M2PApi m2PApi;
  private final ClientConsentService clientConsentService;
  private final CreditLineRepaymentService creditLineRepaymentService;

  private static final String REJECT_LOAN_DATE_FORMAT = "dd MMMM yyyy";
  private static final String STANDARD_DATE_FORMAT = "dd-MM-yyyy";

  /**
   * Constructor for initializing the CollectionService with M2PApi.
   *
   * @param m2PApi The M2P API for fetching repayment schedule data.
   */
  public CollectionService(
      M2PApi m2PApi,
      ClientConsentService clientConsentService,
      CreditLineRepaymentService creditLineRepaymentService) {
    this.m2PApi = m2PApi;
    this.clientConsentService = clientConsentService;
    this.creditLineRepaymentService = creditLineRepaymentService;
  }

  /**
   * Retrieves the repayment schedule for a given loan ID using the M2P API.
   *
   * @param loanAccountNumber The ID of the loan for which the repayment schedule is requested.
   * @return A Mono containing the repayment schedule data.
   */
  public Mono<?> getRepaymentScheduleByLoanAccountNumber(String loanAccountNumber) {
    return m2PApi.getRepaymentScheduleByLoanAccountNumber(loanAccountNumber);
  }

  /**
   * Retrieves foreclosure details for a given loan account number. This method interacts with the
   * M2P API to fetch the foreclosure details based on the provided loan account number and
   * transaction date. It also allows the caller to specify whether the total outstanding interest
   * should be included and whether pre-closure reasons should be included. If the
   * `netForeclosureAmount` is not present (null), it defaults to the value of `amount`. If `amount`
   * is also null, it sets `netForeclosureAmount` to 0.00.
   *
   * @param loanAccountNumber The unique identifier of the loan account.
   * @param transactionDate The date of the transaction for which the foreclosure details are
   *     needed.
   * @param isTotalOutstandingInterest A flag indicating whether to include the total outstanding
   *     interest in the response.
   * @param includePreClosureReason A flag indicating whether to include the pre-closure reasons in
   *     the response.
   * @return A {@link Mono} containing the {@link ForeclosureDetailsResponseDto} with the
   *     foreclosure details.
   */
  public Mono<ForeclosureDetailsResponseDto> getForeclosureDetailsByLoanAccountNumber(
      String loanAccountNumber,
      String transactionDate,
      Boolean isTotalOutstandingInterest,
      Boolean includePreClosureReason) {
    return m2PApi
        .getForeclosureDetailsByLoanAccountNumber(
            loanAccountNumber, transactionDate, isTotalOutstandingInterest, includePreClosureReason)
        .flatMap(
            it -> {
              if (it.getNetForeclosureAmount() == null) {
                it.setNetForeclosureAmount(Optional.ofNullable(it.getAmount()).orElse(0.00));
              }
              return Mono.just(it);
            });
  }

  public Mono<?> postForeClosureRequest(
      String loanAccountNumber, LoanForeClosureDTO foreClosureData) {
    M2pLoanForeClosureDTO m2pLoanForeClosureDTO = getM2pLoanForeClosureDTO(foreClosureData);
    return m2PApi.postForeClosureRequest(loanAccountNumber, m2pLoanForeClosureDTO);
  }

  /**
   * Marks a collection for the specified loan account using the provided request details. This
   * method sets standard fields in the {@link MarkCollectionRequest} and interacts with the partner
   * master service and M2P API to perform the collection marking.
   *
   * @param loanAccountNumber The loan account number for which the collection is to be marked.
   * @param markCollectionRequest The request object containing the details needed to mark the
   *     collection.
   * @param partnerId The ID of the partner making the request.
   * @return A {@link Mono} that emits the result of the collection marking operation or completes
   *     empty if the operation is not performed.
   */
  public Mono<?> markCollection(
      String loanAccountNumber, MarkCollectionRequest markCollectionRequest, String partnerId) {
    markCollectionRequest.setDateFormat(STANDARD_DATE_FORMAT);
    markCollectionRequest.setTimeFormat("dd-MM-yyyy'T'HH:mm:ssZ");
    markCollectionRequest.setLocale("en");
    markCollectionRequest.setExternalId(null);

    if (Objects.nonNull(markCollectionRequest.getReceiptNumber())) {
      markCollectionRequest.setExternalId(
          markCollectionRequest.getReceiptNumber() + "_" + loanAccountNumber);
    }
    return m2PApi.markCollection(loanAccountNumber, markCollectionRequest);
  }

  public Mono<?> saveCharges(String loanAccountNumber, SaveChargesRequest saveChargesRequest) {
    /** HardCoding 2 M2P fields 1. dateFormat 2. locale */
    saveChargesRequest.setDateFormat(STANDARD_DATE_FORMAT);
    saveChargesRequest.setLocale("en");
    return m2PApi.saveCharges(loanAccountNumber, saveChargesRequest);
  }

  public Mono<?> getCharges(String loanAccountNumber, String exclude, Boolean isFetchSpecificData) {
    return m2PApi.getCharges(loanAccountNumber, exclude, isFetchSpecificData);
  }

  public Mono<?> waiveCharge(
      String loanAccountNumber, String chargeId, WaiveChargeRequest waiveChargeRequest) {
    /** HardCoding 2 M2P fields 1. dateFormat 2. locale */
    waiveChargeRequest.setDateFormat(STANDARD_DATE_FORMAT);
    waiveChargeRequest.setLocale("en");
    return m2PApi.waiveCharge(loanAccountNumber, chargeId, waiveChargeRequest);
  }

  public Mono<?> collectCharge(
      String loanAccountNumber, String chargeType, CollectChargeRequest collectChargeRequest) {
    /*
     * HardCoding 2 M2P fields
     * 1. dateFormat
     * 2. locale
     */
    collectChargeRequest.setDateFormat(STANDARD_DATE_FORMAT);
    collectChargeRequest.setLocale("en");
    return m2PApi.collectCharge(loanAccountNumber, chargeType, collectChargeRequest);
  }

  public Mono<byte[]> fetchSOAPdf(String loanAccountNumber, String dateOfGeneration) {
    return m2PApi.fetchSOAPdf(loanAccountNumber, dateOfGeneration);
  }

  public Mono<Object> getAllLoanDetailsAgainstLead(String leadId) {
    return m2PApi.getAllLoanDetailsAgainstLead(leadId);
  }

  public Mono<?> rejectLoan(String loanAccountNumber, RejectLoanRequest rejectLoanRequest) {
    /*
     * HardCoding 2 M2P fields
     * 1. dateFormat
     * 2. locale
     */
    rejectLoanRequest.setDateFormat(REJECT_LOAN_DATE_FORMAT);
    rejectLoanRequest.setLocale("en");
    return m2PApi.rejectLoan(loanAccountNumber, rejectLoanRequest);
  }

  private M2pLoanForeClosureDTO getM2pLoanForeClosureDTO(LoanForeClosureDTO foreClosureData) {
    return M2pLoanForeClosureDTO.builder()
        .dateFormat(STANDARD_DATE_FORMAT)
        .locale("en")
        .note(foreClosureData.getNote())
        .preClosureReasonId(foreClosureData.getPreClosureReasonId())
        .transactionAmount(foreClosureData.getTransactionAmount())
        .transactionDate(foreClosureData.getTransactionDate())
        .paymentTypeId(foreClosureData.getPaymentTypeId())
        .interestWaiverAmount(foreClosureData.getInterestWaiverAmount())
        .receiptNumber(foreClosureData.getReceiptNumber())
        .chargeDiscountDetails(foreClosureData.getChargeDiscountDetails())
        .waiveCharges(foreClosureData.getWaiveCharges())
        .build();
  }

  public Mono<byte[]> fetchNOCPdf(String loanAccountNumber) {
    return m2PApi.fetchNOCPdf(loanAccountNumber);
  }

  public Mono<Object> getLoanDetails(String externalIdOne) {
    return m2PApi.getLoanDetails(externalIdOne);
  }

  public Mono<TransactionDetailResponse> getLoanTransactionDetails(String loanId) {
    return m2PApi.getLoanTransactionDetails(loanId);
  }

  public Mono<?> markPartialCollection(
      String loanAccountNumber, PartRepaymentRequestDTO markCollectionRequest, String partnerId) {
    M2pPartRepaymentRequestDTO m2pPartRepaymentRequestDTO =
        M2pPartRepaymentRequestDTO.getM2pRequestFormatFromDTO(markCollectionRequest);
    return m2PApi.markPartialCollection(loanAccountNumber, m2pPartRepaymentRequestDTO);
  }

  public Mono<?> getLoanSummary(String loanAccountNumber) {
    return m2PApi.getLoanSummaryByLoanAccountNumber(loanAccountNumber);
  }

  public Mono<?> getRpsWithDpdByLoanAccountNumber(String loanAccountNumber) {
    return m2PApi.getRpsWithDpdByLoanAccountNumber(loanAccountNumber);
  }

  /**
   * Retrieves the due amount for a given loan ID using the M2P API.
   *
   * @param loanAccountNumber The ID of the loan for which the repayment schedule is requested.
   * @param date The date for which the due amount is requested.
   * @return A Mono containing the repayment schedule data.
   */
  public Mono<?> fetchDueAsOnDate(String loanAccountNumber, String date) {
    return m2PApi.fetchDueAsOnDate(loanAccountNumber, date);
  }

  /**
   * Retrieves all the loans due amount using the M2P API.
   *
   * @param date The date for which the due amount is requested.
   * @return A Mono containing the repayment schedule data.
   */
  public Mono<?> fetchAllLoansDueAsOnDate(String date) {
    return m2PApi.fetchAllLoansDueAsOnDate(date);
  }

  /**
   * Fetches charge details by external charge ID using the M2P API.
   *
   * @param chargeExternalId The external ID of the charge to retrieve.
   * @return A Mono containing the charge details response.
   */
  public Mono<?> getChargeByExternalId(String chargeExternalId) {
    return m2PApi.fetchChargeByExternalId(chargeExternalId);
  }

  public Mono<?> getConsentByClientId(String clientId) {
    return clientConsentService
        .findTopByClientIdOrderByCreatedAtDesc(clientId)
        .map(Object.class::cast)
        .switchIfEmpty(Mono.just(Collections.emptyMap()));
  }

  public Mono<?> saveConsentByClientId(String clientId, ClientConsentDTO clientConsentDTO) {
    return clientConsentService
        .save(clientId, clientConsentDTO)
        .map(
            saved -> {
              log.info(
                  "[{}] Consent submitted successfully, ID: {}", CLIENT_CONSENT, saved.getId());
              return ResponseDTO.builder()
                  .status(ResponseStatus.SUCCESS)
                  .traceId(MDC.get(TRACE_ID))
                  .message("Consent submitted successfully")
                  .data(saved.getId())
                  .build();
            })
        .onErrorResume(
            error -> {
              log.error("[{}] error while saving consent", CLIENT_CONSENT);
              return Mono.error(
                  new BaseException(
                      "error saving consent",
                      "error while saving consent",
                      HttpStatus.BAD_REQUEST));
            });
  }

  /**
   * Retrieves the LAN details for a given loan ID using the M2P API.
   *
   * @param lan The ID of the loan for which the repayment schedule is requested.
   * @return A Mono containing the repayment schedule data.
   */
  public Mono<List<M2pLanDetails>> getLanDetails(String lan) {
    return m2PApi.getLanDetails(lan);
  }

  public Mono<List<M2pFirstUnpaidTransaction>> getFirstUnpaidInstallmentDate(String lan) {
    return m2PApi.getFirstUnpaidInstallmentDate(lan);
  }

  public Mono<List<M2pExcessAmountResponse>> getExcessAmount(String lan) {
    return m2PApi.getExcessAmount(lan);
  }

  public Mono<CreditLineMarkRepaymentResponse> markRepayment(
      String lineId, CreditLineMarkRepaymentRequest markRepaymentRequest, String productCode) {
    return creditLineRepaymentService.markRepayment(lineId, markRepaymentRequest, productCode);
  }

  public Flux<CreditLineUtil.RepaymentGroupResponse> getRepaymentTransactions(
      String lineId, String drawdownId, String transactionId, String productCode) {
    return creditLineRepaymentService.getRepaymentTransactions(
        lineId, drawdownId, transactionId, productCode);
  }

  public Flux<?> getTransactionsDetails(String lineId, String productCode, boolean activeTxns) {
    return creditLineRepaymentService.getTransactionsDetails(lineId, productCode, activeTxns);
  }

  public Mono<?> getForeclosure(String lineId, String transactionId, String transactionDate) {
    return m2PApi.getForeclosure(lineId, transactionId, transactionDate);
  }

  /**
   * Retrieves foreclosure details for multiple transactions in a credit line.
   *
   * @param lineId The credit line ID.
   * @param transactionIds The list of transaction IDs to fetch foreclosure details for.
   * @param transactionDate Optional transaction date to apply to all requests.
   * @return A Flux containing the foreclosure details for each transaction.
   */
  public Flux<BulkForeclosureDetailsResponse> getBulkForeclosureDetails(
      String lineId, List<String> transactionIds, String transactionDate) {
    return Flux.fromIterable(transactionIds)
        .flatMap(
            transactionId ->
                m2PApi
                    .getForeclosure(lineId, transactionId, transactionDate)
                    .map(
                        details ->
                            BulkForeclosureDetailsResponse.builder()
                                .transactionId(transactionId)
                                .foreclosureDetails(details)
                                .build())
                    .onErrorResume(
                        e ->
                            Mono.just(
                                BulkForeclosureDetailsResponse.builder()
                                    .transactionId(transactionId)
                                    .error(e.getMessage())
                                    .build())));
  }

  public Mono<?> postCreditLineForeClosureRequest(
      String lineId, String transactionId, M2pCreditLineForeClosureDTO foreClosureData) {
    return m2PApi.postForeClosureCreditLineRequest(
        lineId, transactionId, M2pCreditLineForeClosureMapper.toM2pPayload(foreClosureData));
  }
}
