package com.trillionloans.lms.controller;

import static com.trillionloans.lms.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.lms.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.lms.config.SecureInput;
import com.trillionloans.lms.model.dto.LoanForeClosureDTO;
import com.trillionloans.lms.model.dto.PartRepaymentRequestDTO;
import com.trillionloans.lms.model.dto.internal.ClientConsentDTO;
import com.trillionloans.lms.model.request.BulkForeclosureDetailsRequest;
import com.trillionloans.lms.model.request.CollectChargeRequest;
import com.trillionloans.lms.model.dto.internal.M2pCreditLineForeClosureDTO;
import com.trillionloans.lms.model.request.CreditLineMarkRepaymentRequest;
import com.trillionloans.lms.model.request.MarkCollectionRequest;
import com.trillionloans.lms.model.request.RejectLoanRequest;
import com.trillionloans.lms.model.request.SaveChargesRequest;
import com.trillionloans.lms.model.request.WaiveChargeRequest;
import com.trillionloans.lms.model.response.BulkForeclosureDetailsResponse;
import com.trillionloans.lms.model.response.ForeclosureDetailsResponseDto;
import com.trillionloans.lms.model.response.M2pExcessAmountResponse;
import com.trillionloans.lms.model.response.M2pFirstUnpaidTransaction;
import com.trillionloans.lms.model.response.M2pLanDetails;
import com.trillionloans.lms.model.response.RepaymentScheduleResponse;
import com.trillionloans.lms.model.response.TransactionDetailResponse;
import com.trillionloans.lms.service.ChargeService;
import com.trillionloans.lms.service.CollectionService;
import com.trillionloans.lms.util.CreditLineUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Controller class for handling collection-related operations.
 *
 * @author sofiyan
 */
@RestController
@RequestMapping("/partners/api/v1/collection")
@AllArgsConstructor
@Validated
@Tag(name = "Collection", description = "All the Operations related to collection")
public class CollectionController {

  /** Service for handling collection-related business logic. */
  private final CollectionService collectionService;

  private final ChargeService chargeService;
  private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");

  /**
   * Retrieves the repayment schedule for a given loan by Loan Account Number.
   *
   * @param loanAccountNumber The ID of the loan for which the repayment schedule is requested.
   * @return A Mono containing the first unpaid transaction response.
   */
  @Operation(
      summary = "Fetch Repayment Schedule",
      description =
          "This API fetches Repayment Schedule after loan gets disbursed by Loan Account Number",
      parameters = {@Parameter(name = "loanAccountNumber", required = true, example = "14")},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RepaymentScheduleResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/repaymentSchedule")
  public ResponseEntity<Mono<?>> getRepaymentScheduleByLoanAccountNumber(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(
        collectionService.getRepaymentScheduleByLoanAccountNumber(loanAccountNumber));
  }

  @Operation(
      summary = "Fetch ForeClosure Details",
      description =
          "This API fetches ForeClosure details after loan gets disbursed by Loan Account Number",
      parameters = {@Parameter(name = "loanAccountNumber", required = true, example = "14")},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ForeclosureDetailsResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/foreclosure-details")
  public ResponseEntity<Mono<?>> getForeclosureDetailsByLoanAccountNumber(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestParam(name = "transactionDate") String transactionDate,
      @RequestParam(name = "isTotalOutstandingInterest") Boolean isTotalOutstandingInterest,
      @RequestParam(name = "includePreClosureReason") Boolean includePreClosureReason,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(
        collectionService.getForeclosureDetailsByLoanAccountNumber(
            loanAccountNumber,
            transactionDate,
            isTotalOutstandingInterest,
            includePreClosureReason));
  }

  @Operation(
      summary = "Post ForeClosure Request Data",
      description =
          "This API raises request for ForeClosure after loan gets disbursed by Loan Account"
              + " Number",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LoanForeClosureDTO.class)),
      description = "Loan foreClosure data for execution")
  @PostMapping("/loanAccounts/{loanAccountNumber}/foreclosure-execute")
  public ResponseEntity<Mono<?>> postForeClosureRequest(
      @Valid @RequestBody LoanForeClosureDTO foreClosureData,
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(
        collectionService.postForeClosureRequest(loanAccountNumber, foreClosureData));
  }

