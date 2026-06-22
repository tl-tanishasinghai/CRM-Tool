package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.model.dto.KycClientDetails;
import com.trillionloans.los.model.request.ConsentRequest;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2PCkycInfoResponse;
import com.trillionloans.los.service.KycReuseService;
import com.trillionloans.los.service.KycService;
import com.trillionloans.los.service.M2pService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RequestMapping("/partners/api/v1/kyc")
@AllArgsConstructor
@RestController
@Tag(name = "KYC", description = "Operations related to KYC")
public class KycController {
  private final KycService kycService;
  private final M2pService m2pService;
  private final KycReuseService kycReuseService;

  @Operation(
      summary = "Fetch a CKYC status",
      description = "This operation fetch CKYC status associated with lead")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "")}))
  @GetMapping("/ckyc/status/{loanId}")
  public Mono<ResponseEntity<Mono<?>>> getCkycStatus(
      @Parameter(
              name = "loanId",
              description = "ID of the loan",
              required = true,
              example = "00201")
          @PathVariable
          String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(kycService.getCkycStatus(loanId)));
  }

  @Operation(summary = "Initiate CKYC", description = "This operation CKYC against lead")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "")}))
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
  @PostMapping(value = "/ckyc/{leadId}")
  public Mono<ResponseEntity<Mono<?>>> initiateCkyc(
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(kycService.initiateCkyc(leadId)));
  }

  @Operation(
      summary = "Endpoint for fetching client ckyc info",
      description = "Endpoint for fetching client ckyc info")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content = @Content(mediaType = "application/json"))
  @ApiResponse(
      responseCode = "403",
      description = "Forbidden",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
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
  @GetMapping("/ckyc/{leadId}/info")
  public Mono<ResponseEntity<Mono<M2PCkycInfoResponse>>> getCkycInfo(
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(kycService.getCkycInfo(leadId)));
  }

  @Operation(
      summary = "Fetch a VKYC status",
      description = "This operation fetch VKYC status associated with lead")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "")}))
  @GetMapping("/vkyc/status/{loanId}")
  public Mono<ResponseEntity<Mono<Object>>> getVkycStatus(
      @Parameter(
              name = "loanId",
              description = "ID of the loan",
              required = true,
              example = "00201")
          @PathVariable
          String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(kycService.getVkycStatus(loanId)));
  }

  @Operation(summary = "Initiate VKYC", description = "This operation VKYC against lead")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "")}))
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
  @PostMapping(value = "/vkyc/{leadId}")
  public Mono<ResponseEntity<Mono<Object>>> initiateVkyc(
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(kycService.initiateVkyc(leadId)));
  }

  @GetMapping("/validation-data/client/{clientId}/loan/{loanId}")
  public Mono<?> getKycUseDetails(@PathVariable String clientId, @PathVariable String loanId) {
    return m2pService.getKycUseDetails(clientId, loanId);
  }

  @PostMapping("client/{clientId}/loan/{loanId}/kyc-reuse-consent")
  public Mono<ResponseDTO<Object>> registerKycReuseConsent(
      @Valid @RequestBody ConsentRequest requestBody,
      @PathVariable String clientId,
      @PathVariable String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return kycReuseService.registerKycReuseConsent(productCode, clientId, loanId, requestBody);
  }

  @GetMapping("fetch-aadhaar-xml-detail/client/{clientId}")
  public ResponseEntity<Mono<KycClientDetails>> getKycClientDetailsFromAaadhaar(
      @SecureInput @PathVariable(name = "clientId") String clientId) {
    return ResponseEntity.ok(kycReuseService.getKycClientDetailsFromAaadhar(clientId));
  }
}
