package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.model.request.AadhaarXmlRequest;
import com.trillionloans.los.model.request.AadharXmlRequestV2;
import com.trillionloans.los.model.request.MultiConsentRequest;
import com.trillionloans.los.model.request.SelfieUpload;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.okyc.M2pAadhaarXmlResponseDTO;
import com.trillionloans.los.service.LoanApplicationV2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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

@RequestMapping("/partners/api/v2/loan")
@AllArgsConstructor
@RestController
@Tag(name = "Loan-Ops", description = "Operations related to loan applications")
@Validated
public class LoanApplicationV2Controller {

  private final LoanApplicationV2Service loanApplicationV2Service;

  @Operation(
      summary = "Upload Aadhaar XMl",
      description = "This operation Uploads Aadhaar XML against loan Id")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AadhaarXmlRequest.class)),
      description = "Aadhaar Xml request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {
                @ExampleObject(
                    name = "example",
                    value = "{\"status\":\"string\",\"source\":\"string\"}")
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
  @PostMapping(
      value = "/{loanApplicationId:\\d+}/aadhaarXml",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<M2pAadhaarXmlResponseDTO>>> uploadAadhaarXml(
      @SecureInput @Valid @RequestBody AadharXmlRequestV2 aadhaarXmlRequest,
      @PathVariable(name = "loanApplicationId") String loanApplicationId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationV2Service.uploadAadhaarXml(
                aadhaarXmlRequest, loanApplicationId, productCode)));
  }

  @Operation(
      summary = "Upload a selfie",
      description = "This operation uploads a selfie for a loan")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SelfieUpload.class)),
      description = "Image details to be provided in the request body")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"imageId\":184}")}))
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
  @Parameter(
      name = "loanApplicationId",
      description = "loanApplicationId against which selfie is to be uploaded")
  @PostMapping(
      value = "/{loanApplicationId:\\d+}/image",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> uploadSelfieAgainstLoan(
      @Valid @RequestBody SelfieUpload selfieUploadData,
      @PathVariable(name = "loanApplicationId") String loanApplicationId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationV2Service.uploadSelfieAgainstLoan(selfieUploadData, loanApplicationId)));
  }

  @Operation(
      summary = "Create Consents",
      description = "This operation creates consents against a leadId and a loanId")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)),
      description = "Consents to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
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
  @PostMapping(
      value = "/{leadId}/loans/{loanId}/consent",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> createConsents(
      @SecureInput @Valid @RequestBody MultiConsentRequest multiConsentRequest,
      @PathVariable(name = "leadId") String leadId,
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.accepted()
            .body(loanApplicationV2Service.createConsents(multiConsentRequest, leadId, loanId)));
  }

  @Operation(
      summary = "Fetch loan disbursement status report by loan Id",
      description = "This operation fetches loan disbursement status report using loan Id")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Error/Fail",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @GetMapping("/{loanId:\\d+}/disburse/status")
  public Mono<ResponseEntity<Mono<?>>> getLoanDisbursementStatus(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(loanApplicationV2Service.getLoanDisbursementStatus(loanId)));
  }
}
