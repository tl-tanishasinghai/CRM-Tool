package com.trillionloans.los.service.drawdownorchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.ServerErrorException;
import com.trillionloans.los.exception.drawdown.DrawdownValidationException;
import com.trillionloans.los.exception.drawdown.InvoiceValidationException;
import com.trillionloans.los.model.dto.DrawdownInternalRequest;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.AnchorMaster;
import com.trillionloans.los.model.entity.Drawdown;
import com.trillionloans.los.model.entity.Invoice;
import com.trillionloans.los.model.request.DrawdownData;
import com.trillionloans.los.model.request.DrawdownRequest;
import com.trillionloans.los.model.request.InvoiceData;
import com.trillionloans.los.model.request.m2p.M2PDrawdownRequest;
import com.trillionloans.los.model.response.DrawdownInternalResponse;
import com.trillionloans.los.model.response.DrawdownPartnerCallbackDTO;
import com.trillionloans.los.model.response.DrawdownResponse;
import com.trillionloans.los.model.response.EnrichedDrawdownInternalResponse;
import com.trillionloans.los.model.response.InvoiceResponse;
import com.trillionloans.los.model.response.m2p.M2PDrawdownResponse;
import com.trillionloans.los.model.response.m2p.M2pErrorResponseDTO;
import io.r2dbc.postgresql.codec.Json;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
public class DrawdownUtil {
  public static final String PRODUCT_FUND = "FUND";
  public static final String PRODUCT_KCL = "KCL";

  /** Date formatter for parsing disbursed date from M2P (e.g., "Feb 5, 2026") */
  public static final DateTimeFormatter DISBURSED_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private DrawdownUtil() {}

  /**
   * Product config often stores flags as strings ("true") or numbers; only {@link Boolean#TRUE}
   * would pass {@code Boolean.TRUE.equals(...)} and skip the real BRE call.
   */
  public static boolean isMockDrawdownBreEnabled(ProductControl.Flow flowData) {
    if (flowData == null || flowData.getConditions() == null) {
      return false;
    }
    Object raw = flowData.getConditions().get("mockDrawdownBre");
    if (raw == null) {
      return false;
    }
    if (Boolean.TRUE.equals(raw)) {
      return true;
    }
    if (raw instanceof String) {
      return "true".equalsIgnoreCase(((String) raw).trim());
    }
    if (raw instanceof Number) {
      return ((Number) raw).intValue() != 0;
    }
    return false;
  }

  public enum DrawdownProcessType {
    PROCESS_INVOICES_AND_DRAWDOWN,
    PROCESS_DRAWDOWN_ONLY,
    UNSUPPORTED_PRODUCT_CODE
  }

  public static DrawdownProcessType determineDrawdownRequestType(String productCode) {
    if (StringUtils.isBlank(productCode)) {
      log.warn("[DRAWDOWN_UTIL] Product code is null or blank.");
      return DrawdownProcessType.UNSUPPORTED_PRODUCT_CODE;
    }

    return switch (productCode.trim().toUpperCase()) {
      case PRODUCT_FUND -> DrawdownProcessType.PROCESS_INVOICES_AND_DRAWDOWN;
      case PRODUCT_KCL -> DrawdownProcessType.PROCESS_DRAWDOWN_ONLY;
      default -> DrawdownProcessType.UNSUPPORTED_PRODUCT_CODE;
    };
  }

  public static Mono<DrawdownRequest> validateInvoices(DrawdownRequest request) {
    if (CollectionUtils.isEmpty(request.getInvoiceData())) {
      return error("Invoice data is required.");
    }

    return request.getInvoiceData().stream()
        .map(invoice -> getInvoiceValidationError(invoice.getRawData()))
        .filter(Objects::nonNull)
        .findFirst()
        .map(DrawdownUtil::<DrawdownRequest>error)
        .orElse(Mono.just(request));
  }

  private static String getInvoiceValidationError(InvoiceData.RawInvoiceData raw) {
    if (raw == null) return "Raw invoice data is required.";
    if (isInvalidAmount(raw.getAmount()))
      return "Invoice amount is required and it must be greater than zero.";
    if (isBlank(raw.getIdentityKey())) return "Identity key is required.";
    if (raw.getInvoiceDate() == null) return "Invoice date is required.";
    if (raw.getDueDate() == null) return "Due date is required.";

    return null;
  }

  private static boolean isInvalidAmount(BigDecimal amount) {
    return Objects.isNull(amount) || amount.signum() <= 0;
  }

