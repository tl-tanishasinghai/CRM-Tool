package com.trillionloans.los.service.disbursal;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;
import static com.trillionloans.los.constant.StringConstants.MANUAL;
import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.constant.StringConstants.TRIGGER_DISB_CTA_IDENTIFIER;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.BatchStatus;
import com.trillionloans.los.constant.DisbursalStatus;
import com.trillionloans.los.constant.ReverseFeedBatchStatus;
import com.trillionloans.los.constant.ReverseFeedSyncStatus;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.BatchDownloadException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.ServerErrorException;
import com.trillionloans.los.model.dto.ReverseFeedRowDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.DisbursalBatch;
import com.trillionloans.los.model.entity.DisbursalRegistryEntity;
import com.trillionloans.los.model.entity.ReverseFeedBatchEntity;
import com.trillionloans.los.model.entity.ReverseFeedBatchItemEntity;
import com.trillionloans.los.model.request.CreateBatchRequest;
import com.trillionloans.los.model.request.DrawdownApproveRequest;
import com.trillionloans.los.model.request.DrawdownRejectRequest;
import com.trillionloans.los.model.request.LoanAccountRejectRequest;
import com.trillionloans.los.model.request.ReverseFeedApprovalRequest;
import com.trillionloans.los.model.request.ReverseFeedRejectionRequest;
import com.trillionloans.los.model.request.m2p.M2pDisburseLoanByLanRequestDTO;
import com.trillionloans.los.model.response.*;
import com.trillionloans.los.model.response.m2p.DisbursalBatchDataResponse;
import com.trillionloans.los.repository.DisbursalBatchRepository;
import com.trillionloans.los.repository.PartnerMasterRepository;
import com.trillionloans.los.service.db.DisbursalRegistryStoreService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.db.ReverseFeedBatchItemStoreService;
import com.trillionloans.los.service.db.ReverseFeedBatchStoreService;
import com.trillionloans.los.service.drawdownorchestrator.DrawdownOrchestrator;
import com.trillionloans.los.util.EncryptionUtil;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.MDC;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.ContextView;

