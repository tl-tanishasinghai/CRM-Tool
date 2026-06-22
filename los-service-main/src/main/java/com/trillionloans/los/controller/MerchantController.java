package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.model.request.MerchantBankDetails;
import com.trillionloans.los.model.request.MerchantChangeRequest;
import com.trillionloans.los.model.request.MerchantDetailsRequest;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.service.MerchantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Controller class for handling merchant operations related endpoints. */
@RequestMapping("/partners/api/v1/merchant")
@AllArgsConstructor
@RestController
@Validated
@Tag(name = "Merchant-Ops", description = "Operations related to merchants/anchors")
public class MerchantController {
  private final MerchantService merchantService;

  /**
   * Endpoint to stamp merchant details.
   *
   * @param merchantDetailsRequest The request containing merchant details.
   * @return A Mono wrapping ResponseEntity with the outcome of merchant details stamping process.
   */
  @Operation(
      summary = "Stamps merchant details",
      description = "This operation posts the merchant(new) details")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = MerchantDetailsRequest.class)),
      description = "Merchant details to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"resourceId\":44}")}))
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
  @PostMapping(value = "/details", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<Object>>> stampMerchantDetails(
      @SecureInput @Valid @RequestBody MerchantDetailsRequest merchantDetailsRequest,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(merchantService.stampMerchantDetails(merchantDetailsRequest)));
  }

  /**
   * Endpoint to stamp merchant bank account details.
   *
   * @param merchantBankDetails The request containing merchant bank account details.
   * @return A Mono wrapping ResponseEntity with the outcome of merchant details stamping process.
   */
  @Operation(
      summary = "Stamps merchant bank account details",
      description = "This operation posts the merchant bank account details")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = MerchantBankDetails.class)),
      description = "Merchant bank account details to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {
                @ExampleObject(name = "example", value = "{\"anchorBankIdentifier\":\"44\"}")
              }))
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
  @PostMapping(value = "/{identifier}/bank-account", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<Object>>> stampMerchantBankAccountDetails(
      @SecureInput @Valid @RequestBody MerchantBankDetails merchantBankDetails,
      @PathVariable @NonNull String identifier,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            merchantService.stampMerchantBankAccountDetails(merchantBankDetails, identifier)));
  }

  /**
   * Endpoint to update merchant.
   *
   * @param merchantChangeRequest The request containing merchant details.
   * @return A Mono wrapping ResponseEntity with the outcome of merchant details update process.
   */
  @Operation(
      summary = "Updates merchant against loan application",
      description = "This operation updates the merchant/anchor against a loan application")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = MerchantChangeRequest.class)),
      description = "Merchant to be provided in the request body.")
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
  @PutMapping("/loan-application/{loanId}")
  public Mono<ResponseEntity<Mono<Object>>> updateMerchantAgainstLoanApplication(
      @SecureInput @RequestBody MerchantChangeRequest merchantChangeRequest,
      @PathVariable @NonNull String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            merchantService.updateMerchantAgainstLoanApplication(merchantChangeRequest, loanId)));
  }

  @Operation(
      summary = "Get merchant against loan application",
      description = "This operation gets the merchant/anchor against a loan application")
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
  @GetMapping("/loan-application/{loanId}")
  public Mono<ResponseEntity<Mono<Object>>> getMerchantAgainstLoanApplication(
      @PathVariable String loanId) {
    return Mono.just(ResponseEntity.ok(merchantService.getMerchantAgainstLoanApplication(loanId)));
  }
}
