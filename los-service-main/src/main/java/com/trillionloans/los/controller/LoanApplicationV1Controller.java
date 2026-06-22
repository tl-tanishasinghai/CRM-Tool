package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.AttachBankDetailsDTO;
import com.trillionloans.los.model.dto.GetDocketDetailsResponseDto;
import com.trillionloans.los.model.request.AadhaarXmlRequest;
import com.trillionloans.los.model.request.AgreementDocumentUploadRequest;
import com.trillionloans.los.model.request.BulkDocumentsUploadRequest;
import com.trillionloans.los.model.request.BusinessLoanUpdateRequest;
import com.trillionloans.los.model.request.ConsentRequest;
import com.trillionloans.los.model.request.KycUploadDocumentRequest;
import com.trillionloans.los.model.request.LoanApplication;
import com.trillionloans.los.model.request.LoanApproveRequest;
import com.trillionloans.los.model.request.LoanReject;
import com.trillionloans.los.model.request.MandateRegistrationRequest;
import com.trillionloans.los.model.request.NachMandateRequest;
import com.trillionloans.los.model.request.SaveChargeRequest;
import com.trillionloans.los.model.request.SaveMiscellaneousDetailsRequest;
import com.trillionloans.los.model.request.TopupDataRequest;
import com.trillionloans.los.model.request.UpdateLoanApplication;
import com.trillionloans.los.model.response.BusinessLoanUpdateResponse;
import com.trillionloans.los.model.response.GetLoanV2ResponseDTO;
import com.trillionloans.los.model.response.MandateRegistrationDetailsResponse;
import com.trillionloans.los.model.response.MandateRegistrationResponse;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pGetKycStatusResponseDTO;
import com.trillionloans.los.model.response.okyc.M2pAadhaarXmlResponseDTO;
import com.trillionloans.los.service.LoanApplicationService;
import com.trillionloans.los.service.MandateRegistrationService;
import com.trillionloans.los.service.disbursal.DisbursalService;
import com.trillionloans.los.util.FileValidatorUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/partners/api/v1/loan")
@AllArgsConstructor
@RestController
@Tag(name = "Loan-Ops", description = "Operations related to loan applications")
@Validated
public class LoanApplicationV1Controller {

  private final LoanApplicationService loanApplicationService;
  private final DisbursalService disbursalService;
  private final MandateRegistrationService mandateRegistrationService;