/**
 * service for processing reverse feed uploads. handles: excel parsing, batch/item seeding, m2p api
 * calls, registry updates.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NexusManualDisbursalService {

  private final ReverseFeedBatchStoreService batchStoreService;
  private final ReverseFeedBatchItemStoreService itemStoreService;
  private final DisbursalRegistryStoreService registryStoreService;
  private final DisbursalBatchRepository disbursalBatchRepository;
  private final PartnerMasterRepository partnerMasterRepository;
  private final M2PWrapperApi m2pWrapperApi;
  private final DrawdownOrchestrator drawdownOrchestrator;
  private final ProductConfigMasterService productConfigMasterService;
  private final TransactionalOperator transactionalOperator;
  private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
  private final EncryptionUtil encryptionUtil;

  private static final String LOG_HEADER = "[REVERSE_FEED]";
  private static final String CREATE_BATCH_LOG = "[CREATE_BATCH]";
  private static final String MANUAL_QUEUE_LOG = "[MANUAL_QUEUE]";
  private static final String SUCCESS_STATUS = "E"; // e = executed/success
  private static final String REJECTED_STATUS = "R"; // r = rejected/failure
  private static final String DATE_FORMAT = "dd-MM-yyyy";
  private static final String DISBURSE_LOAN_DATE_FORMAT = "yyyy-MM-dd";
  private static final String DOWNLOAD_BATCH_LOG = "[DOWNLOAD_BATCH]";
  private static final String REJECT_LOAN_DATE_FORMAT = "dd MMMM yyyy";

  // determines if the product is line based using flow configuration
  private boolean isLineBasedProduct(ProductControl.Flow flowData) {
    if (flowData == null || flowData.getConditions() == null) {
      return false;
    }
    Object raw = flowData.getConditions().get("lineBasedProduct");
    if (raw instanceof Boolean bool) {
      return bool;
    }
    if (raw instanceof String str) {
      return Boolean.parseBoolean(str);
    }
    return false;
  }

  /**
   * validates that a registry entry is eligible for reverse feed processing. reverse feed is only
   * for manual disbursals that have been batched. returns Mono.empty() if valid, Mono.error() if
   * invalid.
   *
   * <p>validations: - disburse_type must be MANUAL - disburse_status must NOT be INIT or MANUAL_INI
   * (should be MANUAL_BATCHED)
   */
  private Mono<Void> validateRegistryForReverseFeed(
      DisbursalRegistryEntity registry, String excelValue) {
    String disburseType = registry.getDisburseType();
    DisbursalStatus disburseStatus = registry.getDisburseStatus();

    // validate disburse type is MANUAL
    if (!MANUAL.equals(disburseType)) {
      String errorMsg =
          String.format(
              "reverse feed not allowed for non-manual disbursal. excelValue: %s, type: %s",
              excelValue, disburseType);
      log.error("{} {}", LOG_HEADER, errorMsg);
      return Mono.error(new BaseException(errorMsg, null, HttpStatus.BAD_REQUEST));
    }

    // validate status is not INIT or MANUAL_INI (should be MANUAL_BATCHED)
    if (disburseStatus == DisbursalStatus.INIT || disburseStatus == DisbursalStatus.MANUAL_INI) {
      String errorMsg =
          String.format(
              "reverse feed not allowed for status %s. excelValue: %s must be in MANUAL_BATCHED"
                  + " status",
              disburseStatus, excelValue);
      log.error("{} {}", LOG_HEADER, errorMsg);
      return Mono.error(new BaseException(errorMsg, null, HttpStatus.BAD_REQUEST));
    }

    log.info(
        "{} registry validation passed for excelValue: {}, type: {}, status: {}",
        LOG_HEADER,
        excelValue,
        disburseType,
        disburseStatus);
    return Mono.empty();
  }

  /**
   * initiates reverse feed upload - returns ack immediately, processes in background. step 1: read
   * and parse file step 2: seed batch and items in db step 3: return ack response step 4: process
   * items async in background
   */
  public Mono<ReverseFeedUploadResponseDTO> initiateReverseFeedUpload(
      FilePart file, String uploadedBy) {
    log.info(
        "{} initiating reverse feed upload. file: {}, user: {}",
        LOG_HEADER,
        file.filename(),
        uploadedBy);
    UUID batchId = UUID.randomUUID();
    String fileName = file.filename();

    // step 1: read file content and parse excel
    return DataBufferUtils.join(file.content())
        .flatMap(
            dataBuffer -> {
              List<ReverseFeedRowDTO> rows;
              try {
                if (dataBuffer.readableByteCount() == 0) {
                  return Mono.error(
                      new BaseException("file is mandatory", null, HttpStatus.BAD_REQUEST));
                }
                InputStream inputStream = dataBuffer.asInputStream();
                rows = parseExcelFile(inputStream);
                log.info("{} parsed {} rows from excel file", LOG_HEADER, rows.size());
              } catch (Exception e) {
                log.error("{} error parsing excel file: {}", LOG_HEADER, e.getMessage(), e);
                return Mono.error(
                    new BaseException(
                        "failed to parse Excel file: " + e.getMessage(),
                        null,
                        HttpStatus.BAD_REQUEST));
              } finally {
                DataBufferUtils.release(dataBuffer);
              }
              if (rows.isEmpty()) {
                return Mono.error(
                    new BaseException(
                        "excel file contains no valid records", null, HttpStatus.BAD_REQUEST));
              }

              // step 2: seed batch and items in db
              return seedBatchAndItems(batchId, fileName, rows, uploadedBy)
                  .flatMap(
                      batch ->
                          Mono.deferContextual(
                              parentContext -> {
                                // step 3: build ack response
                                ReverseFeedUploadResponseDTO response =
                                    ReverseFeedUploadResponseDTO.builder()
                                        .batchId(batchId.toString())
                                        .status(ReverseFeedBatchStatus.PROCESSING.name())
                                        .message(
                                            "reverse feed uploaded successfully. processing started"
                                                + " in background.")
                                        .totalRecords(rows.size())
                                        .build();

                                // step 4: trigger async processing after returning response (with
                                // context)
                                triggerAsyncProcessing(batchId, parentContext);
                                return Mono.just(response);
                              }));
            });
  }

  /** triggers background processing for the batch. called after ack response is prepared. */
  private void triggerAsyncProcessing(UUID batchId, ContextView parentContext) {
    log.info("{} triggering async processing for batch {}", LOG_HEADER, batchId);
    String traceId = MDC.get(TRACE_ID);
    processAllItems(batchId)
        .subscribeOn(Schedulers.boundedElastic())
        .contextWrite(
            ctx -> {
              ctx = ctx.put(TRACE_ID, traceId);
              if (parentContext.hasKey(PARTNER_ID)) {
                ctx = ctx.put(PARTNER_ID, parentContext.get(PARTNER_ID));
              }
              return ctx;
            })
        .subscribe(
            result -> log.info("{} batch {} processing completed", LOG_HEADER, batchId),
            error ->
                log.error(
                    "{} batch {} processing failed: {}",
                    LOG_HEADER,
                    batchId,
                    error.getMessage(),
                    error));
  }

  // expected column headers for reverse feed excel
  private static final String COL_TRANSACTION_DATE = "Transaction Date";
  private static final String COL_LOAN_ACCOUNT_NUMBER = "Loan Account Number / Transaction ID";
  private static final String COL_LOAN_ACCOUNT_NUMBER_LEGACY = "Loan Account Number";
  private static final String COL_TRANSACTION_STATUS = "Transaction Status";
  private static final String COL_TRANSACTION_REJECTION_REASON = "Transaction Rejection Reason";
  private static final String COL_UTR_REFERENCE_NUMBER = "UTR Reference Number";

  /**
   * parse excel file and extract rows. reads columns by header name for flexibility. expected
   * columns: Transaction Date, Loan Account Number / Transaction ID, Transaction Status,
   * Transaction Rejection Reason, UTR Reference Number
   */
  private List<ReverseFeedRowDTO> parseExcelFile(InputStream inputStream) throws Exception {
    List<ReverseFeedRowDTO> rows = new ArrayList<>();
    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
      Sheet sheet = workbook.getSheetAt(0);

      // build column index map from header row
      Map<String, Integer> columnIndexMap = buildColumnIndexMap(sheet.getRow(0));
      log.info("{} parsed column headers: {}", LOG_HEADER, columnIndexMap.keySet());

      // validate required columns exist
      validateRequiredColumns(columnIndexMap);

      // parse data rows (skip header row 0)
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        ReverseFeedRowDTO dto =
            ReverseFeedRowDTO.builder()
                .transactionDate(
                    getCellValueByColumnName(row, columnIndexMap, COL_TRANSACTION_DATE))
                .loanAccountNumber(
                    getCellValueByColumnName(row, columnIndexMap, COL_LOAN_ACCOUNT_NUMBER))
                .transactionStatus(
                    getCellValueByColumnName(row, columnIndexMap, COL_TRANSACTION_STATUS))
                .transactionRejectionReason(
                    getCellValueByColumnName(row, columnIndexMap, COL_TRANSACTION_REJECTION_REASON))
                .utrReferenceNumber(
                    getCellValueByColumnName(row, columnIndexMap, COL_UTR_REFERENCE_NUMBER))
                .build();

        // skip rows without loan account number (mandatory)
        String lan = dto.getLoanAccountNumber();
        if (lan != null && !lan.trim().isEmpty()) {
          rows.add(dto);
        }
      }
    }
    return rows;
  }

  /** builds a map of column header name to column index from the header row. */
  private Map<String, Integer> buildColumnIndexMap(Row headerRow) {
    Map<String, Integer> columnIndexMap = new java.util.HashMap<>();
    if (headerRow == null) {
      return columnIndexMap;
    }
    for (int i = 0; i < headerRow.getLastCellNum(); i++) {
      Cell cell = headerRow.getCell(i);
      if (cell != null) {
        String headerName = getCellValueAsString(cell);
        if (headerName != null && !headerName.isEmpty()) {
          columnIndexMap.put(headerName.trim(), i);
        }
      }
    }
    // Backward compatibility: accept legacy header and map it to the new expected header.
    if (!columnIndexMap.containsKey(COL_LOAN_ACCOUNT_NUMBER)
        && columnIndexMap.containsKey(COL_LOAN_ACCOUNT_NUMBER_LEGACY)) {
      columnIndexMap.put(
          COL_LOAN_ACCOUNT_NUMBER, columnIndexMap.get(COL_LOAN_ACCOUNT_NUMBER_LEGACY));
    }
    return columnIndexMap;
  }

  /** validates that all required columns are present in the excel file. */
  private void validateRequiredColumns(Map<String, Integer> columnIndexMap) {
    List<String> requiredColumns =
        List.of(
            COL_TRANSACTION_DATE,
            COL_LOAN_ACCOUNT_NUMBER,
            COL_TRANSACTION_STATUS,
            COL_TRANSACTION_REJECTION_REASON,
            COL_UTR_REFERENCE_NUMBER);

    List<String> missingColumns =
        requiredColumns.stream().filter(col -> !columnIndexMap.containsKey(col)).toList();

    if (!missingColumns.isEmpty()) {
      throw new BaseException(
          "missing required columns in excel file: " + String.join(", ", missingColumns),
          null,
          HttpStatus.BAD_REQUEST);
    }
  }

  /** gets cell value by column name using the column index map. */
  private String getCellValueByColumnName(
      Row row, Map<String, Integer> columnIndexMap, String columnName) {
    Integer colIndex = columnIndexMap.get(columnName);
    if (colIndex == null) {
      return null;
    }
    return getCellValueAsString(row.getCell(colIndex));
  }

  private String getCellValueAsString(Cell cell) {
    if (cell == null) return null;
    if (cell.getCellType() == CellType.STRING) {
      return cell.getStringCellValue().trim();
    } else if (cell.getCellType() == CellType.NUMERIC) {
      // handle numeric values (including dates formatted as numbers)
      return String.valueOf((long) cell.getNumericCellValue());
    }
    return null;
  }

  /** phase a: seed batch and all items in a single logical operation. */
  private Mono<ReverseFeedBatchEntity> seedBatchAndItems(
      UUID batchId, String fileName, List<ReverseFeedRowDTO> rows, String uploadedBy) {
    log.info("{} seeding batch {} with {} items", LOG_HEADER, batchId, rows.size());
    LocalDateTime now = LocalDateTime.now(ZoneId.of(ASIA_KOLKATA));

    // create batch entity
    ReverseFeedBatchEntity batch =
        ReverseFeedBatchEntity.builder()
            .batchId(batchId)
            .fileName(fileName)
            .totalRecords(rows.size())
            .successCount(0)
            .failedCount(0)
            .pendingCount(rows.size())
            .status(ReverseFeedBatchStatus.PROCESSING.name())
            .uploadedBy(uploadedBy)
            .uploadedAt(now)
            .build();

    // create item entities
    List<ReverseFeedBatchItemEntity> items =
        rows.stream().map(row -> mapRowToItem(batchId, row)).toList();

    // save batch first, then save all items
    return batchStoreService
        .save(batch)
        .flatMap(savedBatch -> itemStoreService.saveAll(items).collectList().thenReturn(savedBatch))
        .doOnSuccess(
            savedBatch ->
                log.info(
                    "{} batch {} seeded successfully with {} items",
                    LOG_HEADER,
                    batchId,
                    rows.size()))
        .doOnError(
            error ->
                log.error(
                    "{} error seeding batch {}: {}", LOG_HEADER, batchId, error.getMessage()));
  }

  private ReverseFeedBatchItemEntity mapRowToItem(UUID batchId, ReverseFeedRowDTO row) {
    // parse transaction date from excel string
    LocalDateTime transactionDate = parseTransactionDate(row.getTransactionDate());

    // store excel "loan account number" temporarily in referenceId2 for lookup
    // - for non-line products: this is correct field (loan_account_number)
    // - for line products: this will be corrected after processing (it's actually transaction_id)
    // after processing, both ref1 and ref2 will be updated with correct values from registry
    String loanAccountNumber = row.getLoanAccountNumber();
    return ReverseFeedBatchItemEntity.builder()
        .batchId(batchId)
        .referenceId1(null)
        .referenceId2(loanAccountNumber)
        .transactionStatus(row.getTransactionStatus())
        .utrNumber(row.getUtrReferenceNumber())
        .transactionRejectionReason(row.getTransactionRejectionReason())
        .transactionDate(transactionDate)
        .amount(row.getAmount())
        .syncStatus(ReverseFeedSyncStatus.PENDING.name())
        .build();
  }

  /** parses transaction date string from excel to LocalDateTime. supports multiple formats. */
  private LocalDateTime parseTransactionDate(String dateStr) {
    if (dateStr == null || dateStr.isEmpty()) {
      return null;
    }
    try {
      // try common date formats
      String[] formats = {
        REJECT_LOAN_DATE_FORMAT, // 19 February 2026
        DATE_FORMAT, // 19-02-2026
        DISBURSE_LOAN_DATE_FORMAT, // 2026-02-19
        "dd/MM/yyyy", // 19/02/2026
        "MM/dd/yyyy", // 02/19/2026
        "d MMMM yyyy", // 9 February 2026
        "MMMM dd, yyyy" // February 19, 2026
      };
      for (String format : formats) {
        try {
          return LocalDateTime.parse(
              dateStr + " 00:00:00",
              DateTimeFormatter.ofPattern(format + " HH:mm:ss", java.util.Locale.ENGLISH));
        } catch (Exception ignored) {
          // try next format
        }
      }
      // if none work, try parsing as date time
      return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    } catch (Exception e) {
      log.warn("{} failed to parse transaction date '{}': {}", LOG_HEADER, dateStr, e.getMessage());
      return null;
    }
  }

  /**
   * process all pending items in a batch. for each item: call m2p api based on status, then update
   * registry.
   */
  public Mono<Void> processAllItems(UUID batchId) {
    return itemStoreService
        .findByBatchIdAndSyncStatus(batchId, ReverseFeedSyncStatus.PENDING.name())
        .flatMap(this::processItem)
        .collectList()
        .flatMap(results -> updateBatchCounts(batchId))
        .then();
  }

  /** process a single item: call m2p api based on status, then update registry. */
  private Mono<ReverseFeedBatchItemEntity> processItem(ReverseFeedBatchItemEntity item) {
    // excel "loan account number" stored in ref2 temporarily
    String loanAccountNumber = item.getReferenceId2();
    if (loanAccountNumber == null || loanAccountNumber.trim().isEmpty()) {
      log.error("{} loan account number is mandatory; marking item failed", LOG_HEADER);
      return itemStoreService.markFailed(item, "loan account number is mandatory");
    }
    log.info("{} processing item for loanAccountNumber: {}", LOG_HEADER, loanAccountNumber);

    if (SUCCESS_STATUS.equalsIgnoreCase(item.getTransactionStatus())) {
      return processSuccessItem(item);
    } else if (REJECTED_STATUS.equalsIgnoreCase(item.getTransactionStatus())) {
      return processRejectedItem(item);
    } else {
      log.error(
          "{} unknown transaction status '{}' for loanAccountNumber: {}",
          LOG_HEADER,
          item.getTransactionStatus(),
          loanAccountNumber);
      return itemStoreService.markFailed(
          item, "unknown transaction status: " + item.getTransactionStatus());
    }
  }

  /** process success item: call appropriate approval api based on product type. */
  private Mono<ReverseFeedBatchItemEntity> processSuccessItem(ReverseFeedBatchItemEntity item) {
    String loanAccountNumber = item.getReferenceId2();
    log.info("{} processing success item for loanAccountNumber: {}", LOG_HEADER, loanAccountNumber);

    // mandatory validations: do not disburse without UTR or transaction date (do not trigger m2p
    // api)
    if (item.getUtrNumber() == null || item.getUtrNumber().trim().isEmpty()) {
      log.error(
          "{} utr is mandatory for disbursal; skipping m2p api, marking item failed for"
              + " loanAccountNumber: {}",
          LOG_HEADER,
          loanAccountNumber);
      return itemStoreService.markFailed(item, "utr is mandatory for disbursal");
    }
    if (item.getTransactionDate() == null) {
      log.warn(
          "{} invalid or missing transaction date; skipping m2p disburse api, marking item failed"
              + " for loanAccountNumber: {} (proceeding to next row)",
          LOG_HEADER,
          loanAccountNumber);
      return itemStoreService.markFailed(
          item, "invalid or missing transaction date; disbursal not triggered");
    }

    // build generic reverse feed approval request - use transaction date from excel
    ReverseFeedApprovalRequest request =
        ReverseFeedApprovalRequest.builder()
            .transactionDate(item.getTransactionDate())
            .paymentType("online transfer")
            .referenceNumber(item.getUtrNumber())
            .notes("reverse feed approval - utr: " + item.getUtrNumber())
            .build();

    // call appropriate approval api based on product type
    return markSuccessAtM2p(item, request)
        .flatMap(
            response -> {
              item.setReferenceId1(response.getReferenceId1());
              item.setReferenceId2(response.getReferenceId2());
              item.setM2pResponse(response.getM2pResponse());
              return updateItemAndRegistryForSuccess(item);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "{} approval api failed for loanAccountNumber {}: {}",
                  LOG_HEADER,
                  loanAccountNumber,
                  error.getMessage());
              item.setM2pResponse(extractErrorResponse(error));
              return itemStoreService.markFailed(item, error.getMessage());
            });
  }

  /** process rejected item: call appropriate rejection api based on product type. */
  private Mono<ReverseFeedBatchItemEntity> processRejectedItem(ReverseFeedBatchItemEntity item) {
    String loanAccountNumber = item.getReferenceId2();
    log.info(
        "{} processing rejected item for loanAccountNumber: {}", LOG_HEADER, loanAccountNumber);

    // transaction date mandatory for rejection flow (do not trigger m2p api if invalid/missing)
    if (item.getTransactionDate() == null) {
      log.warn(
          "{} invalid or missing transaction date; skipping m2p reject api, marking item failed for"
              + " loanAccountNumber: {} (proceeding to next row)",
          LOG_HEADER,
          loanAccountNumber);
      return itemStoreService.markFailed(
          item, "invalid or missing transaction date; rejection not triggered");
    }

    // build generic reverse feed rejection request - use transaction date from excel
    ReverseFeedRejectionRequest request =
        ReverseFeedRejectionRequest.builder()
            .transactionDate(item.getTransactionDate())
            .rejectionNotes(item.getTransactionRejectionReason())
            .build();

    // call appropriate rejection api based on product type
    return markRejectAtM2p(item, request)
        .flatMap(
            response -> {
              item.setReferenceId1(response.getReferenceId1());
              item.setReferenceId2(response.getReferenceId2());
              item.setM2pResponse(response.getM2pResponse());
              return updateItemAndRegistryForFailure(item);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "{} rejection api failed for loanAccountNumber {}: {}",
                  LOG_HEADER,
                  loanAccountNumber,
                  error.getMessage());
              item.setM2pResponse(extractErrorResponse(error));
              return itemStoreService.markFailed(item, error.getMessage());
            });
  }

  /**
   * update both item and registry for successful disbursal. order: registry update first
   * (critical), then item update. if registry update fails, we don't mark item as success - it can
   * be retried. if item update fails after registry success, registry is still correct (fail-safe).
   */
  private Mono<ReverseFeedBatchItemEntity> updateItemAndRegistryForSuccess(
      ReverseFeedBatchItemEntity item) {
    String referenceId1 = item.getReferenceId1();
    String referenceId2 = item.getReferenceId2();
    String utrNumber = item.getUtrNumber();
    log.info(
        "{} updating registry and item for success - referenceId2: {}, utr: {}",
        LOG_HEADER,
        referenceId2,
        utrNumber);
    return registryStoreService
        .markDisbursementSuccess(referenceId1, referenceId2, utrNumber)
        .flatMap(
            registryEntity -> {
              log.info(
                  "{} [SUCCESS_MARKING] registry updated for referenceId2: {}, now updating item",
                  LOG_HEADER,
                  referenceId2);
              return itemStoreService.markSuccess(item);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "{} [SUCCESS_MARKING] registry update failed for referenceId2: {} - {}",
                  LOG_HEADER,
                  referenceId2,
                  error.getMessage());
              // append warning to existing m2p response
              String existingResponse = item.getM2pResponse() != null ? item.getM2pResponse() : "";
              item.setM2pResponse(
                  existingResponse
                      + " [WARNING: registry update failed - "
                      + error.getMessage()
                      + "]");
              return itemStoreService.markSuccess(item);
            });
  }

  /**
   * update both item and registry for failed/rejected disbursal. order: registry update first
   * (critical), then item update.
   */
  private Mono<ReverseFeedBatchItemEntity> updateItemAndRegistryForFailure(
      ReverseFeedBatchItemEntity item) {
    String referenceId1 = item.getReferenceId1();
    String referenceId2 = item.getReferenceId2();
    String failureReason = item.getTransactionRejectionReason();
    log.info(
        "{} updating registry and item for failure - referenceId2: {}, reason: {}",
        LOG_HEADER,
        referenceId2,
        failureReason);
    return registryStoreService
        .markDisbursementFailed(referenceId1, referenceId2, failureReason)
        .flatMap(
            registryEntity -> {
              log.info(
                  "{} [FAILURE_MARKING] registry updated for referenceId2: {}, now updating item",
                  LOG_HEADER,
                  referenceId2);
              return itemStoreService.markSuccess(item);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "{} [FAILURE_MARKING] registry update failed for referenceId2: {} - {}",
                  LOG_HEADER,
                  referenceId2,
                  error.getMessage());
              // append warning to existing m2p response
              String existingResponse = item.getM2pResponse() != null ? item.getM2pResponse() : "";
              item.setM2pResponse(
                  existingResponse
                      + " [WARNING: registry update failed - "
                      + error.getMessage()
                      + "]");
              return itemStoreService.markSuccess(item);
            });
  }

  /** update batch counts after processing items. */
  private Mono<ReverseFeedBatchEntity> updateBatchCounts(UUID batchId) {
    return Mono.zip(
            itemStoreService.countByBatchIdAndSyncStatus(
                batchId, ReverseFeedSyncStatus.SUCCESS.name()),
            itemStoreService.countByBatchIdAndSyncStatus(
                batchId, ReverseFeedSyncStatus.FAILED.name()),
            itemStoreService.countByBatchIdAndSyncStatus(
                batchId, ReverseFeedSyncStatus.PENDING.name()))
        .flatMap(
            tuple -> {
              int successCount = tuple.getT1().intValue();
              int failedCount = tuple.getT2().intValue();
              int pendingCount = tuple.getT3().intValue();

              String status =
                  failedCount > 0
                      ? ReverseFeedBatchStatus.PARTIAL_FAILURE.name()
                      : ReverseFeedBatchStatus.COMPLETED.name();
              String finalStatus =
                  pendingCount == 0 ? status : ReverseFeedBatchStatus.PROCESSING.name();

              return batchStoreService
                  .findByBatchId(batchId)
                  .flatMap(
                      batch -> {
                        batch.setSuccessCount(successCount);
                        batch.setFailedCount(failedCount);
                        batch.setPendingCount(pendingCount);
                        batch.setStatus(finalStatus);
                        if (pendingCount == 0) {
                          batch.setCompletedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
                        }
                        return batchStoreService.update(batch);
                      });
            });
  }

  /**
   * calls appropriate approval api after reverse feed based on product type.
   *
   * <p>lookup logic: - first tries to find registry by referenceId2 (non-line products: LAN) - if
   * not found, tries by referenceId1 (line products: transaction_id)
   *
   * <p>for line products: excel "loan account number" is actually transaction_id (ref1) for
   * non-line products: excel "loan account number" is loan_account_number (ref2)
   */
  private Mono<DisbursalResultDTO> markSuccessAtM2p(
      ReverseFeedBatchItemEntity item, ReverseFeedApprovalRequest request) {
    // excel "loan account number" stored temporarily in ref2 for lookup
    String loanAccountNumber = item.getReferenceId2();
    log.info("{} calling approval api for loanAccountNumber: {}", LOG_HEADER, loanAccountNumber);

    // try to find registry - first by ref2 (non-line), then by ref1 (line)
    return findRegistryByLoanAccountNumber(loanAccountNumber)
        .flatMap(
            registry -> {
              String referenceId1 = registry.getReferenceId1();
              String referenceId2 = registry.getReferenceId2();
              String productCode = registry.getProductCode();
              String anchorId = registry.getAnchorId();
              // Persist canonical ids from registry before external API call so failed rows
              // also carry correct referenceId1/referenceId2 instead of temporary lookup value.
              item.setReferenceId1(referenceId1);
              item.setReferenceId2(referenceId2);
              log.info(
                  "{} [REVERSE_FEED_APPROVE] found ref1: {}, ref2: {}, productCode: {}, anchorId:"
                      + " {} for loanAccountNumber: {}",
                  LOG_HEADER,
                  referenceId1,
                  referenceId2,
                  productCode,
                  anchorId,
                  loanAccountNumber);

              // validate registry entry is eligible for reverse feed
              return validateRegistryForReverseFeed(registry, loanAccountNumber)
                  .then(
                      productConfigMasterService
                          .getProductConfigMasterData(productCode)
                          .flatMap(
                              productControlData -> {
                                ProductControl.Flow flowData =
                                    productConfigMasterService.getFlowFromProductConfig(
                                        productControlData.getT2(), TRIGGER_DISB_CTA_IDENTIFIER);
                                boolean isLineBased = isLineBasedProduct(flowData);

                                if (isLineBased) {
                                  // line products: excelValue is ref1 (transaction_id)
                                  return processLineBasedApproval(
                                      registry, request, productCode, referenceId1, referenceId2);
                                }

                                // non-line products: excelValue is ref2 (loan_account_number)
                                return processNonLineBasedApproval(
                                    registry, request, referenceId1, referenceId2);
                              }));
            })
        .switchIfEmpty(
            Mono.error(
                new RuntimeException(
                    "registry entry not found for loanAccountNumber: " + loanAccountNumber)));
  }

  /** finds registry by trying ref2 first (non-line products), then ref1 (line products). */
  private Mono<DisbursalRegistryEntity> findRegistryByLoanAccountNumber(String loanAccountNumber) {
    // try ref2 first (non-line products: loan_account_number)
    return registryStoreService
        .findByReferenceId2(loanAccountNumber, false)
        .switchIfEmpty(
            // if not found, try ref1 (line products: transaction_id)
            registryStoreService.findByReferenceId1(loanAccountNumber, false));
  }

  /** processes approval for line-based products using drawdown orchestrator. */
  private Mono<DisbursalResultDTO> processLineBasedApproval(
      DisbursalRegistryEntity registry,
      ReverseFeedApprovalRequest request,
      String productCode,
      String referenceId1,
      String referenceId2) {

    DrawdownApproveRequest drawdownRequest =
        DrawdownApproveRequest.builder()
            .transactionTime(
                getTransactionTimeEpochMillis(
                    normalizeTransactionDateTime(request.getTransactionDate())))
            .paymentType(request.getPaymentType())
            .referenceNumber(request.getReferenceNumber())
            .notes(request.getNotes())
            .build();

    return drawdownOrchestrator
        .approveDrawdown(referenceId1, drawdownRequest, productCode)
        .map(
            response ->
                DisbursalResultDTO.builder()
                    .referenceId1(referenceId1)
                    .referenceId2(referenceId2)
                    .lineId(response.getLineId())
                    .status(response.getStatus())
                    .receiptNumber(response.getReceiptNumber())
                    .approvedAmount(response.getApprovedAmount())
                    .netDisbursement(response.getNetDisbursement())
                    .disbursementDate(response.getDisbursementDate())
                    .isLineBasedProduct(true)
                    .m2pResponse(toJsonString(response))
                    .build());
  }

  /** processes approval for non-line products using disburseLoanByLan api. */
  private Mono<DisbursalResultDTO> processNonLineBasedApproval(
      DisbursalRegistryEntity registry,
      ReverseFeedApprovalRequest request,
      String referenceId1,
      String referenceId2) {

    String disbursementDate =
        request.getTransactionDate() != null
            ? request
                .getTransactionDate()
                .format(DateTimeFormatter.ofPattern(DISBURSE_LOAN_DATE_FORMAT))
            : null;

    M2pDisburseLoanByLanRequestDTO disburseRequest =
        M2pDisburseLoanByLanRequestDTO.builder()
            .actualDisbursementDate(disbursementDate)
            .dateFormat(DISBURSE_LOAN_DATE_FORMAT)
            .locale("en")
            .paymentTypeId(1)
            .note(request.getNotes())
            .bankAccountDetailId(null)
            .routingCode(request.getReferenceNumber())
            .build();

    return m2pWrapperApi
        .disburseLoanByLan(disburseRequest, referenceId2)
        .map(
            response ->
                DisbursalResultDTO.builder()
                    .referenceId1(referenceId1)
                    .referenceId2(referenceId2)
                    .status("SUCCESS")
                    .receiptNumber(disburseRequest.getRoutingCode())
                    .approvedAmount(registry.getGrossDisbursalAmount())
                    .netDisbursement(registry.getNetDisbursalAmount())
                    .disbursementDate(disbursementDate)
                    .isLineBasedProduct(false)
                    .m2pResponse(toJsonString(response))
                    .build());
  }

  /**
   * calls appropriate rejection api after reverse feed based on product type.
   *
   * <p>lookup logic same as markSuccessAtM2p: tries ref2 first, then ref1.
   */
  private Mono<DisbursalResultDTO> markRejectAtM2p(
      ReverseFeedBatchItemEntity item, ReverseFeedRejectionRequest request) {
    // excel "loan account number" stored temporarily in ref2 for lookup
    String loanAccountNumber = item.getReferenceId2();
    log.info("{} calling rejection api for loanAccountNumber: {}", LOG_HEADER, loanAccountNumber);

    // try to find registry - first by ref2 (non-line), then by ref1 (line)
    return findRegistryByLoanAccountNumber(loanAccountNumber)
        .flatMap(
            registry -> {
              String referenceId1 = registry.getReferenceId1();
              String referenceId2 = registry.getReferenceId2();
              String productCode = registry.getProductCode();
              // Persist canonical ids from registry before external API call so failed rows
              // also carry correct referenceId1/referenceId2 instead of temporary lookup value.
              item.setReferenceId1(referenceId1);
              item.setReferenceId2(referenceId2);
              log.info(
                  "{} [REVERSE_FEED_REJECT] found ref1: {}, ref2: {}, productCode: {} for"
                      + " loanAccountNumber: {}",
                  LOG_HEADER,
                  referenceId1,
                  referenceId2,
                  productCode,
                  loanAccountNumber);

              // validate registry entry is eligible for reverse feed
              return validateRegistryForReverseFeed(registry, loanAccountNumber)
                  .then(
                      productConfigMasterService
                          .getProductConfigMasterData(productCode)
                          .flatMap(
                              productControlData -> {
                                ProductControl.Flow flowData =
                                    productConfigMasterService.getFlowFromProductConfig(
                                        productControlData.getT2(), TRIGGER_DISB_CTA_IDENTIFIER);
                                boolean isLineBased = isLineBasedProduct(flowData);
                                if (isLineBased) {
                                  return processLineBasedRejection(
                                      registry, request, productCode, referenceId1, referenceId2);
                                }

                                return processNonLineBasedRejection(
                                    registry, request, referenceId1, referenceId2);
                              }));
            })
        .switchIfEmpty(
            Mono.error(
                new RuntimeException(
                    "registry entry not found for loanAccountNumber: " + loanAccountNumber)));
  }

  /** processes rejection for line-based products using drawdown orchestrator. */
  private Mono<DisbursalResultDTO> processLineBasedRejection(
      DisbursalRegistryEntity registry,
      ReverseFeedRejectionRequest request,
      String productCode,
      String referenceId1,
      String referenceId2) {

    DrawdownRejectRequest drawdownRequest =
        DrawdownRejectRequest.builder()
            .transactionTime(
                getTransactionTimeEpochMillis(
                    normalizeTransactionDateTime(request.getTransactionDate())))
            .rejectionNotes(request.getRejectionNotes())
            .build();

    return drawdownOrchestrator
        .rejectDrawdown(referenceId1, drawdownRequest, productCode)
        .map(
            response ->
                DisbursalResultDTO.builder()
                    .referenceId1(referenceId1)
                    .referenceId2(referenceId2)
                    .lineId(response.getLineId())
                    .status(response.getStatus())
                    .rejectionReason(request.getRejectionNotes())
                    .isLineBasedProduct(true)
                    .m2pResponse(toJsonString(response))
                    .build());
  }

  /** processes rejection for non-line products using rejectLoan api. */
  private Mono<DisbursalResultDTO> processNonLineBasedRejection(
      DisbursalRegistryEntity registry,
      ReverseFeedRejectionRequest request,
      String referenceId1,
      String referenceId2) {

    String rejectedOnDate =
        request.getTransactionDate() != null
            ? request
                .getTransactionDate()
                .format(
                    DateTimeFormatter.ofPattern(REJECT_LOAN_DATE_FORMAT, java.util.Locale.ENGLISH))
            : null;

    LoanAccountRejectRequest loanAccountRejectRequest =
        LoanAccountRejectRequest.builder().rejectedOnDate(rejectedOnDate).build();

    return m2pWrapperApi
        .rejectLoan(referenceId1, referenceId2, loanAccountRejectRequest)
        .map(
            response ->
                DisbursalResultDTO.builder()
                    .referenceId1(referenceId1)
                    .referenceId2(referenceId2)
                    .status("OPS_REJECTED")
                    .disbursementDate(rejectedOnDate)
                    .rejectionReason(request.getRejectionNotes())
                    .isLineBasedProduct(false)
                    .m2pResponse(toJsonString(response))
                    .build());
  }

  /** converts transaction date to epoch millis. uses current time if date is null. */
  private long getTransactionTimeEpochMillis(LocalDateTime transactionDate) {
    if (transactionDate == null) {
      return System.currentTimeMillis();
    }
    return transactionDate.atZone(ZoneId.of(ASIA_KOLKATA)).toInstant().toEpochMilli();
  }

  private LocalDateTime normalizeTransactionDateTime(LocalDateTime transactionDate) {
    if (transactionDate == null) {
      return null;
    }

    ZoneId zoneId = ZoneId.of(ASIA_KOLKATA);
    LocalDate today = LocalDate.now(zoneId);

    if (transactionDate.toLocalDate().isEqual(today)) {
      return LocalDateTime.now(zoneId).withNano(0);
    }

    return transactionDate.toLocalDate().atTime(23, 59, 59);
  }

  /** converts object to json string for storing m2p response. */
  private String toJsonString(Object obj) {
    if (obj == null) {
      return "null";
    }
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      log.warn("{} failed to serialize m2p response: {}", LOG_HEADER, e.getMessage());
      return obj.toString();
    }
  }

  /** extracts raw response body from error. returns actual m2p response if available. */
  private String extractErrorResponse(Throwable error) {
    // check for ClientSideException (has clientResponse field)
    if (error instanceof ClientSideException clientEx) {
      Object clientResponse = clientEx.getClientResponse();
      if (clientResponse != null) {
        return toJsonString(clientResponse);
      }
    }

    // check for ServerErrorException (has clientResponse field)
    if (error instanceof ServerErrorException serverEx) {
      Object clientResponse = serverEx.getClientResponse();
      if (clientResponse != null) {
        return toJsonString(clientResponse);
      }
    }

    // check for BaseException (has clientResponse field)
    if (error instanceof BaseException baseEx) {
      Object clientResponse = baseEx.getClientResponse();
      if (clientResponse != null) {
        return toJsonString(clientResponse);
      }
    }

    // try to get actual response body from webclient exception
    if (error
        instanceof
        org.springframework.web.reactive.function.client.WebClientResponseException webClientEx) {
      String responseBody = webClientEx.getResponseBodyAsString();
      if (responseBody != null && !responseBody.isEmpty()) {
        return responseBody;
      }
    }

    // check cause chain for all exception types
    Throwable cause = error.getCause();
    while (cause != null) {
      if (cause instanceof ClientSideException clientEx) {
        Object clientResponse = clientEx.getClientResponse();
        if (clientResponse != null) {
          return toJsonString(clientResponse);
        }
      }
      if (cause instanceof ServerErrorException serverEx) {
        Object clientResponse = serverEx.getClientResponse();
        if (clientResponse != null) {
          return toJsonString(clientResponse);
        }
      }
      if (cause instanceof BaseException baseEx) {
        Object clientResponse = baseEx.getClientResponse();
        if (clientResponse != null) {
          return toJsonString(clientResponse);
        }
      }
      if (cause
          instanceof
          org.springframework.web.reactive.function.client.WebClientResponseException webClientEx) {
        String responseBody = webClientEx.getResponseBodyAsString();
        if (responseBody != null && !responseBody.isEmpty()) {
          return responseBody;
        }
      }
      cause = cause.getCause();
    }

    // fallback to error message
    return error.getMessage();
  }

  /** get paginated history of reverse feed batches with optional status and date filters. */
  public Mono<ReverseFeedBatchHistoryResponseDTO> getReverseFeedBatchHistory(
      List<String> statuses, LocalDateTime fromDate, LocalDateTime toDate, int page, int limit) {

    List<String> sanitized =
        statuses == null
            ? List.of()
            : statuses.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toList());

    log.info(
        "{} fetching batch history - statuses: {}, from: {}, to: {}, page: {}, limit: {}",
        LOG_HEADER,
        sanitized,
        fromDate,
        toDate,
        page,
        limit);

    Flux<ReverseFeedBatchEntity> entriesFlux;
    Mono<Long> countMono;

    if (sanitized.isEmpty()) {
      entriesFlux = batchStoreService.findByDateRangePaginated(fromDate, toDate, page, limit);
      countMono = batchStoreService.countByDateRange(fromDate, toDate);
    } else {
      entriesFlux =
          batchStoreService.findByStatusesAndDateRangePaginated(
              sanitized, fromDate, toDate, page, limit);
      countMono = batchStoreService.countByStatusesAndDateRange(sanitized, fromDate, toDate);
    }

    return Mono.zip(entriesFlux.map(this::mapToHistoryDTO).collectList(), countMono)
        .map(
            tuple -> {
              List<ReverseFeedBatchHistoryDTO> historyItems = tuple.getT1();
              Long totalCount = tuple.getT2();

              return ReverseFeedBatchHistoryResponseDTO.builder()
                  .batches(historyItems)
                  .page(page)
                  .limit(limit)
                  .totalCount(totalCount)
                  .hasMore((long) (page + 1) * limit < totalCount)
                  .build();
            });
  }

  private ReverseFeedBatchHistoryDTO mapToHistoryDTO(ReverseFeedBatchEntity entity) {
    return ReverseFeedBatchHistoryDTO.builder()
        .batchId(entity.getBatchId().toString())
        .fileName(entity.getFileName())
        .status(entity.getStatus())
        .totalRecords(entity.getTotalRecords())
        .successCount(entity.getSuccessCount())
        .failedCount(entity.getFailedCount())
        .uploadedBy(entity.getUploadedBy())
        .uploadedAt(entity.getUploadedAt())
        .completedAt(entity.getCompletedAt())
        .build();
  }

  /** get status of a reverse feed batch by batch id. */
  public Mono<ReverseFeedBatchStatusResponseDTO> getReverseFeedBatchStatus(String batchIdStr) {
    log.info("{} fetching reverse feed batch status for batchId: {}", LOG_HEADER, batchIdStr);
    UUID batchId;
    try {
      batchId = UUID.fromString(batchIdStr);
    } catch (IllegalArgumentException e) {
      return Mono.error(new BaseException("invalid batch id format", null, HttpStatus.BAD_REQUEST));
    }

    return batchStoreService
        .findByBatchId(batchId)
        .switchIfEmpty(
            Mono.error(
                new BaseException("batch not found: " + batchIdStr, null, HttpStatus.NOT_FOUND)))
        .map(
            batch -> {
              int total = batch.getTotalRecords() != null ? batch.getTotalRecords() : 0;
              int success = batch.getSuccessCount() != null ? batch.getSuccessCount() : 0;
              int failed = batch.getFailedCount() != null ? batch.getFailedCount() : 0;
              int pending = batch.getPendingCount() != null ? batch.getPendingCount() : 0;

              int processed = success + failed;
              int percentage = total > 0 ? (int) Math.min((processed * 100.0) / total, 100) : 0;

              boolean isDownloadable =
                  ReverseFeedBatchStatus.COMPLETED.name().equals(batch.getStatus())
                      || ReverseFeedBatchStatus.PARTIAL_FAILURE.name().equals(batch.getStatus());

              return ReverseFeedBatchStatusResponseDTO.builder()
                  .batchId(batch.getBatchId().toString())
                  .status(batch.getStatus())
                  .fileName(batch.getFileName())
                  .totalRecords(total)
                  .successCount(success)
                  .failedCount(failed)
                  .pendingCount(pending)
                  .percentage(percentage)
                  .isDownloadable(isDownloadable)
                  .uploadedBy(batch.getUploadedBy())
                  .uploadedAt(batch.getUploadedAt())
                  .completedAt(batch.getCompletedAt())
                  .build();
            })
        .doOnError(
            error -> {
              if (!(error instanceof BaseException)) {
                log.error(
                    "{} failed to fetch reverse feed batch status: {}",
                    LOG_HEADER,
                    error.getMessage(),
                    error);
              }
            });
  }

  /** generate excel file for reverse feed batch download. */
  public Mono<byte[]> generateBatchExcel(String batchIdStr) {
    log.info("{} generating excel for batch: {}", LOG_HEADER, batchIdStr);
    UUID batchId;
    try {
      batchId = UUID.fromString(batchIdStr);
    } catch (IllegalArgumentException e) {
      return Mono.error(new BaseException("Invalid batch id format", null, HttpStatus.BAD_REQUEST));
    }
    return batchStoreService
        .findByBatchId(batchId)
        .switchIfEmpty(Mono.error(new BaseException("batch not found", null, HttpStatus.NOT_FOUND)))
        .flatMap(
            batch ->
                itemStoreService
                    .findByBatchId(batchId)
                    .collectList()
                    .map(this::createReverseFeedExcelBytes));
  }

  /** creates excel bytes from reverse feed batch items. */
  private byte[] createReverseFeedExcelBytes(List<ReverseFeedBatchItemEntity> items) {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Reverse Feed Details");
      Row headerRow = sheet.createRow(0);
      String[] headers = {
        "Reference ID 1 (LoanAppId/TxnId)",
        "Reference ID 2 (LAN/LineId)",
        "Bank Status",
        "UTR Number",
        "Rejection Reason",
        "Sync Status",
        "M2P Response",
        "Error Message",
        "Processed At"
      };
      for (int i = 0; i < headers.length; i++) {
        headerRow.createCell(i).setCellValue(headers[i]);
      }

      // data rows
      int rowNum = 1;
      for (ReverseFeedBatchItemEntity item : items) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0)
            .setCellValue(item.getReferenceId1() != null ? item.getReferenceId1() : "");
        row.createCell(1)
            .setCellValue(item.getReferenceId2() != null ? item.getReferenceId2() : "");
        row.createCell(2)
            .setCellValue(item.getTransactionStatus() != null ? item.getTransactionStatus() : "");
        row.createCell(3).setCellValue(item.getUtrNumber() != null ? item.getUtrNumber() : "");
        row.createCell(4)
            .setCellValue(
                item.getTransactionRejectionReason() != null
                    ? item.getTransactionRejectionReason()
                    : "");
        row.createCell(5).setCellValue(item.getSyncStatus() != null ? item.getSyncStatus() : "");
        row.createCell(6).setCellValue(item.getM2pResponse() != null ? item.getM2pResponse() : "");
        row.createCell(7)
            .setCellValue(item.getErrorMessage() != null ? item.getErrorMessage() : "");
        row.createCell(8)
            .setCellValue(item.getProcessedAt() != null ? item.getProcessedAt().toString() : "");
      }

      workbook.write(outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      log.error("{} error generating excel: {}", LOG_HEADER, e.getMessage(), e);
      throw new BaseException(
          "failed to generate Excel file: " + e.getMessage(),
          null,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Gets eligible loan entries for manual disbursal batch creation. Supports filtering by multiple
   * product codes. First verifies that the given product codes have RemitX-enabled partners in
   * partner_master. Only product codes with a RemitX-enabled partner are queried. If no product
   * codes are provided, returns loans for all RemitX-enabled products.
   *
   * @param productCodes list of product code filters (e.g., [CL, PL])
   * @param page zero-based page number
   * @param limit number of items per page
   * @return paginated ManualQueueResponseDTO
   */
  public Mono<ManualQueueResponseDTO> getEligibleLoansForBatch(
      List<String> productCodes, int page, int limit) {

    List<String> sanitized =
        productCodes == null
            ? List.of()
            : productCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .toList();

    log.info(
        "{} Fetching eligible loans - productCodes: {}, page: {}, limit: {}",
        MANUAL_QUEUE_LOG,
        sanitized,
        page,
        limit);

    if (sanitized.isEmpty()) {
      return getEligibleLoansForAllRemitXProducts(page, limit);
    }

    if (sanitized.size() == 1) {
      return getEligibleLoansForSingleProduct(sanitized.get(0), page, limit);
    }

    return getEligibleLoansForMultipleProducts(sanitized, page, limit);
  }

  private Mono<ManualQueueResponseDTO> getEligibleLoansForSingleProduct(
      String productCode, int page, int limit) {
    return partnerMasterRepository
        .findByProductCodeAndIsRemitXEnabled(productCode, true)
        .flatMap(
            partner -> {
              String partnerName = partner.getPartnerName();

              log.info(
                  "{} remitX-enabled partner found for productCode: {}, partnerName: {}",
                  MANUAL_QUEUE_LOG,
                  productCode,
                  partnerName);

              Mono<List<DisbursalRegistryEntity>> entriesMono =
                  registryStoreService
                      .findByProductCodeAndDisburseStatusPaginated(
                          productCode, DisbursalStatus.MANUAL_INI, page, limit)
                      .collectList();

              Mono<Long> countMono =
                  registryStoreService.countByProductCodeAndDisburseStatus(
                      productCode, DisbursalStatus.MANUAL_INI);

              return Mono.zip(entriesMono, countMono)
                  .map(
                      tuple ->
                          buildManualQueueResponse(
                              tuple.getT1(),
                              tuple.getT2(),
                              Map.of(productCode, partnerName),
                              page,
                              limit));
            })
        .switchIfEmpty(
            Mono.fromSupplier(
                () -> {
                  log.info(
                      "{} No RemitX-enabled partner found for productCode: {}, returning empty",
                      MANUAL_QUEUE_LOG,
                      productCode);
                  return ManualQueueResponseDTO.builder()
                      .data(List.of())
                      .totalCount(0L)
                      .page(page)
                      .limit(limit)
                      .hasMore(false)
                      .build();
                }))
        .doOnSuccess(
            response ->
                log.info(
                    "{} found {} eligible loans (totalCount: {}) for productCode: {}",
                    MANUAL_QUEUE_LOG,
                    response.getData().size(),
                    response.getTotalCount(),
                    productCode));
  }

  private Mono<ManualQueueResponseDTO> getEligibleLoansForMultipleProducts(
      List<String> productCodes, int page, int limit) {

    Mono<Map<String, String>> partnerMapMono =
        partnerMasterRepository
            .findByProductCodeInAndIsRemitXEnabled(productCodes, true)
            .collectList()
            .map(
                partners -> {
                  Map<String, String> map = new java.util.HashMap<>();
                  partners.forEach(p -> map.put(p.getProductCode(), p.getPartnerName()));
                  return map;
                });

    return partnerMapMono
        .flatMap(
            partnerMap -> {
              List<String> validCodes = List.copyOf(partnerMap.keySet());

              if (validCodes.isEmpty()) {
                log.info(
                    "{} No RemitX-enabled partners found for productCodes: {}, returning empty",
                    MANUAL_QUEUE_LOG,
                    productCodes);
                return Mono.just(
                    ManualQueueResponseDTO.builder()
                        .data(List.of())
                        .totalCount(0L)
                        .page(page)
                        .limit(limit)
                        .hasMore(false)
                        .build());
              }

              log.info(
                  "{} remitX-enabled partners found for productCodes: {}",
                  MANUAL_QUEUE_LOG,
                  validCodes);

              Mono<List<DisbursalRegistryEntity>> entriesMono =
                  registryStoreService
                      .findByProductCodesInAndDisburseStatusPaginated(
                          validCodes, DisbursalStatus.MANUAL_INI, page, limit)
                      .collectList();

              Mono<Long> countMono =
                  registryStoreService.countByProductCodesInAndDisburseStatus(
                      validCodes, DisbursalStatus.MANUAL_INI);

              return Mono.zip(entriesMono, countMono)
                  .map(
                      tuple ->
                          buildManualQueueResponse(
                              tuple.getT1(), tuple.getT2(), partnerMap, page, limit));
            })
        .doOnSuccess(
            response ->
                log.info(
                    "{} found {} eligible loans (totalCount: {}) for productCodes: {}",
                    MANUAL_QUEUE_LOG,
                    response.getData().size(),
                    response.getTotalCount(),
                    productCodes));
  }

  private ManualQueueResponseDTO buildManualQueueResponse(
      List<DisbursalRegistryEntity> entries,
      Long totalCount,
      Map<String, String> partnerMap,
      int page,
      int limit) {

    List<EligibleLoanDTO> data =
        entries.stream()
            .map(
                entry ->
                    EligibleLoanDTO.builder()
                        .referenceId1(entry.getReferenceId1())
                        .productCode(entry.getProductCode())
                        .disburseStatus(entry.getDisburseStatus().name())
                        .partnerName(partnerMap.getOrDefault(entry.getProductCode(), ""))
                        .grossDisbursalAmount(
                            entry.getGrossDisbursalAmount() == null
                                ? null
                                : entry.getGrossDisbursalAmount().toString())
                        .netDisbursalAmount(
                            entry.getNetDisbursalAmount() == null
                                ? null
                                : entry.getNetDisbursalAmount().toString())
                        .build())
            .toList();

    return ManualQueueResponseDTO.builder()
        .data(data)
        .totalCount(totalCount)
        .page(page)
        .limit(limit)
        .hasMore((long) (page + 1) * limit < totalCount)
        .build();
  }

  private Mono<ManualQueueResponseDTO> getEligibleLoansForAllRemitXProducts(int page, int limit) {
    log.info(
        "{} No productCode provided, fetching loans for all remitX-enabled products",
        MANUAL_QUEUE_LOG);

    Mono<Map<String, String>> partnerMapMono =
        partnerMasterRepository
            .findByIsRemitXEnabled(true)
            .collectList()
            .map(
                partners -> {
                  Map<String, String> map = new java.util.HashMap<>();
                  partners.forEach(p -> map.put(p.getProductCode(), p.getPartnerName()));
                  return map;
                });

    Mono<List<DisbursalRegistryEntity>> entriesMono =
        registryStoreService
            .findByRemitXEnabledAndDisburseStatusPaginated(DisbursalStatus.MANUAL_INI, page, limit)
            .collectList();

    Mono<Long> countMono =
        registryStoreService.countByRemitXEnabledAndDisburseStatus(DisbursalStatus.MANUAL_INI);

    return Mono.zip(partnerMapMono, entriesMono, countMono)
        .map(
            tuple -> {
              Map<String, String> partnerMap = tuple.getT1();
              List<DisbursalRegistryEntity> entries = tuple.getT2();
              Long totalCount = tuple.getT3();

              List<EligibleLoanDTO> data =
                  entries.stream()
                      .map(
                          entry ->
                              EligibleLoanDTO.builder()
                                  .referenceId1(entry.getReferenceId1())
                                  .productCode(entry.getProductCode())
                                  .disburseStatus(entry.getDisburseStatus().name())
                                  .partnerName(partnerMap.getOrDefault(entry.getProductCode(), ""))
                                  .grossDisbursalAmount(
                                      entry.getGrossDisbursalAmount() == null
                                          ? null
                                          : entry.getGrossDisbursalAmount().toString())
                                  .netDisbursalAmount(
                                      entry.getNetDisbursalAmount() == null
                                          ? null
                                          : entry.getNetDisbursalAmount().toString())
                                  .build())
                      .toList();

              return ManualQueueResponseDTO.builder()
                  .data(data)
                  .totalCount(totalCount)
                  .page(page)
                  .limit(limit)
                  .hasMore((long) (page + 1) * limit < totalCount)
                  .build();
            })
        .doOnSuccess(
            response ->
                log.info(
                    "{} found {} eligible loans (totalCount: {}) for all remitX-enabled products",
                    MANUAL_QUEUE_LOG,
                    response.getData().size(),
                    response.getTotalCount()));
  }

  /**
   * Creates a new disbursal batch. Persists the batch with IN_PROGRESS status, returns the response
   * immediately, then validates and processes referenceIds asynchronously on a bounded-elastic
   * scheduler. If validation fails (including empty/null referenceIds), the batch is marked as
   * FAILED in the background.
   *
   * @param createBatchRequest request containing the list of referenceIds
   * @param uploadedBy the user creating the batch
   * @return CreateBatchResponse with the new batchId and IN_PROGRESS status (returned immediately)
   */
  public Mono<CreateBatchResponse> createBatch(
      CreateBatchRequest createBatchRequest, String uploadedBy) {

    List<String> referenceIds = createBatchRequest.getReferenceIds();

    UUID batchId = UUID.randomUUID();
    int totalRecords = (referenceIds != null) ? referenceIds.size() : 0;
    LocalDateTime now = LocalDateTime.now(ZoneId.of(ASIA_KOLKATA));

    log.info(
        "{} creating batch {} with {} referenceIds, requested by: {}",
        CREATE_BATCH_LOG,
        batchId,
        totalRecords,
        uploadedBy);

    String validationError = validateReferenceIds(referenceIds);

    BatchStatus initialStatus =
        (validationError != null) ? BatchStatus.FAILED : BatchStatus.IN_PROGRESS;

    DisbursalBatch batch =
        DisbursalBatch.builder()
            .batchId(batchId)
            .batchStatus(initialStatus)
            .totalRecords(totalRecords)
            .createdAt(now)
            .updatedAt(now)
            .createdBy(uploadedBy)
            .errorDetails(validationError)
            .isDeleted(false)
            .build();

    return disbursalBatchRepository
        .save(batch)
        .doOnSuccess(
            savedBatch -> {
              if (savedBatch.getBatchStatus() == BatchStatus.IN_PROGRESS) {
                processCreateBatch(savedBatch, referenceIds);
              } else {
                log.info(
                    "{} batch {} saved with FAILED status: {}",
                    CREATE_BATCH_LOG,
                    savedBatch.getBatchId(),
                    validationError);
              }
            })
        .map(
            savedBatch ->
                CreateBatchResponse.builder()
                    .batchId(savedBatch.getBatchId())
                    .batchStatus(savedBatch.getBatchStatus())
                    .message(
                        savedBatch.getBatchStatus() == BatchStatus.FAILED
                            ? validationError
                            : "batch creation initiated, processing in background.")
                    .build());
  }

  private String validateReferenceIds(List<String> referenceIds) {
    if (referenceIds == null || referenceIds.isEmpty()) {
      return "referenceIds must not be empty";
    }
    if (hasDuplicates(referenceIds)) {
      Set<String> seen = new HashSet<>();
      List<String> duplicates =
          referenceIds.stream().filter(id -> !seen.add(id)).distinct().toList();
      return "referenceIds contain duplicates: " + duplicates;
    }
    return null;
  }

  /**
   * Asynchronously validates all referenceIds against the disbursal registry, then hydrates them
   * from M2P and finalizes the batch. If any referenceId is not found with MANUAL_INI status, or
   * M2P returns no data, the batch is marked as FAILED and processing stops. Runs entirely on a
   * bounded-elastic scheduler so it does not block the caller.
   *
   * <p>Note: empty/null and duplicate referenceIds are validated upfront in {@link #createBatch}
   * and the batch is saved directly with FAILED status, so they never reach this method.
   */
  private void processCreateBatch(DisbursalBatch batch, List<String> referenceIds) {
    UUID batchId = batch.getBatchId();
    log.info("{} triggering async processing for batch {}", CREATE_BATCH_LOG, batchId);
    String traceId = MDC.get(TRACE_ID);
    String partnerId = MDC.get(PARTNER_ID);

    Flux.fromIterable(referenceIds)
        .concatMap(
            referenceId ->
                registryStoreService
                    .findByReferenceId1AndDisburseStatus(
                        referenceId, DisbursalStatus.MANUAL_INI, false)
                    .switchIfEmpty(
                        Mono.error(
                            new BaseException(
                                "reference id " + referenceId + " not found with MANUAL_INI status",
                                null,
                                HttpStatus.BAD_REQUEST)))
                    .flatMap(
                        registry ->
                            partnerMasterRepository
                                .findByProductCodeAndIsRemitXEnabled(
                                    registry.getProductCode(), true)
                                .switchIfEmpty(
                                    Mono.error(
                                        new BaseException(
                                            "remitX not enabled for productCode: "
                                                + registry.getProductCode()
                                                + " (referenceId: "
                                                + referenceId
                                                + ")",
                                            null,
                                            HttpStatus.BAD_REQUEST)))
                                .thenReturn(registry)))
        .collectList()
        .doOnSuccess(
            registries ->
                log.info(
                    "{} all {} referenceIds validated for batch {}",
                    CREATE_BATCH_LOG,
                    registries.size(),
                    batchId))
        .flatMap(registries -> hydrateAndFinalizeBatch(batch, registries))
        .onErrorResume(
            error -> {
              log.error(
                  "{} error processing batch {}: {}",
                  CREATE_BATCH_LOG,
                  batchId,
                  error.getMessage());
              return markBatchAsFailed(batch, error.getMessage());
            })
        .subscribeOn(Schedulers.boundedElastic())
        .contextWrite(ctx -> ctx.put(TRACE_ID, traceId).put(PARTNER_ID, partnerId))
        .subscribe();
  }

  /**
   * Hydrates registry entries from M2P and finalizes the batch.
   *
   * <p>Phase 1 (non-transactional): Fetch M2P disbursal batch data for each referenceId via
   * external API calls. Takes the first row of each Flux. If any referenceId returns empty data, an
   * error is raised immediately and processing stops.
   *
   * <p>Phase 2 (transactional/atomic): Re-verify each referenceId still has MANUAL_INI status,
   * apply hydration data, mark the batch as COMPLETED, and update all registry entries to
   * MANUAL_BATCHED — all within a single transaction.
   */
  private Mono<Void> hydrateAndFinalizeBatch(
      DisbursalBatch batch, List<DisbursalRegistryEntity> registries) {

    UUID batchId = batch.getBatchId();
    log.info(
        "{} starting hydration for batch {} with {} entries",
        CREATE_BATCH_LOG,
        batchId,
        registries.size());

    if (registries.isEmpty()) {
      log.warn("{} no registry entries found for batch {}", CREATE_BATCH_LOG, batchId);
      return Mono.empty();
    }

    String productCode = registries.get(0).getProductCode();

    // determine product type (loan/lan based vs transaction/line based) from product config
    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlData.getT2(), TRIGGER_DISB_CTA_IDENTIFIER);

              boolean isLineBasedProduct = isLineBasedProduct(flowData);
              log.info(
                  "{} hydration flow resolved for productCode {} - lineBasedProduct: {}",
                  CREATE_BATCH_LOG,
                  productCode,
                  isLineBasedProduct);

              // phase 1: fetch m2p data for all reference_ids (external api calls, no db writes)
              return Flux.fromIterable(registries)
                  .concatMap(
                      registry -> {
                        String referenceId = registry.getReferenceId1();
                        String lineId = registry.getReferenceId2();
                        String anchorId = registry.getAnchorId();
                        log.info(
                            "{} fetching m2p disbursal batch data for referenceId: {}",
                            CREATE_BATCH_LOG,
                            referenceId);

                        Mono<DisbursalBatchDataResponse> m2pMono;
                        if (isLineBasedProduct) {
                          if (anchorId == null || anchorId.isBlank()) {
                            return Mono.error(
                                new BaseException(
                                    "anchor_id is required for line based products",
                                    null,
                                    HttpStatus.BAD_REQUEST));
                          }
                          m2pMono =
                              m2pWrapperApi
                                  .getDisbursalBatchDataM2pForLineProducts(
                                      referenceId, anchorId, lineId)
                                  .next();
                        } else {
                          m2pMono = m2pWrapperApi.getDisbursalBatchDataM2p(referenceId).next();
                        }

                        return m2pMono
                            .switchIfEmpty(
                                Mono.error(
                                    new BaseException(
                                        "no m2p disbursal batch data found for referenceId: "
                                            + referenceId,
                                        null,
                                        HttpStatus.BAD_REQUEST)))
                            .doOnSuccess(
                                data ->
                                    log.info(
                                        "{} m2p data fetched for referenceId: {}",
                                        CREATE_BATCH_LOG,
                                        referenceId))
                            .map(
                                data ->
                                    new AbstractMap.SimpleEntry<String, DisbursalBatchDataResponse>(
                                        referenceId, data));
                      })
                  .collectList()
                  .doOnSuccess(
                      pairs ->
                          log.info(
                              "{} m2p data fetched for all {} referenceIds in batch {}",
                              CREATE_BATCH_LOG,
                              pairs.size(),
                              batchId))
                  // phase 2: atomically persist hydration data and finalize the batch
                  .flatMap(
                      pairs ->
                          persistHydrationAndFinalize(
                                  batch,
                                  pairs.stream()
                                      .map(
                                          pair ->
                                              (Map.Entry<String, DisbursalBatchDataResponse>) pair)
                                      .toList())
                              .as(transactionalOperator::transactional))
                  .then();
            });
  }

  /**
   * Atomically persists hydration data and finalizes the batch. All DB operations run within a
   * single transaction:
   *
   * <ol>
   *   <li>Re-fetch each registry entry with MANUAL_INI status check (ensures no concurrent change)
   *   <li>Apply hydration data from M2P and save
   *   <li>Mark the batch as COMPLETED
   *   <li>Update all registry entries to MANUAL_BATCHED status
   * </ol>
   */
  private Mono<Void> persistHydrationAndFinalize(
      DisbursalBatch batch,
      List<Map.Entry<String, DisbursalBatchDataResponse>> referenceDataPairs) {

    UUID batchId = batch.getBatchId();
    LocalDateTime now = LocalDateTime.now(ZoneId.of(ASIA_KOLKATA));

    // step 1 & 2: re-verify manual_ini status, apply hydration data, and save
    return Flux.fromIterable(referenceDataPairs)
        .concatMap(
            pair -> {
              String referenceId = pair.getKey();
              DisbursalBatchDataResponse data = pair.getValue();

              return registryStoreService
                  .findByReferenceId1AndDisburseStatus(
                      referenceId, DisbursalStatus.MANUAL_INI, false)
                  .switchIfEmpty(
                      Mono.error(
                          new BaseException(
                              "reference ID " + referenceId + " is no longer in MANUAL_INI status",
                              null,
                              HttpStatus.CONFLICT)))
                  .flatMap(
                      registry -> {
                        applyHydrationData(registry, data, batchId, now);
                        return registryStoreService.update(registry);
                      });
            })
        .collectList()
        // step 3: mark batch as completed
        .flatMap(
            hydratedEntries -> {
              log.info(
                  "{} all {} entries hydrated for batch {}, marking COMPLETED",
                  CREATE_BATCH_LOG,
                  hydratedEntries.size(),
                  batchId);

              double totalNetAmount =
                  hydratedEntries.stream()
                      .map(DisbursalRegistryEntity::getNetDisbursalAmount)
                      .filter(java.util.Objects::nonNull)
                      .reduce(BigDecimal.ZERO, BigDecimal::add)
                      .doubleValue();

              batch.setBatchStatus(BatchStatus.COMPLETED);
              batch.setHydratedRecords(hydratedEntries.size());
              batch.setNetAmount(totalNetAmount);
              batch.setUpdatedAt(now);

              return disbursalBatchRepository.save(batch).thenReturn(hydratedEntries);
            })
        // step 4: update all registry entries to manual_batched
        .flatMapMany(Flux::fromIterable)
        .concatMap(
            entry -> {
              entry.setDisburseStatus(DisbursalStatus.MANUAL_BATCHED);
              return registryStoreService.update(entry);
            })
        .collectList()
        .doOnSuccess(
            entries ->
                log.info(
                    "{} batch {} finalized — {} entries marked as MANUAL_BATCHED",
                    CREATE_BATCH_LOG,
                    batchId,
                    entries.size()))
        .then();
  }

  /**
   * Applies hydration data from the M2P DisbursalBatchDataResponse to a DisbursalRegistryEntity.
   * Sets the batchId, all M2P-sourced fields, and hydration timestamps.
   */
  private void applyHydrationData(
      DisbursalRegistryEntity registry,
      DisbursalBatchDataResponse data,
      UUID batchId,
      LocalDateTime now) {

    registry.setBatchId(batchId);
    registry.setClientId(data.getClientId());
    registry.setClientName(data.getClientName());
    registry.setReferenceId2(data.getLoanAccountNumber());
    registry.setTransactionDate(data.getTransactionDate());
    registry.setBankAccountNumber(encryptIfPresent(data.getBankAccount()));
    registry.setIfscCode(encryptIfPresent(data.getIfscCode()));
    registry.setBankHolderName(encryptIfPresent(data.getBankHolderName()));
    registry.setBankName(data.getBankName());
    registry.setGrossDisbursalAmount(parseBigDecimal(data.getGrossDisbursalAmount()));
    registry.setNetDisbursalAmount(parseBigDecimal(data.getNetDisbursalAmount()));
    registry.setBalanceTransferOutstanding(parseBigDecimal(data.getBalanceTransferOutstanding()));
    registry.setPartner(data.getPartner());
    registry.setBalanceTransferCustomerExistingLoanId(
        data.getBalanceTransferCustomerExistingLoanId());
    registry.setAnchorId(data.getAncId());
    registry.setIsHydrated(true);
    registry.setHydratedStartedAt(now);
    registry.setHydratedCompletedAt(now);
  }

  private boolean hasDuplicates(List<String> list) {
    return list.size() != new HashSet<>(list).size();
  }

  private String encryptIfPresent(String value) {
    return (value != null && !value.isBlank()) ? encryptionUtil.encrypt(value) : value;
  }

  private String decryptIfPresent(String value) {
    return (value != null && !value.isBlank()) ? encryptionUtil.decrypt(value) : value;
  }

  /**
   * Safely parses a String to BigDecimal, returning null if the value is null, blank, or invalid.
   */
  private BigDecimal parseBigDecimal(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException e) {
      log.error("{} failed to parse big-decimal value: {}", CREATE_BATCH_LOG, value);
      return null;
    }
  }

  /** Marks an existing DisbursalBatch as FAILED with the given error details. */
  private Mono<Void> markBatchAsFailed(DisbursalBatch batch, String errorDetails) {
    batch.setBatchStatus(BatchStatus.FAILED);
    batch.setErrorDetails(errorDetails);
    batch.setUpdatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));

    return disbursalBatchRepository
        .save(batch)
        .doOnSuccess(
            saved ->
                log.info(
                    "{} batch {} marked as FAILED: {}",
                    CREATE_BATCH_LOG,
                    saved.getBatchId(),
                    errorDetails))
        .doOnError(
            error ->
                log.error(
                    "{} failed to mark batch {} as FAILED: {}",
                    CREATE_BATCH_LOG,
                    batch.getBatchId(),
                    error.getMessage(),
                    error))
        .then();
  }

  public Mono<BatchStatusResponse> getBatchStatus(UUID batchId) {
    return disbursalBatchRepository
        .findById(batchId)
        .switchIfEmpty(
            Mono.error(
                new BaseException(
                    "the requested resource is not available.",
                    "the requested resource is not available.",
                    HttpStatus.NOT_FOUND)))
        .map(
            disbursalBatch -> {
              int total = disbursalBatch.getTotalRecords();
              int hydrated =
                  disbursalBatch.getHydratedRecords() == null
                      ? 0
                      : disbursalBatch.getHydratedRecords();

              int percentage = calculatePercentage(hydrated, total);

              boolean isDownloadable = percentage == 100;

              return BatchStatusResponse.builder()
                  .batchId(disbursalBatch.getBatchId())
                  .batchStatus(disbursalBatch.getBatchStatus())
                  .hydratedRecords(hydrated)
                  .totalRecords(total)
                  .percentage(percentage)
                  .isDownloadable(isDownloadable)
                  .build();
            })
        .onErrorMap(
            ex -> {
              if (ex instanceof BaseException) {
                log.error(
                    "[GET_BATCH_STATUS] the requested resource is not available: {}", batchId);
                return ex;
              }
              log.error("[GET_BATCH_STATUS] failed to fetch batch status: {}", ex.getMessage(), ex);
              return new BaseException(
                  "failed to fetch batch status",
                  "failed to fetch batch status",
                  HttpStatus.INTERNAL_SERVER_ERROR);
            });
  }

  private int calculatePercentage(int hydrated, int total) {
    if (total == 0) {
      return 0;
    }
    return (int) Math.min(Math.round((hydrated * 100.0) / total), 100);
  }

  public Mono<BatchHistoryResponse> getBatchHistory(
      List<String> statuses, LocalDateTime fromDate, LocalDateTime toDate, int page, int limit) {

    List<String> sanitized =
        statuses == null
            ? List.of()
            : statuses.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toList());

    log.info(
        "[GET_BATCH_HISTORY] fetching batch history - statuses: {}, from: {}, to: {}, page: {},"
            + " limit: {}",
        sanitized,
        fromDate,
        toDate,
        page,
        limit);

    int offset = page * limit;

    Flux<DisbursalBatch> entriesFlux;
    Mono<Long> countMono;

    if (sanitized.isEmpty()) {
      entriesFlux =
          disbursalBatchRepository.findByDateRangePaginated(fromDate, toDate, limit, offset);
      countMono = disbursalBatchRepository.countByDateRange(fromDate, toDate);
    } else {
      entriesFlux =
          disbursalBatchRepository.findByStatusesAndDateRangePaginated(
              sanitized, fromDate, toDate, limit, offset);
      countMono = disbursalBatchRepository.countByStatusesAndDateRange(sanitized, fromDate, toDate);
    }

    return Mono.zip(entriesFlux.map(this::toBatchHistoryItemDTO).collectList(), countMono)
        .map(
            tuple -> {
              List<BatchHistoryResponse.BatchHistoryItemDTO> batches = tuple.getT1();
              Long totalCount = tuple.getT2();

              return BatchHistoryResponse.builder()
                  .batches(batches)
                  .totalCount(totalCount)
                  .page(page)
                  .limit(limit)
                  .hasMore((long) (page + 1) * limit < totalCount)
                  .build();
            })
        .onErrorMap(
            ex -> {
              log.error(
                  "[GET_BATCH_HISTORY] failed to fetch batch history: {}", ex.getMessage(), ex);
              return new BaseException(
                  "failed to fetch batch history",
                  "failed to fetch batch history",
                  HttpStatus.INTERNAL_SERVER_ERROR);
            });
  }

  private BatchHistoryResponse.BatchHistoryItemDTO toBatchHistoryItemDTO(DisbursalBatch batch) {
    return BatchHistoryResponse.BatchHistoryItemDTO.builder()
        .batchId(batch.getBatchId())
        .status(batch.getBatchStatus().name())
        .totalAmount(batch.getNetAmount())
        .createdAt(batch.getCreatedAt())
        .createdBy(batch.getCreatedBy())
        .build();
  }

  /**
   * Generates an Excel (.xlsx) file containing all disbursal registry entries for a COMPLETED
   * batch.
   *
   * @param batchId the batch UUID
   * @return byte array of the generated Excel file
   */
  public Mono<byte[]> downloadBatchExcel(String batchIdStr) {

    UUID batchId;
    try {
      batchId = UUID.fromString(batchIdStr);
    } catch (IllegalArgumentException e) {
      return Mono.error(
          new BatchDownloadException(
              "invalid batch id format", "invalid batch id format", HttpStatus.BAD_REQUEST));
    }

    log.info("{} download requested for batchId: {}", DOWNLOAD_BATCH_LOG, batchId);

    return disbursalBatchRepository
        .findById(batchId)
        .switchIfEmpty(
            Mono.error(
                new BatchDownloadException(
                    "batch not found: " + batchId,
                    "batch not found: " + batchId,
                    HttpStatus.BAD_REQUEST)))
        .flatMap(
            batch -> {
              if (batch.getBatchStatus() != BatchStatus.COMPLETED) {
                log.error(
                    "{} batch {} is not COMPLETED, current status: {}",
                    DOWNLOAD_BATCH_LOG,
                    batchId,
                    batch.getBatchStatus());
                return Mono.error(
                    new BatchDownloadException(
                        "batch is not in COMPLETED status. Current status: "
                            + batch.getBatchStatus(),
                        "batch is not in COMPLETED status",
                        HttpStatus.BAD_REQUEST));
              }
              return registryStoreService
                  .findByBatchId(batchId)
                  .collectList()
                  .flatMap(
                      registries -> {
                        if (registries.isEmpty()) {
                          log.error(
                              "{} no registry entries found for batchId: {}",
                              DOWNLOAD_BATCH_LOG,
                              batchId);
                          return Mono.error(
                              new BatchDownloadException(
                                  "no registry entries found for batch: " + batchId,
                                  "no registry entries found for batch",
                                  HttpStatus.BAD_REQUEST));
                        }
                        return Mono.fromCallable(() -> generateExcelBytes(registries))
                            .subscribeOn(Schedulers.boundedElastic());
                      });
            })
        .onErrorResume(
            error -> {
              log.error(
                  "{} failed to generate Excel for batchId: {}: {}",
                  DOWNLOAD_BATCH_LOG,
                  batchId,
                  error.getMessage());
              if (error instanceof BatchDownloadException) {
                return Mono.error(error);
              }
              return Mono.error(
                  new BatchDownloadException(
                      "download failed",
                      error.getMessage() == null ? "download failed" : error.getMessage(),
                      HttpStatus.BAD_REQUEST));
            })
        .doOnSuccess(
            bytes ->
                log.info(
                    "{} excel generated successfully for batchId: {}, size: {} bytes",
                    DOWNLOAD_BATCH_LOG,
                    batchId,
                    bytes != null ? bytes.length : 0));
  }

  /**
   * Builds the Excel workbook from registry entries and returns the byte array.
   *
   * @param registries list of disbursal registry entries
   * @return byte array of the .xlsx file
   */
  private byte[] generateExcelBytes(List<DisbursalRegistryEntity> registries) {
    String[] headers = {
      "Bank Holder Name",
      "Loan Account Number / Line Id",
      COL_TRANSACTION_DATE,
      "Lead Id / Transaction Id",
      "Disbursal Registry Id",
      "Product Code",
      "Gross Disbursal Amount",
      "Net Disbursal Amount",
      "IFSC Code",
      "Bank Name",
      "Bank Account",
      "clientId",
      "clientName",
      "BalanceTransferOutstanding",
      "Partner",
      "Balance Transfer Customer Existing loan ID",
      "anchor_id"
    };

    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Batch Data");

      // header row
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
      }

      // data rows
      for (int idx = 0; idx < registries.size(); idx++) {
        DisbursalRegistryEntity r = registries.get(idx);
        Row row = sheet.createRow(idx + 1);

        setCellValue(row, 0, decryptIfPresent(r.getBankHolderName()));
        setCellValue(row, 1, r.getReferenceId2());
        setCellValue(row, 2, r.getTransactionDate());
        setCellValue(row, 3, r.getReferenceId1());
        setCellValue(row, 4, r.getId() != null ? String.valueOf(r.getId()) : null);
        setCellValue(row, 5, r.getProductCode());
        setCellValue(
            row,
            6,
            r.getGrossDisbursalAmount() != null
                ? r.getGrossDisbursalAmount().toPlainString()
                : null);
        setCellValue(
            row,
            7,
            r.getNetDisbursalAmount() != null ? r.getNetDisbursalAmount().toPlainString() : null);
        setCellValue(row, 8, decryptIfPresent(r.getIfscCode()));
        setCellValue(row, 9, r.getBankName());
        setCellValue(row, 10, decryptIfPresent(r.getBankAccountNumber()));
        setCellValue(row, 11, r.getClientId());
        setCellValue(row, 12, r.getClientName());
        setCellValue(
            row,
            13,
            r.getBalanceTransferOutstanding() != null
                ? r.getBalanceTransferOutstanding().toPlainString()
                : null);
        setCellValue(row, 14, r.getPartner());
        setCellValue(row, 15, r.getBalanceTransferCustomerExistingLoanId());
        setCellValue(row, 16, r.getAnchorId());
      }

      workbook.write(out);
      return out.toByteArray();

    } catch (Exception ex) {
      log.error("{} error generating excel: {}", DOWNLOAD_BATCH_LOG, ex.getMessage(), ex);
      throw new BaseException(
          "failed to generate excel file",
          "failed to generate excel file",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /** Sets a cell value; writes empty string if the value is null. */
  private void setCellValue(Row row, int colIndex, String value) {
    Cell cell = row.createCell(colIndex);
    cell.setCellValue(value != null ? value : "");
  }
}
