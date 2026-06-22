package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.model.partner.m2p.M2PGenerateCreditLineRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2PGenerateCreditLineResponseDTO;
import com.trillionloans.los.model.request.CreditLineLoanApplication;
import com.trillionloans.los.model.request.InitiateCreditLineRequestDTO;
import com.trillionloans.los.model.response.CreditLineCallbackToPartnerDTO;
import com.trillionloans.los.model.response.InitiateCreditLineResponseDTO;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanCreationResponseDTO;
import com.trillionloans.los.service.CreditLineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller for Credit Line operations.
 *
 * <p>This controller handles all credit line related endpoints including creation, generation,
 * approval, activation, and status retrieval.
 */
@RequestMapping("/partners/api/v1/credit-line")
@AllArgsConstructor
@RestController
@Tag(name = "Credit-Line-Ops", description = "Operations related to credit line management")
@Validated
public class CreditLineController {

  private final CreditLineService creditLineService;

  /**
   * Creates a credit line lead.
   *
   * <p>This endpoint creates a new credit line lead with product validation.
   *
   * @param loanData the credit line loan application data
   * @param clientId the client ID
   * @param productCode the product code from request header
   * @return Mono containing the loan creation response
   */
  @Operation(
      summary = "Create credit line lead",
      description =
          "Creates a new credit line lead after validating the product code is a valid credit line"
              + " product")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = M2pLoanCreationResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request - Invalid product code or request data",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @PostMapping(value = "/{clientId}/leads", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<M2pLoanCreationResponseDTO>>> createCreditLineLead(
      @SecureInput @Valid @RequestBody CreditLineLoanApplication loanData,
      @PathVariable(name = "clientId") String clientId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(creditLineService.createCreditLineLead(loanData, clientId, productCode)));
  }

  /**
   * Generates a credit line.
   *
   * <p>This endpoint directly creates a credit line on M2P.
   *
   * @param request the generate credit line request
   * @param leadId the lead ID
   * @param productCode the product code from request header
   * @return Mono containing the generate credit line response
   */
  @Operation(
      summary = "Generate credit line",
      description = "Directly creates a credit line on M2P for the given lead")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = M2PGenerateCreditLineResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @PostMapping(value = "/{leadId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<M2PGenerateCreditLineResponseDTO>>> generateCreditLine(
      @SecureInput @Valid @RequestBody M2PGenerateCreditLineRequestDTO request,
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(creditLineService.generateCreditLine(request, leadId, productCode)));
  }

  /**
   * Approves a credit line.
   *
   * @param leadId the lead ID
   * @param productCode the product code from request header
   * @return Mono containing the approval response
   */
  @Operation(
      summary = "Approve credit line",
      description = "Approves the credit line for the given lead")
  @ApiResponse(responseCode = "200", description = "Success")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @PostMapping(value = "/{leadId}/approve")
  public Mono<ResponseEntity<Mono<?>>> approveCreditLine(
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(creditLineService.approveCreditLine(leadId, productCode)));
  }

  /**
   * Activates a credit line.
   *
   * @param leadId the lead ID
   * @param productCode the product code from request header
   * @return Mono containing the activation response
   */
  @Operation(
      summary = "Activate credit line",
      description = "Activates the credit line for the given lead")
  @ApiResponse(responseCode = "200", description = "Success")
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @PostMapping(value = "/{leadId}/activate")
  public Mono<ResponseEntity<Mono<?>>> activateCreditLine(
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(creditLineService.activateCreditLine(leadId, productCode)));
  }

  /**
   * Fetches credit line details.
   *
   * @param leadId the lead ID
   * @param productCode the product code from request header
   * @return Mono containing the credit line details
   */
  @Operation(
      summary = "Fetch credit line",
      description = "Retrieves credit line details from M2P for the given lead")
  @ApiResponse(responseCode = "200", description = "Success")
  @ApiResponse(
      responseCode = "404",
      description = "Not Found",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @GetMapping(value = "/{leadId}")
  public Mono<ResponseEntity<Mono<?>>> fetchCreditLine(
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(creditLineService.fetchCreditLine(leadId, productCode)));
  }

  @GetMapping(value = "line/{lineId}")
  public Mono<ResponseEntity<Mono<?>>> fetchCreditLineDetailsByLineId(
      @PathVariable(name = "lineId") String lineId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(creditLineService.fetchCreditLineDetailsByLineId(lineId, productCode)));
  }

  /**
   * Initiates a credit line.
   *
   * <p>This endpoint initiates a credit line by saving the credit line details with PENDING status.
   *
   * @param request the initiate credit line request containing limit and tenure details
   * @param leadId the lead ID
   * @param productCode the product code from request header
   * @return Mono containing the response with status
   */
  @Operation(
      summary = "Initiate credit line",
      description = "Initiates a credit line by saving the credit line details with PENDING status")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InitiateCreditLineRequestDTO.class)),
      description = "Credit line initiation details")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = InitiateCreditLineResponseDTO.class)))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Error/Fail",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @PostMapping(value = "/{leadId}/initiate", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<InitiateCreditLineResponseDTO>>> initiateCreditLine(
      @SecureInput @Valid @RequestBody InitiateCreditLineRequestDTO request,
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(creditLineService.initiateCreditLine(request, leadId, productCode)));
  }

  /**
   * Gets the status of a credit line.
   *
   * <p>This endpoint retrieves the current status of a credit line from the database. Can be used
   * as an alternative to callback-based status updates.
   *
   * @param leadId the lead ID
   * @param productCode the product code from request header
   * @return Mono containing the credit line status DTO
   */
  @Operation(
      summary = "Get credit line status",
      description =
          "Retrieves the current status of a credit line from the database. This can be used as an"
              + " alternative to callback-based status updates.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = CreditLineCallbackToPartnerDTO.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Not Found - Credit line not found for the given lead",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @GetMapping(value = "/{leadId}/status")
  public Mono<ResponseEntity<Mono<CreditLineCallbackToPartnerDTO>>> getCreditLineStatus(
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(creditLineService.getCreditLineStatusByLeadId(leadId, productCode)));
  }

  @GetMapping(value = "line/{lineId}/status")
  public Mono<ResponseEntity<Mono<CreditLineCallbackToPartnerDTO>>> getCreditLineStatusByLineId(
      @PathVariable(name = "lineId") String lineId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(creditLineService.getCreditLineStatusByLineId(lineId, productCode)));
  }
}