  private static boolean isBlank(String str) {
    return Objects.isNull(str) || str.trim().isEmpty();
  }

  private static <T> Mono<T> error(String message) {
    return Mono.error(new InvoiceValidationException(message));
  }

  // ==================== DATE PARSING UTILITIES ====================

  /**
   * Parses disbursed date string from M2P format (e.g., "Feb 5, 2026") to LocalDate.
   *
   * @param dateStr the date string in M2P format
   * @return parsed LocalDate or null if parsing fails
   */
  public static LocalDate parseDisbursedDate(String dateStr) {
    if (StringUtils.isBlank(dateStr)) {
      return null;
    }
    try {
      return LocalDate.parse(dateStr, DISBURSED_DATE_FORMATTER);
    } catch (Exception e) {
      log.warn(
          "[DRAWDOWN_UTIL] Failed to parse disbursed date: {}. Error: {}", dateStr, e.getMessage());
      return null;
    }
  }

  // ==================== DTO BUILDER UTILITIES ====================

  /**
   * Builds minimal status response with only lineId, transactionId, and status. Used for
   * non-terminal statuses (INIT, BRE_INIT, BRE_APPROVED, OPS_APPROVAL_PENDING, etc.)
   *
   * @param drawdown the drawdown entity
   * @return minimal callback DTO
   */
  public static DrawdownPartnerCallbackDTO buildMinimalStatusResponse(Drawdown drawdown) {
    return DrawdownPartnerCallbackDTO.builder()
        .lineId(drawdown.getLineId())
        .drawdownRequestId(String.valueOf(drawdown.getId()))
        .transactionId(drawdown.getTransactionId())
        .lanId(null)
        .status(drawdown.getStatus().name())
        .receiptNumber(null)
        .approvedAmount(null)
        .netDisbursement(null)
        .disbursementDate(null)
        .rejectionReason(null)
        .build();
  }

  // ==================== ERROR HANDLING UTILITIES ====================

  /**
   * Checks if the error indicates the transaction is not in WAITING_FOR_APPROVAL state. Error
   * message pattern: "CreditLineTransaction with identifier {id} is not WAITING_FOR_APPROVAL"
   *
   * <p>Parses the ClientSideException response body to extract the M2P error details.
   *
   * @param error the throwable to check
   * @param gson Gson instance for JSON parsing
   * @return true if error indicates not waiting for approval
   */
  public static boolean isNotWaitingForApprovalError(Throwable error, Gson gson) {
    if (!(error instanceof ClientSideException clientSideException)) {
      return false;
    }

    M2pErrorResponseDTO errorResponse =
        getM2pErrorResponse(clientSideException.getResponseBody(), gson);
    if (errorResponse == null) {
      return false;
    }

    // Check developerMessage at root level
    if (containsNotWaitingForApproval(errorResponse.getDeveloperMessage())) {
      return true;
    }

    // Check developerMessage in errors list
    if (errorResponse.getErrors() != null && !errorResponse.getErrors().isEmpty()) {
      return errorResponse.getErrors().stream()
          .filter(Objects::nonNull)
          .map(M2pErrorResponseDTO.ErrorDetailDTO::getDeveloperMessage)
          .anyMatch(DrawdownUtil::containsNotWaitingForApproval);
    }

    return false;
  }

  /**
   * Checks if the message contains "is not WAITING_FOR_APPROVAL" pattern.
   *
   * @param message the message to check
   * @return true if message contains the pattern
   */
  public static boolean containsNotWaitingForApproval(String message) {
    if (StringUtils.isBlank(message)) {
      return false;
    }
    return message.contains("is not WAITING_FOR_APPROVAL")
        || message.contains("not WAITING_FOR_APPROVAL");
  }