  @Operation(
      summary = "Create a new loan application",
      description = "This operation creates a new loan, terms, and charges related details.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LoanApplication.class)),
      description = "Loan details to be provided in the request body.")
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
                    value =
                        "{\"clientId\":0,\"resourceId\":0,\"rollbackTransaction\":false,\"additionalResponseData\":{\"loanApplicationReferenceNo\":\"string\"}}")
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
  @PostMapping(value = "/{leadId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> createLoanApplication(
      @SecureInput @Valid @RequestBody LoanApplication loanData,
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationService.triggerLoanAppCreationBasedOnPanValidationServiceType(
                loanData, leadId, productCode)));
  }

  @Operation(
      summary = "Update a loan application",
      description = "This operation updates a loan application")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = UpdateLoanApplication.class)),
      description = "Loan update details to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"resourceId\":0}")}))
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
  @PutMapping(
      value = "/{loanId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      params = "!isBusinessLoanUpdate")
  public Mono<ResponseEntity<Mono<Object>>> updateLoanApplication(
      @Valid @RequestBody UpdateLoanApplication loanData,
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.updateLoan(loanData, loanId, productCode)));
  }

  @Operation(
      summary = "Update business loan details",
      description = "This operation updates business loan details for a loan application")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BusinessLoanUpdateResponse.class)))
  @ApiResponse(
      responseCode = "400",
      description =
          "Bad request (e.g. isBusinessLoanUpdate not true, or product is not a business loan"
              + " product); body from GlobalExceptionHandler",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Loan application or product context not found",
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
  @PutMapping(
      value = "/{loanId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      params = "isBusinessLoanUpdate")
  public Mono<ResponseEntity<BusinessLoanUpdateResponse>> updateBusinessLoanDetails(
      @Valid @RequestBody BusinessLoanUpdateRequest request,
      @PathVariable(name = "loanId") String loanId,
      @RequestParam(name = "isBusinessLoanUpdate") Boolean isBusinessLoanUpdate) {
    return loanApplicationService.updateBusinessLoanDetailsAsResponse(
        loanId, request.getBusinessLoanDetails(), isBusinessLoanUpdate);
  }

  @Operation(summary = "Reject loan", description = "This operation rejects a loan application")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LoanReject.class)),
      description = "Loan details to be provided in the request body.")
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
                    value =
                        "{\"officeId\":0,\"clientId\":0,\"resourceId\":0,\"changes\":{\"statusEnum\":0}}")
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
  @PutMapping(value = "/{loanId}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> rejectLoanApplication(
      @SecureInput @Valid @RequestBody LoanReject rejectionData,
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.rejectLoanApplication(rejectionData, loanId)));
  }

  @Operation(
      summary = "Fetch loan applications",
      description = "This operation fetches loan applications against a lead")
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
                    value =
                        "[{\"loanApplicationReferenceId\":0,\"loanApplicationReferenceNo\":\"string\",\"clientName\":\"string\",\"status\":{\"id\":0,\"code\":\"string\",\"value\":\"string\"},\"accountType\":{\"id\":0,\"code\":\"string\",\"value\":\"string\"},\"loanProductId\":0,\"loanProductName\":\"string\",\"loanAmountRequested\":0,\"isCoApplicant\":false,\"isStalePeriodExceeded\":false,\"isWorkflowArchived\":false,\"allowUpfrontCollection\":false,\"allowsDisbursementToGroupBankAccounts\":false,\"isFlatInterestRate\":false,\"synRepaymentWithMeeting\":false,\"isTopup\":false,\"subStatus\":0,\"leadType\":\"string\",\"losProductKey\":\"string\"}]")
              }))
  @ApiResponse(
      responseCode = "500",
      description = "Error/Fail",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @GetMapping("/{leadId}")
  public Mono<ResponseEntity<Mono<?>>> getLoanApplications(
      @Parameter(
              name = "leadId",
              description = "ID of the lead against which loans are to be fetched",
              required = true,
              example = "00201")
          @PathVariable
          String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(loanApplicationService.getLoanApplications(leadId)));
  }

  @Operation(
      summary = "Fetch loan application by loan Id",
      description = "This operation fetches loan application by loan Id")
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
                    value =
                        "{\"loanApplicationReferenceId\":0,\"loanApplicationReferenceNo\":\"string\",\"clientId\":0,\"loanOfficerId\":0,\"loanOfficerName\":\"string\",\"status\":{\"id\":0,\"code\":\"string\",\"value\":\"string\"},\"accountType\":{\"id\":0,\"code\":\"string\",\"value\":\"string\"},\"loanProductId\":0,\"loanProductName\":\"string\",\"loanPurpose\":{\"isActive\":false,\"mandatory\":false},\"loanAmountRequested\":0,\"numberOfRepayments\":0,\"repaymentPeriodFrequency\":{\"id\":0,\"code\":\"string\",\"value\":\"string\"},\"repayEvery\":0,\"termPeriodFrequency\":{\"id\":0,\"code\":\"string\",\"value\":\"string\"},\"termFrequency\":0,\"calculatedEmiAmount\":0,\"interestRatePerPeriod\":40,\"isCoApplicant\":false,\"isCreditBureauProduct\":false,\"isStalePeriodExceeded\":false,\"workflowId\":0,\"isWorkflowEnabled\":false,\"isWorkflowArchived\":false,\"allowUpfrontCollection\":false,\"allowsDisbursementToGroupBankAccounts\":false,\"isFlatInterestRate\":false,\"synRepaymentWithMeeting\":false,\"isTopup\":false,\"stage\":{\"isActive\":false,\"mandatory\":false},\"loanApplicationAdditionalDetailsData\":{\"sourcingChannel\":{\"isActive\":false,\"mandatory\":false}},\"appliedLoanAmount\":0,\"loanOfficeId\":0,\"loanOfficeName\":\"string\",\"graceOnPrincipalPayment\":0,\"graceOnInterestPayment\":0,\"graceOnInterestCharged\":0,\"leadType\":\"string\",\"losProductKey\":\"string\",\"losProductData\":{\"id\":0,\"name\":\"string\",\"shortName\":\"string\",\"productKey\":\"string\",\"isActive\":true,\"applicantType\":\"string\",\"leadType\":\"string\",\"isVclBusinessType\":false,\"anchors\":[],\"currencyData\":{\"currencyDecimalPlaces\":0,\"currencyInMultiplesOf\":0},\"isAnchorSupported\":false},\"loanAmountApproved\":0,\"repeatsOnDayOfMonth\":[],\"workFlowBasicDetails\":{\"parentTaskId\":0,\"currentTaskId\":0,\"currentTaskStatus\":\"string\",\"currentTaskName\":\"string\"}}")
              }))
  @ApiResponse(
      responseCode = "500",
      description = "Error/Fail",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @GetMapping("/application/{loanId}")
  public Mono<ResponseEntity<Mono<Object>>> getLoanApplicationByLoanId(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.getLoanApplicationByLoanIdV3(loanId)));
  }

  @Operation(
      summary = "Fetch loan application by loan Id v2",
      description = "This operation fetches loan application by loan Id v2")
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
                    value =
                        "{\"loanId\":32,\"loanApplicationId\":1,\"clientId\":1,\"utrNumber\":\"0\",\"approvedAmount\":10000,\"netDisburseAmount\":9500,\"disbursedDate\":\"2024-01-31\",\"disbursalBankAccount\":\"000190600017041\",\"loanApplicationStatus\":\"loanApplicationNumberNotPresent\"}")
              }))
  @ApiResponse(
      responseCode = "500",
      description = "Error/Fail",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @GetMapping("/application/get/{loanId}")
  public Mono<ResponseEntity<Mono<GetLoanV2ResponseDTO>>> getLoanApplicationByLoanIdV2(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.getLoanApplicationByLoanIdV2(loanId)));
  }

  @Operation(
      summary = "Fetch document by loanId & documentId",
      description = "This operation fetches document by loanId & documentId")
  @GetMapping("/application/{loanId}/document/{documentId}")
  public Mono<ResponseEntity<Mono<ByteArrayResource>>> getDocumentByLoanIdAndDocumentId(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          String loanId,
      @PathVariable("documentId") String documentId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .body(loanApplicationService.getDocumentByLoanIdAndDocumentId(loanId, documentId)));
  }

  @Operation(
      summary = "Fetch Enach mandate against a loanId",
      description = "This operation fetches Enach mandate against a loanId")
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
                    value =
                        "[{\"requestId\":0,\"entityType\":\"string\",\"entityId\":0,\"status\":\"string\",\"umrn\":\"string\",\"bankAccountHolderName\":\"string\",\"bankName\":\"string\",\"branchName\":\"string\",\"bankAccountNumber\":\"string\",\"micr\":\"string\",\"ifsc\":\"string\",\"accountTypeEnum\":{\"id\":0,\"code\":\"string\",\"value\":\"string\"},\"periodUntilCancelled\":false,\"debitTypeEnum\":{\"id\":0,\"code\":\"string\",\"value\":\"string\"},\"amount\":0,\"debitFrequencyEnum\":{\"id\":0,\"code\":\"string\",\"value\":\"string\"},\"channel\":\"string\",\"mode\":\"string\",\"createdOn\":\"string\",\"isPhysicalFileUploaded\":false}]")
              }))
  @ApiResponse(
      responseCode = "500",
      description = "Error/Fail",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @GetMapping("/application/{loanId}/enach-mandate")
  public Mono<ResponseEntity<Flux<?>>> getNachMandateRequest(
      @Parameter(name = "loanId", description = "Loan Id", example = "00201") @PathVariable
          String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(loanApplicationService.getNachMandateRequest(loanId)));
  }

  @Operation(
      summary = "Send Enach mandate against a loanId",
      description = "This operation sends Enach mandate against a loanId")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = NachMandateRequest.class)),
      description = "Nach mandate details to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"resourceId\":243}")}))
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
      value = "/application/{loanId}/enach-mandate",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> uploadNachMandateRequest(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          String loanId,
      @SecureInput @Valid @RequestBody NachMandateRequest nachMandateRequest,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationService.uploadNachMandateRequest(
                loanId, nachMandateRequest, productCode)));
  }

  @Operation(
      summary = "Upload a Agreement documents of a loan",
      description = "This operation uploads a Agreement documents against a loan")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "multipart/form-data",
              schema = @Schema(implementation = AgreementDocumentUploadRequest.class)),
      description = "name, tagIdentifier and file needs to be provided in the request body")
  @Parameter(
      name = "loanId",
      description = "loanId against which agreement document is to be uploaded")
  @PostMapping(value = "/application/{loanId}/upload/agreement")
  public Mono<ResponseEntity<?>> uploadAgreementDocumentByLoanId(
      @RequestPart("name") String name,
      @RequestPart("tagIdentifier") String tagIdentifier,
      @SecureInput @Valid @RequestPart("file") FilePart file,
      @PathVariable("loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return FileValidatorUtil.hasValidExtensionAndMimeType(file)
        .flatMap(
            isValid -> {
              if (Boolean.TRUE.equals(isValid)) {
                AgreementDocumentUploadRequest request =
                    AgreementDocumentUploadRequest.builder()
                        .name(name)
                        .tagIdentifier(tagIdentifier)
                        .file(file)
                        .build();

                return loanApplicationService
                    .uploadAgreementDocumentAgainstLoan(request, loanId)
                    .map(ResponseEntity::ok);
              } else {
                return Mono.error(new IllegalArgumentException("Invalid file type or content"));
              }
            });
  }

  @Operation(
      summary = "Upload Aadhaar XMl",
      description = "This operation Uploads Aadhaar XML against lead Id")
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
  @PostMapping(value = "/{leadId}/upload/aadhaarXml", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<M2pAadhaarXmlResponseDTO>>> uploadAadhaarXml(
      @SecureInput @Valid @RequestBody AadhaarXmlRequest aadhaarXmlRequest,
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationService.uploadAadhaarXml(aadhaarXmlRequest, leadId, productCode)));
  }

  @Operation(
      summary = "Upload a KYC documents of a loan",
      description = "This operation uploads a KYC documents against a lead Id")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "multipart/form-data",
              schema = @Schema(implementation = KycUploadDocumentRequest.class)),
      description = "name, tagIdentifier and file needs to be provided in the request body")
  @Parameter(name = "leadId", description = "leadId against which KYC document is to be uploaded")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"status\":\"string\"}")}))
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
  @PostMapping(value = "/application/{kycId}/upload/kyc/lead/{leadId}")
  public Mono<ResponseEntity<Mono<?>>> uploadKycDocumentAgainstLeadId(
      @SecureInput @Valid @RequestPart("file") FilePart file,
      @RequestPart("name") String name,
      @RequestPart("side") String side,
      @RequestPart("tagIdentifier") String tagIdentifier,
      @PathVariable("leadId") String leadId,
      @PathVariable("kycId") String kycId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {

    return FileValidatorUtil.hasValidExtensionAndMimeType(file)
        .flatMap(
            isValid -> {
              if (Boolean.TRUE.equals(isValid)) {
                KycUploadDocumentRequest kycUploadDocumentRequestBody =
                    KycUploadDocumentRequest.builder()
                        .name(name)
                        .tagIdentifier(tagIdentifier)
                        .file(file)
                        .side(side)
                        .build();

                return Mono.just(
                    ResponseEntity.ok(
                        loanApplicationService.uploadKycDocumentAgainstLeadId(
                            kycUploadDocumentRequestBody, leadId, kycId)));
              } else {
                return Mono.error(new IllegalArgumentException("Invalid file type or content"));
              }
            });
  }

  @Operation(
      summary = "Create a Consent",
      description = "This operation creates a consent against a leadId and a loanId")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ConsentRequest.class)),
      description = "Consent details to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"id\":0}")}))
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
  public Mono<ResponseEntity<Mono<?>>> createConsent(
      @SecureInput @Valid @RequestBody ConsentRequest consentRequest,
      @PathVariable(name = "leadId") String leadId,
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.createConsent(consentRequest, leadId, loanId)));
  }

  @Operation(
      summary = "Upload loan documents",
      description = "This operation uploads multiple documents against a loan")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BulkDocumentsUploadRequest.class)),
      description = "Documents to be provided in the request body.")
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
                    value =
                        "{\"documents\":[{\"documentId\":0,\"documentDetails\":{\"isImageQualityRequired\":false,\"nameMatchRequired\":false,\"isDOBMatchRequired\":false,\"isOcrConfidenceScoreValidationRequired\":false,\"tag\":\"KFS/LOAN_AGREEMENT\",\"document\":{\"fileName\":\"string\",\"filePath\":\"string\",\"fileType\":\"string\",\"storageType\":\"string\"}}}]}")
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
      value = "/application/{loanId}/upload/documents",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> uploadDocumentsAgainstLoan(
      @SecureInput @Valid @RequestBody BulkDocumentsUploadRequest bulkDocumentsUploadRequest,
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationService.uploadDocumentsAgainstLoan(
                bulkDocumentsUploadRequest, loanId, productCode)));
  }

  /**
   * Retrieves the repayment schedule for a given loan ID.
   *
   * @param loanId The ID of the loan for which the repayment schedule is requested.
   * @return A Mono containing the repayment schedule response.
   */
  @Operation(
      summary = "Fetch Repayment Schedule",
      description = "This API fetches Repayment Schedule data by Loan id",
      parameters = {@Parameter(name = "loanId", required = true)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping("/{loanId}/repaymentSchedule")
  public Mono<ResponseEntity<Mono<?>>> getRepaymentScheduleByLoanId(
      @PathVariable String loanId, @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.getRepaymentScheduleByLoanId(loanId)));
  }

  @Operation(
      summary = "Disbursement API",
      description = "This operation disburse loan against loanId and leadId")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"status\":\"string\"}")}))
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
  @PostMapping("/{loanId}/leads/{leadId}/disburse")
  public Mono<ResponseEntity<Mono<?>>> triggerDisbursement(
      @PathVariable(name = "loanId") String loanId,
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(disbursalService.triggerDisbursement(loanId, leadId, productCode)));
  }

  @Operation(
      summary = "BRE step completion",
      description = "This operation completes the bre step in the workflow")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"status\":\"string\"}")}))
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
  @PostMapping("/{loanId}/bre-completion")
  public Mono<ResponseEntity<Mono<?>>> completeBreStep(
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.completeBreStep(loanId, productCode)));
  }

  @Operation(
      summary = "Start Kyc Step Completion",
      description = "This operation completes the start-kyc step in the workflow")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"status\":\"string\"}")}))
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
  @PostMapping("/{loanId}/start-kyc")
  public Mono<ResponseEntity<Mono<?>>> completeStartKycStep(
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.completeStartKycStep(loanId, productCode)));
  }

  @Operation(
      summary = "Add Charge",
      description = "This operation add charge against loan application")
  @PostMapping("/{loanId}/charge")
  public Mono<ResponseEntity<Mono<?>>> addCharge(
      @PathVariable(name = "loanId") String loanId,
      @SecureInput @Valid @RequestBody SaveChargeRequest saveChargeRequest,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.addCharges(saveChargeRequest, loanId)));
  }

  @Operation(
      summary = "Undo Approve loan",
      description = "This operation undoes the approved loan application",
      parameters = {@Parameter(name = "loanId", required = true)},
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "404",
            description = "Loan application reference with given identifier does not exist",
            content = @Content(mediaType = "application/json"))
      })
  @PutMapping(value = "/{loanId}/undo-approve")
  public Mono<ResponseEntity<Mono<?>>> undoApproveLoanApplication(
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(loanApplicationService.undoApproveLoan(loanId)));
  }

  @Operation(
      summary = "Approve loan",
      description = "This operation approves a loan application",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "404",
            description = "Loan application reference with given identifier does not exist",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LoanApproveRequest.class)),
      description = "Loan Approval details to be provided in the request body.")
  @PutMapping(value = "/{loanId}/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<Object>>> approveLoanApplication(
      @Valid @RequestBody LoanApproveRequest approveData,
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationService.approveLoanWithValidation(
                loanId, approveData.getApprovedDate())));
  }

  @Operation(
      summary = "Add Top-up data",
      description = "This operation adds top up data",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "404",
            description = "Loan application reference with given identifier does not exist",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TopupDataRequest.class)),
      description = "top up data needs to be provided in the request body.")
  @PostMapping(value = "/{loanId}/topupdata", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> addTopUpData(
      @SecureInput @Valid @RequestBody TopupDataRequest topupDataRequest,
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.addTopUpDataTable(topupDataRequest, loanId)));
  }

  @Operation(
      summary = "Fetch BRE Status",
      description = "This operation fetches BRE Status against a lead")
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
                    value =
                        "{\"active\": false,  \"status\": \"SUCCESS\",  \"stage\": \"COMPLETED\"}")
              }))
  @ApiResponse(
      responseCode = "500",
      description = "Error/Fail",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @GetMapping("/{leadId}/bre-status")
  public Mono<ResponseEntity<Mono<?>>> getBreStatus(
      @Parameter(
              name = "leadId",
              description = "ID of the lead against which BRE status to be fetched",
              required = true,
              example = "00201")
          @PathVariable
          String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(loanApplicationService.getBreStatus(leadId)));
  }

  @Operation(
      summary = "Attach bank account details at loan level!",
      description = "This operation attaches bank account details to a given loan application")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AttachBankDetailsDTO.class)),
      description = "Bank  details to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"bankId\":243}")}))
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
      value = "/{loanId}/lead/{leadId}/attach-bank-account",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> attachBankAccount(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          String loanId,
      @Parameter(name = "leadId", description = "Lead Id", required = true, example = "1001")
          @PathVariable
          String leadId,
      @SecureInput @Valid @RequestBody AttachBankDetailsDTO attachBankDetailsDTO,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    // Validate loanId and leadId
    if (loanId.equals("null") || loanId.trim().isEmpty()) {
      throw new BaseException(
          "Loan Id must not be empty or null!",
          "Loan Id must not be empty or null!",
          HttpStatus.BAD_REQUEST);
    }

    if (leadId.equals("null") || leadId.trim().isEmpty()) {
      throw new BaseException(
          "Lead Id must not be empty or null!",
          "Lead Id must not be empty or null!",
          HttpStatus.BAD_REQUEST);
    }
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationService.attachBankAccountProductWise(
                loanId, leadId, attachBankDetailsDTO, productCode)));
  }

  @Operation(
      summary = "Fetch bank account details by loan Id",
      description =
          "This operation fetches bank account details mapped at loan level using loan Id")
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
  @GetMapping("/{loanId}/fetch-bank-details")
  public Mono<ResponseEntity<Mono<?>>> getBankDetailsFromLoanLevelDataTable(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.getBankDetailsFromLoanLevelDataTable(loanId)));
  }

  @Operation(summary = "Add Co-applicant", description = "This operation adds co-applicant")
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
                    value = "{\"officeId\":0,\"clientId\":0,\"resourceId\":0,\"subResourceId\":0}")
              }))
  @ApiResponse(
      responseCode = "400",
      description = "Bad request",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @ApiResponse(
      responseCode = "404",
      description = "Loan application with given identifier does not exist",
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
  @PostMapping("/{loanId}/leads/{leadId}/co-applicant")
  public Mono<ResponseEntity<Mono<?>>> addCoApplicant(
      @PathVariable(name = "loanId") String loanId,
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(loanApplicationService.addCoApplicant(loanId, leadId, productCode)));
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
  @GetMapping("/{loanId}/disburse/status")
  public Mono<ResponseEntity<Mono<?>>> getLoanDisbursementStatus(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(loanApplicationService.getLoanDisbursementStatus(loanId)));
  }

  @Operation(
      summary = "Upload OKYC Aadhaar XMl",
      description = "This operation Uploads OKYC Aadhaar XML against lead Id")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AadhaarXmlRequest.class)),
      description = "OKYC Aadhaar Xml request body.")
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
      value = "/{leadId}/upload/okyc-aadhaar-xml",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<M2pAadhaarXmlResponseDTO>>> uploadOkycAadhaarXml(
      @Valid @RequestBody AadhaarXmlRequest okycAadhaarXmlRequest,
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationService.uploadOkycAadhaarXml(
                okycAadhaarXmlRequest, leadId, productCode)));
  }

  @Operation(
      summary = "Fetch documents",
      description = "This API Fetches the list of all documents against a loanApplicationId",
      parameters = {@Parameter(name = "loanApplicationId", required = true, example = "890")},
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
  @GetMapping("/{loanApplicationId}/documents")
  public Mono<ResponseEntity<Mono<?>>> getDocumentList(
      @PathVariable String loanApplicationId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(loanApplicationService.getDocumentList(loanApplicationId)));
  }

  @Operation(
      summary = "Retry Risk Categorization Failure",
      description = "This operation retry risk categorization for failed loan application id")
  @ApiResponse(
      responseCode = "200",
      description = "Bad Request",
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
  @PostMapping("/retry-risk-categorization")
  public Flux<?> retryRiskCategorizationFailures() {
    return loanApplicationService.retryRiskCategorizationFailureCase();
  }

  @Operation(
      summary = "Fetch KYC status check report by loan Id",
      description = "This operation fetches KYC status report using loan Id")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = M2pGetKycStatusResponseDTO.class)))
  @ApiResponse(
      responseCode = "404",
      description = "KYC record not found",
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
  @GetMapping("/{loanId:\\d+}/kyc-status")
  public Mono<ResponseEntity<Mono<M2pGetKycStatusResponseDTO>>> getKycStatus(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(loanApplicationService.getKycStatus(loanId)));
  }

  @Operation(
      summary = "Freeze loan application",
      description = "This operation approves a loan application and hits LAA CTA",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful operation",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "404",
            description = "Loan application reference with given identifier does not exist",
            content = @Content(mediaType = "application/json"))
      })
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LoanApproveRequest.class)),
      description = "Loan Approval details to be provided in the request body.")
  @PostMapping(
      value = "/freeze-loan-application/{loanId}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<Object>>> freezeLoanApplication(
      @Valid @RequestBody LoanApproveRequest approveData,
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationService.approveLoanAndTriggerCta(
                loanId, approveData.getApprovedDate())));
  }

  @Operation(
      summary = "Trillion's Mandate registration on loan application",
      description = "This operation create mandate registration on loan application at Trillion")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = MandateRegistrationRequest.class)),
      description = "Nach mandate details to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = MandateRegistrationResponse.class),
              examples = {@ExampleObject(name = "example", value = "{\"resourceId\":243}")}))
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
      value = "/{loanId}/leadId/{leadId}/register/mandate",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<MandateRegistrationResponse>>> createMandateRegistration(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          @Size(
              max = 100,
              message = "[MandateRegistrationRequest] loanId exceeds max length of 100")
          String loanId,
      @Parameter(name = "leadId", description = "Lead Id", required = true, example = "00201")
          @PathVariable
          @Size(
              max = 100,
              message = "[MandateRegistrationRequest] leadId exceeds max length of 100")
          String leadId,
      @SecureInput @Valid @RequestBody MandateRegistrationRequest mandateRegistrationRequest,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            mandateRegistrationService.createMandateRegistration(
                loanId, leadId, mandateRegistrationRequest, productCode)));
  }

  @Operation(
      summary = "Fetch mandate status by loan Id, lead Id and mandate Id",
      description = "This operation fetches mandate status by loan Id, lead Id and mandate Id")
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
  @ApiResponse(
      responseCode = "400",
      description = "Error/Fail",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @GetMapping("/{loanId}/leadId/{leadId}/mandate/{mandateId}/status")
  public Mono<ResponseEntity<Mono<MandateRegistrationDetailsResponse>>> fetchMandateRegistration(
      @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
          @PathVariable
          @Size(
              max = 100,
              message = "[MANDATE_REGISTRATION_DETAILS] loanId exceeds max length of 100")
          String loanId,
      @Parameter(name = "leadId", description = "Lead Id", required = true, example = "00201")
          @PathVariable
          @Size(
              max = 100,
              message = "[MANDATE_REGISTRATION_DETAILS] mandateId exceeds max length of 200")
          String leadId,
      @Parameter(name = "mandateId", description = "Mandate Id", required = true, example = "00201")
          @PathVariable
          @Size(
              max = 100,
              message = "[MANDATE_REGISTRATION_DETAILS] loanId exceeds max length of 100")
          String mandateId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            mandateRegistrationService.fetchMandateRegistration(
                leadId, loanId, mandateId, productCode)));
  }

  @Operation(
      summary = "Fetch loan details report by loan Id",
      description = "This operation fetches loan details report using loan Id")
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
  @GetMapping("/{loanId}/docketDetails")
  public Mono<ResponseEntity<Mono<GetDocketDetailsResponseDto>>>
      getLoanAndClientDetailsForDocketPopulationByLoanId(
          @Parameter(name = "loanId", description = "Loan Id", required = true, example = "00201")
              @PathVariable
              String loanId,
          @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            loanApplicationService.getLoanAndClientDetailsForDocketPopulationByLoanId(loanId)));
  }

  @Operation(
      summary = "Save miscellaneous details for client and/or loan application",
      description =
          "This operation saves miscellaneous details for client and/or loan application."
              + " Both client and loan application miscellaneous details are optional."
              + " ClientId is fetched from loanApplicationId."
              + " Data is saved transactionally to maintain atomicity.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SaveMiscellaneousDetailsRequest.class)),
      description = "Miscellaneous details to be saved")
  @ApiResponse(
      responseCode = "200",
      description = "Success - Data saved successfully",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class),
              examples = {@ExampleObject(name = "success", value = "{\"status\":\"SUCCESS\"}")}))
  @ApiResponse(
      responseCode = "400",
      description = "Bad Request",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @PostMapping("/{loanApplicationId}/miscellaneous-details")
  public Mono<ResponseEntity<Map<String, String>>> saveMiscellaneousDetails(
      @Parameter(
              name = "loanApplicationId",
              description = "Loan Application Id",
              required = true,
              example = "12345")
          @PathVariable
          String loanApplicationId,
      @RequestBody SaveMiscellaneousDetailsRequest request) {
    return loanApplicationService.saveMiscellaneousDetails(
        loanApplicationId,
        request.getClientMiscellaneousDetails(),
        request.getLoanApplicationMiscellaneousDetails());
  }
}