  @Operation(
      summary = "Mark Collection API for Principal and Interest",
      description = "This API Mark collection for a loan against Loan Account Number",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = MarkCollectionRequest.class)),
      description = "Collection details needs to be provided in the request body.")
  @PostMapping("/loanAccounts/{loanAccountNumber}/markCollection")
  public ResponseEntity<Mono<?>> markCollection(
      @SecureInput @Valid @RequestBody MarkCollectionRequest markCollectionRequest,
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode,
      @RequestHeader(name = PARTNER_ID) String partnerId) {
    return ResponseEntity.ok(
        collectionService.markCollection(loanAccountNumber, markCollectionRequest, partnerId));
  }

  @Operation(
      summary = "Save charges",
      description = "This API save charges against Loan Account Number",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SaveChargesRequest.class)),
      description = "Collection details needs to be provided in the request body.")
  @PostMapping("/loanAccounts/{loanAccountNumber}/save-charges")
  public ResponseEntity<Mono<?>> savePenalty(
      @SecureInput @Valid @RequestBody SaveChargesRequest saveChargesRequest,
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(collectionService.saveCharges(loanAccountNumber, saveChargesRequest));
  }

  @Operation(
      summary = "Fetch Charges Details",
      description = "This API fetches Charges details by Loan Account Number",
      parameters = {@Parameter(name = "loanAccountNumber", required = true)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/getCharges")
  public ResponseEntity<Mono<?>> getCharges(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestParam(name = "exclude") String exclude,
      @RequestParam(name = "isFetchSpecificData") Boolean isFetchSpecificData,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(
        collectionService.getCharges(loanAccountNumber, exclude, isFetchSpecificData));
  }

  @Operation(
      summary = "Waive Charge",
      description = "This API Waive charges against Loan Account Number and Charge Identifier",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = WaiveChargeRequest.class)),
      description = "Collection details needs to be provided in the request body.")
  @PostMapping("/loanAccounts/{loanAccountNumber}/Charges/{chargeId}/waiveCharge")
  public ResponseEntity<Mono<?>> waiveCharge(
      @SecureInput @Valid @RequestBody WaiveChargeRequest waiveChargeRequest,
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @PathVariable(name = "chargeId") String chargeId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(
        collectionService.waiveCharge(loanAccountNumber, chargeId, waiveChargeRequest));
  }

  @Operation(
      summary = "Collect Charge",
      description = "This API use to collect charges against Loan Account Number and Charge Type",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CollectChargeRequest.class)),
      description = "Charge Collection details needs to be provided in the request body.")
  @PostMapping("/loanAccounts/{loanAccountNumber}/Charges/{chargeType}/collectCharge")
  public ResponseEntity<Mono<?>> collectCharge(
      @SecureInput @Valid @RequestBody CollectChargeRequest collectChargeRequest,
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @PathVariable(name = "chargeType") String chargeType,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(
        collectionService.collectCharge(loanAccountNumber, chargeType, collectChargeRequest));
  }

  @Operation(
      summary = "Fetch NOC PDF",
      description = "This API fetches NOC pdf by Loan Account Number",
      parameters = {@Parameter(name = "loanAccountNumber", required = true)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/noc")
  public Mono<ResponseEntity<ByteArrayResource>> fetchNOCPdf(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return collectionService
        .fetchNOCPdf(loanAccountNumber)
        .map(
            pdfBytes -> {
              ByteArrayResource resource = new ByteArrayResource(pdfBytes);
              return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(resource);
            });
  }

  @Operation(
      summary = "Fetch SOA PDF",
      description = "This API fetches SOA pdf by Loan Account Number and dateOfGeneration",
      parameters = {@Parameter(name = "loanAccountNumber", required = true)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/fetchSOA")
  public Mono<ResponseEntity<ByteArrayResource>> fetchSOAPdf(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestParam(name = "dateOfGeneration") String dateOfGeneration,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    if (!DATE_PATTERN.matcher(dateOfGeneration).matches()) {
      return Mono.error(
          new IllegalArgumentException("Invalid dateOfGeneration. Must be in YYYY-MM-DD format."));
    }
    return collectionService
        .fetchSOAPdf(loanAccountNumber, dateOfGeneration)
        .map(
            pdfBytes -> {
              ByteArrayResource resource = new ByteArrayResource(pdfBytes);
              return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(resource);
            });
  }

  @Operation(
      summary = "Reject/Cancel Loan",
      description = "This API use to Reject/cancel loan against Loan Account Number",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = RejectLoanRequest.class)),
      description = "Charge Collection details needs to be provided in the request body.")
  @PostMapping("/loanAccounts/{loanAccountNumber}/reject")
  public ResponseEntity<Mono<?>> rejectLoan(
      @SecureInput @RequestBody RejectLoanRequest rejectLoanRequest,
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(collectionService.rejectLoan(loanAccountNumber, rejectLoanRequest));
  }

  @Operation(
      summary = "Fetch lead,lan and loan details",
      description = "This API fetches lead, lan and loan ids mapped against given external id",
      parameters = {@Parameter(name = "external_id", required = true)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loans/details")
  public ResponseEntity<Mono<Object>> getLoanDetails(
      @RequestParam("external_id") String externalIdOne) {
    return ResponseEntity.ok(collectionService.getLoanDetails(externalIdOne));
  }

  @Operation(
      summary = "Fetch transaction details of loan id",
      description = "This API fetches the transaction details of loan id",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loan/{loanId}/transaction-details")
  public ResponseEntity<Mono<TransactionDetailResponse>> getTransactionDetails(
      @PathVariable("loanId") String loanId) {
    return ResponseEntity.ok(collectionService.getLoanTransactionDetails(loanId));
  }

  @Operation(
      summary = "Mark Partial Collection API",
      description = "This API Marks partial collection for a loan against Loan Account Number",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = PartRepaymentRequestDTO.class)),
      description = "Collection details needs to be provided in the request body.")
  @PostMapping("/loanAccounts/{loanAccountNumber}/part-payment")
  public ResponseEntity<Mono<?>> markPartialCollection(
      @Valid @RequestBody PartRepaymentRequestDTO markCollectionRequest,
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode,
      @RequestHeader(name = PARTNER_ID) String partnerId) {
    return ResponseEntity.ok(
        collectionService.markPartialCollection(
            loanAccountNumber, markCollectionRequest, partnerId));
  }

  @Operation(
      summary = "Fetch summary  of loan id",
      description = "This API fetches summary of loan id",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loan/{loanId}/summary")
  public ResponseEntity<Mono<?>> getLoanSummary(@PathVariable("loanId") String loanId) {
    return ResponseEntity.ok(collectionService.getLoanSummary(loanId));
  }

  /**
   * Retrieves the repayment schedule with Dpd for a given loan by Loan Account Number.
   *
   * @param loanAccountNumber The ID of the loan for which the repayment schedule is requested.
   * @return A Mono containing the repayment schedule response.
   */
  @Operation(
      summary = "Fetch Repayment Schedule",
      description =
          "This API fetches Repayment Schedule with DPD after loan gets disbursed by Loan Account"
              + " Number",
      parameters = {@Parameter(name = "loanAccountNumber", required = true, example = "14")},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RepaymentScheduleResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/repayment-schedule-with-dpd")
  public ResponseEntity<Mono<?>> getRpsWithDpdByLoanAccountNumber(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(collectionService.getRpsWithDpdByLoanAccountNumber(loanAccountNumber));
  }

  /**
   * Retrieves the due as on date details
   *
   * @param loanAccountNumber The ID of the loan for which the repayment schedule is requested.
   * @param date the date at which the due is to be fetched
   * @return A Mono containing the repayment schedule response.
   */
  @Operation(
      summary = "Fetch Due amount as on date",
      description =
          "This API Fetch Due amount as on date after loan gets disbursed by Loan Account Number",
      parameters = {
        @Parameter(name = "loanAccountNumber", required = true, example = "14"),
        @Parameter(name = "date", required = true, example = "01-01-2025")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RepaymentScheduleResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/fetch-due/as-on-date/{date}")
  public ResponseEntity<Mono<?>> fetchDueAsOnDate(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @PathVariable(name = "date") String date,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(collectionService.fetchDueAsOnDate(loanAccountNumber, date));
  }

  /**
   * Retrieves all loans the due as on date details
   *
   * @param date the date at which the due is to be fetched
   * @return A Mono containing the repayment schedule response.
   */
  @Operation(
      summary = "Fetch all loans Due amount as on date",
      description =
          "This API Fetch Due amount as on date after loan gets disbursed by Loan Account Number",
      parameters = {
        @Parameter(name = "loanAccountNumber", required = true, example = "14"),
        @Parameter(name = "date", required = true, example = "01-01-2025")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RepaymentScheduleResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/fetch-due/all-loans/as-on-date/{date}")
  public ResponseEntity<Mono<?>> fetchAllLoansDueAsOnDate(
      @PathVariable(name = "date") String date,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(collectionService.fetchAllLoansDueAsOnDate(date));
  }

  @Operation(
      summary = "Fetch loans against lead",
      description = "This API Fetches all the loan details against a lead",
      parameters = {@Parameter(name = "leadId", required = true, example = "14")},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("{leadId}/loan/details")
  public ResponseEntity<Mono<Object>> getAllLoanDetails(@PathVariable("leadId") String leadId) {
    return ResponseEntity.ok(collectionService.getAllLoanDetailsAgainstLead(leadId));
  }

  @Operation(
      summary = "Fetch Charge Details by Charge external Id",
      description = "This API fetches Charge details by Charge external Id",
      parameters = {@Parameter(name = "chargeExternalId", required = true)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loan-charges/{chargeExternalId}")
  public ResponseEntity<Mono<?>> getChargeByExternalId(
      @PathVariable(name = "chargeExternalId")
          @Size(max = 100, message = "[FetchCharge] chargeExternalId exceeds max length of 100")
          String chargeExternalId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {

    String trimmedExternalId = chargeExternalId != null ? chargeExternalId.trim() : null;
    return ResponseEntity.ok(collectionService.getChargeByExternalId(trimmedExternalId));
  }

  @Operation(
      summary = "Post bounce penal charges",
      description = "",
      responses = {
        @ApiResponse(
            responseCode = "202",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @PostMapping("/penal-charges")
  public Mono<ResponseEntity<Map<String, String>>> postPenalCharges(
      @RequestParam(name = "runDate", required = false) String runDate // "yyyy-MM-dd"
      ) {
    return Mono.deferContextual(
        ctx -> {
          chargeService
              .startChargesRun(runDate)
              .subscribeOn(Schedulers.boundedElastic())
              .contextWrite(ctx) // propagate traceId
              .subscribe();

          return Mono.just(
              ResponseEntity.status(202)
                  .body(
                      Map.of(
                          "message",
                          "Accepted. Processing started for runDate="
                              + (runDate == null ? "today" : runDate))));
        });
  }

  @Operation(
      summary = "Fetch Latest Consent by ClientID",
      description = "Fetch Latest Consent by ClientID",
      parameters = {@Parameter(name = "clientId", required = true)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/getConsent/{clientId}")
  public ResponseEntity<Mono<?>> getConsentByClientId(
      @PathVariable(name = "clientId") String clientId) {
    return ResponseEntity.ok(collectionService.getConsentByClientId(clientId));
  }

  @Operation(
      summary = "Save Consent by Client Id",
      description = "This API save Consent for a Client",
      parameters = {@Parameter(name = "clientId", required = true)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @PostMapping("/saveConsent/{clientId}")
  public ResponseEntity<Mono<?>> saveConsentByClientId(
      @Valid @RequestBody ClientConsentDTO clientConsentDTO,
      @PathVariable(name = "clientId") String clientId) {
    return ResponseEntity.ok(collectionService.saveConsentByClientId(clientId, clientConsentDTO));
  }

  @Operation(
      summary = "get charges against a loan account",
      description = "get charges against a loan account",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/charge-details")
  public Mono<ResponseEntity<Mono<?>>> getChargeDetails(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber) {
    return Mono.just(ResponseEntity.ok(chargeService.getCharges(loanAccountNumber)));
  }

  /**
   * Retrieves the due as on date details
   *
   * @param loanAccountNumber The ID of the loan for which the repayment schedule is requested.
   * @param date the date at which the due is to be fetched
   * @return A Mono containing the repayment schedule response.
   */
  @Operation(
      summary = "Fetch Due amount as on date",
      description =
          "This API Fetch Due amount as on date after loan gets disbursed by Loan Account Number",
      parameters = {
        @Parameter(name = "loanAccountNumber", required = true, example = "14"),
        @Parameter(name = "date", required = true, example = "01-01-2025")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RepaymentScheduleResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/get-lan-details")
  public ResponseEntity<Mono<List<M2pLanDetails>>> getLanDetails(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(collectionService.getLanDetails(loanAccountNumber));
  }

  /**
   * Retrieves the due as on date details
   *
   * @param loanAccountNumber The ID of the loan for which the repayment schedule is requested.
   * @param date the date at which the due is to be fetched
   * @return A Mono containing the repayment schedule response.
   */
  @Operation(
      summary = "Fetch Due amount as on date",
      description =
          "This API Fetch Due amount as on date after loan gets disbursed by Loan Account Number",
      parameters = {
        @Parameter(name = "loanAccountNumber", required = true, example = "14"),
        @Parameter(name = "date", required = true, example = "01-01-2025")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = RepaymentScheduleResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/get-first-unpaid-installment")
  public ResponseEntity<Mono<List<M2pFirstUnpaidTransaction>>> getFirstUnpaidInstallment(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(collectionService.getFirstUnpaidInstallmentDate(loanAccountNumber));
  }

  /**
   * @param loanAccountNumber The ID of the loan for which the excess amount is fetched
   * @return A Mono containing the excess amount response along with lan
   */
  @Operation(
      summary = "Fetch total excess amount along with loan account number",
      description = "This API fetch total excess amount along with loan account number",
      parameters = {
        @Parameter(name = "loanAccountNumber", required = true, example = "14"),
        @Parameter(name = "date", required = true, example = "01-01-2025")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = M2pExcessAmountResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/loanAccounts/{loanAccountNumber}/get-excess-amount")
  public ResponseEntity<Mono<List<M2pExcessAmountResponse>>> getExcessAmount(
      @PathVariable(name = "loanAccountNumber") String loanAccountNumber,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(collectionService.getExcessAmount(loanAccountNumber));
  }

  @Operation(
      summary = "Mark Repayment API for Credit Line",
      description = "This API Mark repayment for a Credit Line loan against Line Id",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = MarkCollectionRequest.class)),
      description = "Repayment details needs to be provided in the request body.")
  @PostMapping("/credit-line/{lineId}/markRepayment")
  public ResponseEntity<Mono<?>> markRepayment(
      @SecureInput @Valid @RequestBody CreditLineMarkRepaymentRequest markRepaymentRequest,
      @PathVariable(name = "lineId") String lineId,
      @RequestHeader(name = PRODUCT_CODE) String productCode,
      @RequestHeader(name = PARTNER_ID) String partnerId) {
    return ResponseEntity.ok(
        collectionService.markRepayment(lineId, markRepaymentRequest, productCode));
  }

  @GetMapping("/credit-line/{lineId}/repayment-transactions")
  public Flux<CreditLineUtil.RepaymentGroupResponse> getRepaymentTransactions(
      @PathVariable String lineId,
      @RequestParam(required = false) String drawdownId,
      @RequestParam(required = false) String transactionId,
      @RequestHeader(name = PRODUCT_CODE) String productCode,
      @RequestHeader(name = PARTNER_ID) String partnerId) {

    return collectionService.getRepaymentTransactions(
        lineId, drawdownId, transactionId, productCode);
  }

  @GetMapping("/credit-line/{lineId}/transactions")
  public ResponseEntity<Flux<?>> getAllTransactionDetails(
      @PathVariable(name = "lineId") String lineId,
      @RequestParam(name = "active", defaultValue = "false") boolean activeTxn,
      @RequestHeader(name = PRODUCT_CODE) String productCode,
      @RequestHeader(name = PARTNER_ID) String partnerId) {

    return ResponseEntity.ok(
        collectionService.getTransactionsDetails(lineId, productCode, activeTxn));
  }

  @Operation(
      summary = "Fetch ForeClosure Details",
      description =
          "This API fetches ForeClosure details after Credit Line loan gets disbursed by Loan"
              + " Account Number and transactionId",
      parameters = {
        @Parameter(name = "loanAccountNumber", required = true, example = "14"),
        @Parameter(name = "transactionId", required = true, example = "14")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ForeclosureDetailsResponseDto.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/credit-line/{lineId}/foreclosure-details/{transactionId}")
  public ResponseEntity<Mono<?>> getForeclosureDetails(
      @PathVariable(name = "lineId") String lineId,
      @PathVariable(name = "transactionId") String transactionId,
      @RequestParam(name = "transactionDate", required = false) String transactionDate,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(
        collectionService.getForeclosure(lineId, transactionId, transactionDate));
  }

  @Operation(
      summary = "Fetch Bulk ForeClosure Details",
      description =
          "This API fetches ForeClosure details for multiple transactions in a Credit Line",
      parameters = {@Parameter(name = "lineId", required = true, example = "14")},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BulkForeclosureDetailsResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @PostMapping("/credit-line/{lineId}/bulk-foreclosure-details")
  public ResponseEntity<Flux<BulkForeclosureDetailsResponse>> getBulkForeclosureDetails(
      @PathVariable(name = "lineId") String lineId,
      @Valid @RequestBody BulkForeclosureDetailsRequest request,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(
        collectionService.getBulkForeclosureDetails(
            lineId, request.getTransactionIds(), request.getTransactionDate()));
  }

  @Operation(
      summary = "Post Credit Line ForeClosure Request Data",
      description =
          "This API raises request for ForeClosure after Credit Line loan gets disbursed by Loan"
              + " Account and transactionId Number",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = M2pCreditLineForeClosureDTO.class)),
      description = "Loan foreClosure data for execution")
  @PostMapping("/credit-line/{lineId}/credit-line-foreclosure-execute/{transactionId}")
  public ResponseEntity<Mono<?>> postCreditLineForeClosureRequest(
      @Valid @RequestBody M2pCreditLineForeClosureDTO foreClosureData,
      @PathVariable(name = "lineId") String lineId,
      @PathVariable(name = "transactionId") String transactionId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return ResponseEntity.ok(
        collectionService.postCreditLineForeClosureRequest(
            lineId, transactionId, foreClosureData));
  }
}