  /**
   * Parses the response body into M2pErrorResponseDTO.
   *
   * @param responseBody the response body object
   * @param gson Gson instance for JSON parsing
   * @return parsed error response or null if parsing fails
   */
  public static M2pErrorResponseDTO getM2pErrorResponse(Object responseBody, Gson gson) {
    if (responseBody == null) {
      return null;
    }
    try {
      return gson.fromJson(gson.toJson(responseBody), M2pErrorResponseDTO.class);
    } catch (Exception e) {
      log.warn("[DRAWDOWN_UTIL] Failed to parse M2P error response: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Extracts a user-friendly error message from agreement upload failures. Handles
   * ClientSideException, ServerErrorException, BaseException, and generic Throwables. For M2P
   * errors, prefers defaultUserMessage over developerMessage.
   *
   * @param error the error from agreement upload flow
   * @param gson Gson instance for parsing M2P error response
   * @param fallback fallback message when extraction yields null or trivial value (e.g. "value")
   * @return extracted message or fallback
   */
  public static String extractAgreementUploadErrorMessage(
      Throwable error, Gson gson, String fallback) {
    if (error == null) {
      return fallback;
    }

    Object responseBody = null;
    if (error instanceof ClientSideException clientEx) {
      responseBody = clientEx.getResponseBody();
    } else if (error instanceof ServerErrorException serverEx) {
      responseBody = serverEx.getClientResponse();
    }

    if (responseBody != null) {
      M2pErrorResponseDTO m2pError = getM2pErrorResponse(responseBody, gson);
      if (m2pError != null) {
        String msg = extractMessageFromM2pError(m2pError);
        if (StringUtils.isNotBlank(msg) && isTrivialMessage(msg)) {
          return msg;
        }
      }
    }

    if (error instanceof com.trillionloans.los.exception.BaseException baseEx) {
      String msg = baseEx.getMessage();
      if (StringUtils.isNotBlank(msg) && isTrivialMessage(msg)) {
        return msg;
      }
    }

    String msg = error.getMessage();
    if (StringUtils.isNotBlank(msg) && isTrivialMessage(msg)) {
      return msg;
    }

    return fallback;
  }

  private static String extractMessageFromM2pError(M2pErrorResponseDTO m2pError) {
    if (m2pError == null) return null;
    if (StringUtils.isNotBlank(m2pError.getDefaultUserMessage())) {
      return m2pError.getDefaultUserMessage();
    }
    if (StringUtils.isNotBlank(m2pError.getDeveloperMessage())) {
      return m2pError.getDeveloperMessage();
    }
    if (m2pError.getErrors() != null && !m2pError.getErrors().isEmpty()) {
      M2pErrorResponseDTO.ErrorDetailDTO first = m2pError.getErrors().get(0);
      if (first != null && StringUtils.isNotBlank(first.getDefaultUserMessage())) {
        return first.getDefaultUserMessage();
      }
      if (first != null && StringUtils.isNotBlank(first.getDeveloperMessage())) {
        return first.getDeveloperMessage();
      }
    }
    return null;
  }

  private static boolean isTrivialMessage(String msg) {
    if (msg == null) return false;
    String trimmed = msg.trim().toLowerCase();
    return !trimmed.isEmpty() && !"value".equals(trimmed) && !"null".equals(trimmed);
  }

  /**
   * BRE request for KCL: merges {@code alternateData}, then applies the explicit keys below (same
   * shape as your KCL contract).
   */
  public static Mono<Map<String, Object>> mapToKclDrawdownBreRequest(
      String clientId, String leadId, String lineId, String drawdownID, DrawdownRequest request) {
    if (request == null) {
      return Mono.error(new DrawdownValidationException("Drawdown request cannot be null"));
    }
    DrawdownData drawdownData = request.getDrawdownData();
    if (drawdownData == null) {
      return Mono.error(new DrawdownValidationException("Drawdown data cannot be null"));
    }

    return Mono.fromCallable(
        () -> {
          Map<String, Object> input = new HashMap<>();

          Map<String, Object> alternateData =
              Optional.ofNullable(drawdownData.getAlternateData()).orElse(Collections.emptyMap());
          input.putAll(alternateData);

          input.put("external_id", leadId);
          input.put("limit_id", lineId);
          input.put(
              "requested_amount",
              drawdownData.getAmount() != null ? drawdownData.getAmount() : null);
          input.put("retailer_code", clientId);
          input.put("drawdown_id", StringUtils.defaultString(drawdownID));

          if (drawdownData.getEmiConversionDetails() != null) {
            String productShortName =
                Optional.ofNullable(drawdownData.getProductShortName())
                    .map(String::trim)
                    .orElse(StringUtils.EMPTY);

            if (productShortName.equals("TLBL")) {
              Integer tenureTLBL = drawdownData.getEmiConversionDetails().getRepaymentEvery();
              input.put("tenure", tenureTLBL);

            } else if (productShortName.equals("TLEMI")) {
              Integer tenureTLBL = drawdownData.getEmiConversionDetails().getNumberOfRepayments();
              input.put("tenure", tenureTLBL);

            } else {
              log.error("[DRAWDOWN_UTIL] Incorrect ProductShortName. Setting tenure as empty");
              input.put("tenure", StringUtils.EMPTY);
            }

            Double roi = drawdownData.getEmiConversionDetails().getInterestRatePerPeriod();
            input.put("roi", roi != null ? BigDecimal.valueOf(roi) : null);
          }

          input.put("processing_fee", BigDecimal.valueOf(0));

          String loanType =
              switch (Optional.ofNullable(drawdownData.getProductShortName())
                  .orElse(StringUtils.EMPTY)) {
                case "TLBL" -> "Bullet";
                case "TLEMI" -> "Emi";
                default -> StringUtils.EMPTY;
              };
          input.put("loan_type", loanType);

          Map<String, Object> values = new HashMap<>();
          values.put("input", input);
          Map<String, Object> result = new HashMap<>();
          result.put("values", values);
          return result;
        });
  }

  public static Mono<Map<String, Object>> mapToFundDrawdownBreRequest(
      String clientId,
      String leadId,
      String lineId,
      String drawdownID,
      AnchorMaster anchor,
      DrawdownRequest request) {
    if (request == null) {
      return Mono.error(new DrawdownValidationException("Drawdown request cannot be null"));
    }
    DrawdownData drawdownData = request.getDrawdownData();
    if (drawdownData == null) {
      return Mono.error(new DrawdownValidationException("Drawdown data cannot be null"));
    }
    if (anchor == null) {
      return Mono.error(new DrawdownValidationException("Anchor master cannot be null"));
    }

    return Mono.fromCallable(
        () -> {
          Map<String, Object> input = new HashMap<>();

          Map<String, Object> alternateData =
              Optional.ofNullable(drawdownData.getAlternateData()).orElse(Collections.emptyMap());

          // Set all keys from alternate data into input
          input.putAll(alternateData);

          input.put("external_id", leadId);
          input.put("pancard_distributor", anchor.getPan());
          input.put("limit_id", lineId);
          input.put(
              "requested_amount",
              drawdownData.getAmount() != null ? drawdownData.getAmount() : null);
          input.put("distributor_code", request.getAnchorId());
          input.put("invoice_no_repeat_flag", Boolean.FALSE);

          if (drawdownData.getEmiConversionDetails() != null) {
            String productShortName =
                Optional.ofNullable(drawdownData.getProductShortName())
                    .map(String::trim)
                    .orElse(StringUtils.EMPTY);

            if (productShortName.equals("TLBL")) {
              Integer tenureTLBL = drawdownData.getEmiConversionDetails().getRepaymentEvery();
              input.put("tenure", tenureTLBL);

            } else if (productShortName.equals("TLEMI")) {
              Integer tenureTLBL = drawdownData.getEmiConversionDetails().getNumberOfRepayments();
              input.put("tenure", tenureTLBL);

            } else {
              log.error("[DRAWDOWN_UTIL] Incorrect ProductShortName. Setting tenure as empty");
              input.put("tenure", StringUtils.EMPTY);
            }

            Double roi = drawdownData.getEmiConversionDetails().getInterestRatePerPeriod();
            input.put("roi", roi != null ? BigDecimal.valueOf(roi) : null);
          }

          input.put("processing_fee", BigDecimal.valueOf(0));
          input.put("retailer_code", clientId);

          input.put("drawdown_id", drawdownID);
          input.put("distributor_name", anchor.getAnchorName());

          String loanType =
              switch (Optional.ofNullable(drawdownData.getProductShortName())
                  .orElse(StringUtils.EMPTY)) {
                case "TLBL" -> "Bullet";
                case "TLEMI" -> "Emi";
                default -> StringUtils.EMPTY;
              };

          input.put("loan_type", loanType);

          try {
            List<Map<String, Object>> breInvoices =
                Optional.ofNullable(request.getInvoiceData())
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(Objects::nonNull)
                    .map(
                        inv -> {
                          var raw = inv.getRawData();
                          Map<String, Object> breInv = new HashMap<>();
                          if (raw != null) {
                            breInv.put("invoice_number", raw.getInvoiceNumber()); // int
                            breInv.put("invoice_date", raw.getInvoiceDate());
                            breInv.put("invoice_amount", raw.getAmount());
                            breInv.put("gst_number_invoice", raw.getGst());
                          }
                          return breInv;
                        })
                    .collect(Collectors.toList());
            input.put("invoice_data", OBJECT_MAPPER.writeValueAsString(breInvoices));
          } catch (Exception e) {
            log.warn("[DRAWDOWN_UTIL] Failed to serialize invoice data: {}", e.getMessage());
          }

          Map<String, Object> values = new HashMap<>();
          values.put("input", input);
          Map<String, Object> result = new HashMap<>();
          result.put("values", values);
          return result;
        });
  }

  public static Mono<M2PDrawdownRequest> mapToM2PRequest(
      DrawdownInternalRequest request, String drawdownId) {
    return Mono.fromCallable(
            () -> {
              DrawdownData drawdowndata = request.getDrawdownData();

              // 1. EMI Conversion Details
              M2PDrawdownRequest.EmiConversionDetails details =
                  Optional.ofNullable(drawdowndata.getEmiConversionDetails())
                      .map(
                          emi ->
                              M2PDrawdownRequest.EmiConversionDetails.builder()
                                  .numberOfRepayments(emi.getNumberOfRepayments())
                                  .productShortName(drawdowndata.getProductShortName())
                                  .interestRatePerPeriod(emi.getInterestRatePerPeriod())
                                  .loanTermFrequencyType(emi.getLoanTermFrequencyType())
                                  .repaymentFrequencyType(emi.getLoanTermFrequencyType())
                                  .repaymentEvery(emi.getRepaymentEvery())
                                  .loanTermFrequency(emi.getRepaymentEvery())
                                  .build())
                      .orElse(null);

              // 2. Charges
              List<M2PDrawdownRequest.Charge> chargesList =
                  Optional.ofNullable(drawdowndata.getCharges())
                      .orElse(Collections.emptyList())
                      .stream()
                      .map(
                          sourceCharge ->
                              M2PDrawdownRequest.Charge.builder()
                                  .amount(sourceCharge.getAmount())
                                  .chargeIdentifier(sourceCharge.getChargeIdentifier())
                                  .build())
                      .toList();

              // 3. Order & Merchant Details
              //              M2PDrawdownRequest.OrderDetails orderDetails =
              //                  Optional.ofNullable(request.getMerchantDetails())
              //                      .map(
              //                          m ->
              //                              M2PDrawdownRequest.MerchantDetails.builder()
              //                                  .merchantName(m.getMerchantName())
              //                                  .build())
              //                      .map(
              //                          mDto ->
              //                              M2PDrawdownRequest.OrderDetails.builder()
              //                                  .merchantDetails(mDto)
              //                                  .build())
              //                      .orElse(null);

              // 4. Payment Details
              M2PDrawdownRequest.PaymentDetails paymentDetails =
                  Optional.ofNullable(request.getPaymentDetails())
                      .map(
                          pd ->
                              M2PDrawdownRequest.PaymentDetails.builder()
                                  .paymentType(pd.getPaymentType())
                                  .paymentTypeId(pd.getPaymentTypeId())
                                  .build())
                      .orElse(null);

              return M2PDrawdownRequest.builder()
                  .externalId(drawdownId)
                  .amount(drawdowndata.getAmount())
                  .notes(drawdowndata.getNotes())
                  .transactionTime(Instant.now().toEpochMilli())
                  .emiConversionDetails(details)
                  .charges(chargesList)
                  //                  .orderDetails(orderDetails)
                  .paymentDetails(paymentDetails)
                  .build();
            })
        .doOnSuccess(
            m2pReq -> log.info("[DRAWDOWN] Vendor request prepared for DrawdownId: {}", drawdownId))
        .onErrorResume(
            error -> {
              log.error(
                  "[DRAWDOWN] Error while mapping to vendor request. DrawdownId: {}", drawdownId);
              return Mono.error(error);
            });
  }

  public static Mono<EnrichedDrawdownInternalResponse> mapToEnrichedDrawdownInternalResponse(
      DrawdownInternalResponse drawdownInternalResponse, M2PDrawdownResponse m2pResponse) {
    return Mono.fromCallable(
        () ->
            EnrichedDrawdownInternalResponse.builder()
                .drawdownInternalResponse(drawdownInternalResponse)
                .m2PDrawdownResponse(m2pResponse)
                .build());
  }

  public static Mono<Drawdown> mapToDrawdownEntity(
      DrawdownRequest request,
      Drawdown.DrawdownStatus status,
      String partnerId,
      String anchorId,
      String lineId) {
    return Mono.fromCallable(
            () -> {
              DrawdownData data = request.getDrawdownData();
              String jsonMetadata = OBJECT_MAPPER.writeValueAsString(data);

              return Drawdown.builder()
                  .partnerId(partnerId)
                  .anchorId(anchorId)
                  .amount(data.getAmount())
                  .status(status)
                  .transactionId(StringUtils.EMPTY)
                  .lineId(lineId)
                  .externalId(request.getExternalId())
                  .metadata(Json.of(jsonMetadata))
                  .build();
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  public static Mono<DrawdownInternalResponse> mapToDrawdownInternalResponse(
      Drawdown processedDrawdown,
      List<InvoiceResponse> processedInvoices,
      String partnerId,
      String anchorId) {

    return Mono.fromCallable(
        () -> {
          List<String> invoiceIds =
              processedInvoices.stream()
                  .map(invoice -> invoice.getId().toString())
                  .collect(Collectors.toList());

          return DrawdownInternalResponse.builder()
              .partnerId(partnerId)
              .anchorId(anchorId)
              .drawdownId(processedDrawdown.getId().toString())
              .drawdown(processedDrawdown)
              .invoiceIds(invoiceIds)
              .invoices(processedInvoices)
              .build();
        });
  }

  /**
   * Builds DrawdownResponse from a Drawdown entity. Used for idempotency responses and GET by
   * externalId.
   */
  public static DrawdownResponse buildDrawdownResponseFromEntity(Drawdown drawdown) {
    if (drawdown == null) {
      throw new IllegalArgumentException("Drawdown cannot be null");
    }
    return DrawdownResponse.builder()
        .anchorId(drawdown.getAnchorId())
        .partnerId(drawdown.getPartnerId())
        .drawdownId(drawdown.getId() != null ? String.valueOf(drawdown.getId()) : null)
        .transactionId(drawdown.getTransactionId())
        .status(drawdown.getStatus() != null ? drawdown.getStatus().name() : null)
        .externalId(drawdown.getExternalId())
        .build();
  }

  public static Mono<DrawdownResponse> mapToDrawdownResponse(
      EnrichedDrawdownInternalResponse enrichedDrawdownInternalResponse) {
    return Mono.fromCallable(
        () -> {
          if (enrichedDrawdownInternalResponse == null) {
            throw new IllegalArgumentException("EnrichedDrawdownInternalResponse cannot be null");
          }
          DrawdownInternalResponse drawdownInternalResponse =
              enrichedDrawdownInternalResponse.getDrawdownInternalResponse();

          M2PDrawdownResponse m2PDrawdownResponse =
              enrichedDrawdownInternalResponse.getM2PDrawdownResponse();

          Drawdown drawdown =
              drawdownInternalResponse != null ? drawdownInternalResponse.getDrawdown() : null;
          return DrawdownResponse.builder()
              .anchorId(
                  drawdownInternalResponse != null ? drawdownInternalResponse.getAnchorId() : null)
              .partnerId(
                  drawdownInternalResponse != null ? drawdownInternalResponse.getPartnerId() : null)
              .drawdownId(
                  drawdownInternalResponse != null
                      ? drawdownInternalResponse.getDrawdownId()
                      : null)
              .transactionId(
                  m2PDrawdownResponse != null ? m2PDrawdownResponse.getTransactionId() : null)
              .status(
                  m2PDrawdownResponse != null ? m2PDrawdownResponse.getProcessingStatus() : null)
              .externalId(drawdown != null ? drawdown.getExternalId() : null)
              .build();
        });
  }

  public static Mono<InvoiceResponse> mapToInvoiceResponse(Invoice entity) {
    if (entity == null) {
      return Mono.empty();
    }

    return Mono.fromCallable(
        () -> {
          InvoiceResponse response = new InvoiceResponse();
          response.setId(entity.getId());
          response.setAmount(entity.getAmount());
          response.setInvoiceNumber(entity.getInvoiceNumber());
          response.setInvoiceDate(entity.getInvoiceDate());

          if (entity.getMetadata() != null) {
            try {
              response.setMetadata(OBJECT_MAPPER.readTree(entity.getMetadata().asString()));
            } catch (Exception e) {
              log.error(
                  "[INVOICE][ERROR] Failed to parse metadata JSON for invoice id={}",
                  entity.getId(),
                  e);
              response.setMetadata(null);
            }
          }
          return response;
        });
  }
}
