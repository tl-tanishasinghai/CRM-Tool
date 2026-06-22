package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.constant.ResponseStatus;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.model.request.CreateBatchRequest;
import com.trillionloans.los.model.response.*;
import com.trillionloans.los.service.disbursal.NexusManualDisbursalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * controller for reverse feed operations. handles bank response file uploads for manual disbursal
 * reconciliation.
 */
@RestController
@RequestMapping("/api/v1/disbursal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reverse Feed", description = "APIs for reverse feed processing")
public class NexusManualDisbursalController {

  private final NexusManualDisbursalService nexusManualDisbursalService;

  private static final String USER_ID_HEADER = "X-User-Id";
  private static final String DEFAULT_USER = "system";
  private static final String XLSX = ".xlsx";
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
  private static final String DATE_FORMAT_MESSAGE =
      "Invalid date format for '%s'. Expected format: DD-MM-YYYY (e.g., 01-01-2025)";
  private static final String INVALID_DATE_FORMAT_MESSAGE =
      "Invalid date format for '%s'. Expected format: DD-MM-YYYY (e.g., 01-01-2025)";
  private static final String FROM_DATE_AFTER_TO_DATE_MESSAGE =
      "'from_date' cannot be greater than 'to_date'";

  @Operation(
      summary = "Upload reverse feed batch",
      description =
          "Upload bank response Excel file for processing. "
              + "Expected columns: Transaction Date, Loan Account Number, Transaction Status, "
              + "Transaction Rejection Reason, UTR Reference Number")
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "Accepted - File uploaded and processing started",
        content = @Content(schema = @Schema(implementation = ReverseFeedUploadResponseDTO.class))),
    @ApiResponse(
        responseCode = "400",
        description = "Bad Request - Invalid file format or empty file"),
    @ApiResponse(responseCode = "500", description = "Internal Server Error - Processing failed")
  })
  @PostMapping(value = "/batches/reverse-feed", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<ResponseEntity<ResponseDTO<ReverseFeedUploadResponseDTO>>> uploadReverseFeed(
      @RequestPart(value = "file", required = false) FilePart file,
      @RequestHeader(name = PRODUCT_CODE, required = false) String productCode,
      @RequestHeader(name = USER_ID_HEADER, required = false, defaultValue = DEFAULT_USER)
          String userId) {
    // validate file is present (return 400 before any use of file to avoid npe)
    // curl can send empty filenames in cases like: --form 'file=@""' / --form 'file=@'''
    String rawFilename = file == null ? null : file.filename();
    String filename =
        rawFilename == null ? null : rawFilename.trim().replace("\"", "").replace("'", "").trim();
    if (file == null || filename == null || filename.isEmpty()) {
      log.error(
          "[REVERSE_FEED] POST /batches/reverse-feed - file is mandatory but missing or empty");
      return Mono.error(
          new BaseException("file is mandatory", "file is mandatory", HttpStatus.BAD_REQUEST));
    }
    log.info("[REVERSE_FEED] POST /batches/reverse-feed - file: {}, user: {}", filename, userId);

    // validate file extension
    if (!filename.endsWith(XLSX) && !filename.endsWith(".xls")) {
      return Mono.error(
          new BaseException(
              "invalid file format. only .xlsx and .xls files are allowed.",
              null,
              HttpStatus.BAD_REQUEST));
    }

    return nexusManualDisbursalService
        .initiateReverseFeedUpload(file, userId)
        .map(
            response ->
                ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(
                        ResponseDTO.<ReverseFeedUploadResponseDTO>builder()
                            .status(ResponseStatus.SUCCESS)
                            .message(response.getMessage())
                            .traceId(MDC.get(TRACE_ID))
                            .data(response)
                            .build()));
  }

  @Operation(
      summary = "Get reverse feed batch history",
      description =
          "Get paginated history of reverse feed batches."
              + " Status accepts a comma-separated list (e.g., COMPLETED,FAILED)."
              + " If omitted, returns batches for all statuses."
              + " Date format: DD-MM-YYYY. If dates are omitted, defaults to last 1 year.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content =
            @Content(schema = @Schema(implementation = ReverseFeedBatchHistoryResponseDTO.class))),
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
  })
  @GetMapping("/batches/reverse-feed/history")
  public Mono<ResponseEntity<ResponseDTO<ReverseFeedBatchHistoryResponseDTO>>>
      getReverseFeedBatchHistory(
          @Parameter(
                  description =
                      "Batch statuses (e.g., COMPLETED,FAILED). Comma-separated list."
                          + " If omitted, returns batches for all statuses.",
                  example = "COMPLETED,FAILED")
              @RequestParam(required = false)
              List<String> status,
          @Parameter(
                  description = "Start date in DD-MM-YYYY format. Defaults to 1 year ago.",
                  example = "01-01-2025")
              @RequestParam(name = "from_date", required = false)
              String fromDateStr,
          @Parameter(
                  description = "End date in DD-MM-YYYY format. Defaults to today.",
                  example = "25-02-2026")
              @RequestParam(name = "to_date", required = false)
              String toDateStr,
          @Parameter(description = "Page number (0-based)", example = "0")
              @RequestParam(defaultValue = "0")
              int page,
          @Parameter(description = "Number of items per page", example = "10")
              @RequestParam(defaultValue = "10")
              int limit) {

    LocalDate fromDate = parseDate(fromDateStr, "from_date");
    LocalDate toDate = parseDate(toDateStr, "to_date");

    LocalDate effectiveToDate = toDate != null ? toDate : LocalDate.now();
    LocalDate effectiveFromDate = fromDate != null ? fromDate : effectiveToDate.minusYears(1);

    if (effectiveFromDate.isAfter(effectiveToDate)) {
      return Mono.error(
          new ClientSideException(
              FROM_DATE_AFTER_TO_DATE_MESSAGE,
              FROM_DATE_AFTER_TO_DATE_MESSAGE,
              HttpStatus.BAD_REQUEST));
    }

    LocalDateTime fromDateTime = effectiveFromDate.atStartOfDay();
    LocalDateTime toDateTime = effectiveToDate.atTime(LocalTime.MAX);

    log.info(
        "[REVERSE_FEED] GET /batches/reverse-feed/history - status: {}, from: {}, to: {}, page:"
            + " {}, limit: {}",
        status,
        fromDateTime,
        toDateTime,
        page,
        limit);

    if (page < 0 || limit < 0) {
      return Mono.error(
          new ClientSideException(
              "page and limit must be non-negative values",
              "page and limit must be non-negative values",
              HttpStatus.BAD_REQUEST));
    }

    return nexusManualDisbursalService
        .getReverseFeedBatchHistory(status, fromDateTime, toDateTime, page, limit)
        .map(
            response ->
                ResponseEntity.ok(
                    ResponseDTO.<ReverseFeedBatchHistoryResponseDTO>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("batch history fetched successfully")
                        .traceId(MDC.get(TRACE_ID))
                        .data(response)
                        .build()));
  }

  @Operation(
      summary = "Download reverse feed batch as Excel",
      description = "Download the reverse feed batch details as an Excel file")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Success - Excel file returned"),
    @ApiResponse(responseCode = "404", description = "Batch not found"),
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
  })
  @GetMapping("/batches/reverse-feed/{batchId}/download")
  public Mono<ResponseEntity<byte[]>> downloadBatch(
      @Parameter(description = "Batch ID (UUID)", required = true) @PathVariable String batchId) {
    log.info("[REVERSE_FEED] GET /batches/reverse-feed/{}/download", batchId);

    return nexusManualDisbursalService
        .generateBatchExcel(batchId)
        .map(
            bytes -> {
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(
                  MediaType.parseMediaType(
                      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
              headers.setContentDispositionFormData("attachment", "reverse-feed-" + batchId + XLSX);
              headers.setContentLength(bytes.length);

              return ResponseEntity.ok().headers(headers).body(bytes);
            });
  }

  @Operation(
      summary = "Get reverse feed batch status",
      description = "Get status of a reverse feed batch by batch ID")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content =
            @Content(schema = @Schema(implementation = ReverseFeedBatchStatusResponseDTO.class))),
    @ApiResponse(responseCode = "400", description = "Bad Request - Invalid batch ID format"),
    @ApiResponse(responseCode = "404", description = "Not Found - Batch not found"),
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
  })
  @GetMapping("/batches/reverse-feed/{batchId}/status")
  public Mono<ResponseEntity<ResponseDTO<ReverseFeedBatchStatusResponseDTO>>>
      getReverseFeedBatchStatus(
          @Parameter(description = "Batch ID (UUID)", required = true) @PathVariable
              String batchId) {
    log.info("[REVERSE_FEED] GET /batches/reverse-feed/{}/status", batchId);

    return nexusManualDisbursalService
        .getReverseFeedBatchStatus(batchId)
        .map(
            response ->
                ResponseEntity.ok(
                    ResponseDTO.<ReverseFeedBatchStatusResponseDTO>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("batch status fetched successfully")
                        .traceId(MDC.get(TRACE_ID))
                        .data(response)
                        .build()));
  }

  @Operation(
      summary = "Create Batch",
      description =
          "Create a batch with atomic transaction in LOS to lock loans and initialize hydration. "
              + "Returns immediately with batchId; processing happens in background.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "Accepted - Batch creation initiated, processing in background",
        content = @Content(schema = @Schema(implementation = CreateBatchResponse.class))),
    @ApiResponse(responseCode = "400", description = "Bad Request - Empty or invalid referenceIds"),
    @ApiResponse(responseCode = "500", description = "Internal Server Error - Processing failed")
  })
  @PostMapping("/batches/forward-feed")
  public Mono<ResponseEntity<ResponseDTO<CreateBatchResponse>>> createBatch(
      @SecureInput @Valid @RequestBody CreateBatchRequest createBatchRequest,
      @RequestHeader(name = PRODUCT_CODE, required = false) String productCode,
      @RequestHeader(name = USER_ID_HEADER, required = false, defaultValue = DEFAULT_USER)
          String userId) {
    log.info("[FORWARD_FEED] POST /batches/forward-feed - user: {}", userId);

    return nexusManualDisbursalService
        .createBatch(createBatchRequest, userId)
        .map(
            response ->
                ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(
                        ResponseDTO.<CreateBatchResponse>builder()
                            .status(ResponseStatus.SUCCESS)
                            .message(response.getMessage())
                            .traceId(MDC.get("traceId"))
                            .data(response)
                            .build()));
  }

  @Operation(
      summary = "Get Eligible loans for the batch",
      description =
          "Get all the eligible loans with MANUAL_INI status for the given product codes."
              + " Accepts multiple product codes as a comma-separated list."
              + " If omitted, returns loans for all remitX-enabled products.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = ManualQueueResponseDTO.class))),
    @ApiResponse(responseCode = "404", description = "Failed to get the Eligible loans"),
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
  })
  @GetMapping("/batches/forward-feed/manual-queue")
  public Mono<ResponseEntity<ResponseDTO<ManualQueueResponseDTO>>> getAllEligibleLoansForBatch(
      @Parameter(
              description =
                  "Product codes (e.g., CL,PL). Comma-separated list. If omitted, returns loans"
                      + " for all remitX-enabled products.",
              example = "CL,PL")
          @RequestParam(required = false)
          List<String> productCodes,
      @Parameter(description = "Page number (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Number of items per page", example = "10")
          @RequestParam(defaultValue = "10")
          int limit) {
    log.info(
        "[FORWARD_FEED] GET /batches/forward-feed/manual-queue - productCodes: {}, page: {},"
            + " limit: {}",
        productCodes,
        page,
        limit);

    if (page < 0 || limit < 0) {
      return Mono.error(
          new BaseException(
              "page and limit must be non-negative values", null, HttpStatus.BAD_REQUEST));
    }

    return nexusManualDisbursalService
        .getEligibleLoansForBatch(productCodes, page, limit)
        .map(
            response ->
                ResponseEntity.ok(
                    ResponseDTO.<ManualQueueResponseDTO>builder()
                        .status(ResponseStatus.SUCCESS)
                        .message("eligible loans fetched successfully")
                        .traceId(MDC.get("traceId"))
                        .data(response)
                        .build()));
  }

  @Operation(summary = "Get batch status", description = "Get batch status via batchId")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = BatchStatusResponse.class))),
    @ApiResponse(responseCode = "404", description = "Not Found"),
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
  })
  @GetMapping("/batches/forward-feed/{batchId}/status")
  public Mono<ResponseEntity<BatchStatusResponse>> getBatchStatus(
      @PathVariable(name = "batchId") UUID batchId) {
    log.info("[FORWARD_FEED] GET /batches/forward-feed/{}/status", batchId);
    return nexusManualDisbursalService.getBatchStatus(batchId).map(ResponseEntity::ok);
  }

  @Operation(
      summary = "Get list of batches",
      description =
          "Get batches filtered by status and date range with pagination."
              + " Status accepts a comma-separated list (e.g., COMPLETED,FAILED)."
              + " If omitted, returns batches for all statuses."
              + " Date format: DD-MM-YYYY. If dates are omitted, defaults to last 1 year.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = BatchHistoryResponse.class))),
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
  })
  @GetMapping("/batches/forward-feed/history")
  public Mono<ResponseEntity<BatchHistoryResponse>> getBatchHistory(
      @Parameter(
              description =
                  "Batch statuses (e.g., COMPLETED,FAILED,IN_PROGRESS). Comma-separated list."
                      + " If omitted, returns batches for all statuses.",
              example = "COMPLETED,FAILED")
          @RequestParam(required = false)
          List<String> status,
      @Parameter(
              description = "Start date in DD-MM-YYYY format. Defaults to 1 year ago.",
              example = "01-01-2025")
          @RequestParam(name = "from_date", required = false)
          String fromDateStr,
      @Parameter(
              description = "End date in DD-MM-YYYY format. Defaults to today.",
              example = "25-02-2026")
          @RequestParam(name = "to_date", required = false)
          String toDateStr,
      @Parameter(description = "Page number (0-based)", example = "0")
          @RequestParam(defaultValue = "0")
          int page,
      @Parameter(description = "Number of items per page", example = "10")
          @RequestParam(defaultValue = "10")
          int limit) {

    LocalDate fromDate = parseDate(fromDateStr, "from_date");
    LocalDate toDate = parseDate(toDateStr, "to_date");

    LocalDate effectiveToDate = toDate != null ? toDate : LocalDate.now();
    LocalDate effectiveFromDate = fromDate != null ? fromDate : effectiveToDate.minusYears(1);

    if (effectiveFromDate.isAfter(effectiveToDate)) {
      return Mono.error(
          new ClientSideException(
              FROM_DATE_AFTER_TO_DATE_MESSAGE,
              FROM_DATE_AFTER_TO_DATE_MESSAGE,
              HttpStatus.BAD_REQUEST));
    }

    LocalDateTime fromDateTime = effectiveFromDate.atStartOfDay();
    LocalDateTime toDateTime = effectiveToDate.atTime(LocalTime.MAX);

    log.info(
        "[FORWARD_FEED] GET /batches/forward-feed/history - status: {}, from: {}, to: {}, page:"
            + " {}, limit: {}",
        status,
        fromDateTime,
        toDateTime,
        page,
        limit);

    if (page < 0 || limit < 0) {
      return Mono.error(
          new ClientSideException(
              "page and limit must be non-negative values",
              "page and limit must be non-negative values",
              HttpStatus.BAD_REQUEST));
    }

    return nexusManualDisbursalService
        .getBatchHistory(status, fromDateTime, toDateTime, page, limit)
        .map(ResponseEntity::ok);
  }

  @Operation(
      summary = "Download batch as Excel",
      description =
          "Downloads an Excel file containing all disbursal registry entries for a COMPLETED batch")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Excel file downloaded successfully",
        content =
            @Content(
                mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
    @ApiResponse(responseCode = "400", description = "Batch is not in COMPLETED status"),
    @ApiResponse(responseCode = "404", description = "Batch not found"),
    @ApiResponse(responseCode = "500", description = "Internal Server Error")
  })
  @GetMapping(value = "/batches/forward-feed/{batchId}/download", produces = MediaType.ALL_VALUE)
  public Mono<ResponseEntity<?>> downloadBatchExcel(
      @Parameter(description = "Batch UUID", required = true) @PathVariable(name = "batchId")
          String batchIdStr) {
    log.info("[FORWARD_FEED] GET /batches/forward-feed/{}/download requested", batchIdStr);

    return nexusManualDisbursalService
        .downloadBatchExcel(batchIdStr)
        .<ResponseEntity<?>>map(
            bytes -> {
              HttpHeaders headers = new HttpHeaders();
              headers.setContentType(
                  MediaType.parseMediaType(
                      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
              headers.setContentDispositionFormData("attachment", "batch_" + batchIdStr + XLSX);
              headers.setContentLength(bytes.length);
              return ResponseEntity.ok().headers(headers).body(bytes);
            });
  }

  private LocalDate parseDate(String dateStr, String paramName) {
    if (dateStr == null || dateStr.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
    } catch (DateTimeParseException ex) {
      throw new ClientSideException(
          String.format(INVALID_DATE_FORMAT_MESSAGE, paramName),
          String.format(INVALID_DATE_FORMAT_MESSAGE, paramName),
          HttpStatus.BAD_REQUEST);
    }
  }
}
