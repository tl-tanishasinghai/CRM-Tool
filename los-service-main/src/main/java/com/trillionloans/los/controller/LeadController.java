package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.model.dto.BankDetailsDTO;
import com.trillionloans.los.model.dto.LeadIdResponse;
import com.trillionloans.los.model.dto.internal.LoanLevelClientDetailsCacheDTO;
import com.trillionloans.los.model.request.DedupeLeadRequest;
import com.trillionloans.los.model.request.Lead;
import com.trillionloans.los.model.request.LeadBulkDocumentsUploadRequest;
import com.trillionloans.los.model.request.LeadUpdate;
import com.trillionloans.los.model.request.PanDetails;
import com.trillionloans.los.model.request.RepaymentScheduleRequest;
import com.trillionloans.los.model.request.SelfieUpload;
import com.trillionloans.los.model.response.ClientDetailsCpResponseDto;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pBankDetailsResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pClientCreationResponseDTO;
import com.trillionloans.los.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/partners/api/v1/lead")
@AllArgsConstructor
@RestController
@Tag(name = "Lead-Ops", description = "Operations related to lead")
@Validated
public class LeadController {
  private final LeadService leadService;

  @Operation(
      summary = "Fetch a lead",
      description =
          "This operation fetch a lead and associates related details by clientId or"
              + " panDocumentKey")
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
                        "{\"clientId\":6526,\"firstName\":\"Gurunath\",\"lastName\":\"K\",\"dateOfBirth\":\"Oct2,1999\",\"gender\":\"Male\",\"mobileNo\":\"7890654365\",\"officeName\":\"HeadOffice\",\"addressType\":\"PermanentAddress\",\"addressLineOne\":\"ACS\",\"addressLineTwo\":\"SCS\",\"landmark\":\"\",\"postalCode\":\"560043\",\"panId\":5379,\"clientPandocumentkey\":\"\",\"vKYC\":\"PENDING\"}")
              }))
  @GetMapping
  public Mono<ResponseEntity<?>> getLeadDetails(@RequestParam Map<String, String> queryParams) {
    return leadService.getLeadData(queryParams).map(ResponseEntity::ok);
  }

  @Operation(
      summary = "Endpoint for new lead creation",
      description = "This operation creates a new lead and associates related details.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Lead.class)),
      description = "Lead details to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = M2pClientCreationResponseDTO.class)))
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
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> createLead(
      @SecureInput @Valid @RequestBody Lead leadData,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(leadService.initiateCreateLead(leadData, productCode)));
  }

  @Operation(
      summary = "Update a lead",
      description = "This operation updates a lead and associates related details.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LeadUpdate.class)),
      description = "Lead details to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"clientId\":0}")}))
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
  @PutMapping(value = "/{leadId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> updateLead(
      @Valid @RequestBody LeadUpdate leadData,
      @SecureInput @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(leadService.updateLead(leadData, leadId)));
  }

  @Operation(
      summary = "Fetch a lead",
      description = "This operation fetch a lead and associates related details.")
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
                        "{\"clientId\":0,\"firstName\":\"string\",\"lastName\":\"string\",\"dateOfBirth\":\"string\",\"gender\":\"string\",\"mobileNo\":\"string\",\"email\":\"string\",\"education\":\"string\",\"officeName\":\"string\",\"addressType\":\"string\",\"addressLineOne\":\"string\",\"addressLineTwo\":\"string\",\"landmark\":\"string\",\"postalCode\":\"string\",\"ffirstName\":\"string\",\"flastName\":\"string\",\"fdateOfBirth\":\"string\",\"fgender\":\"string\",\"panId\":0,\"clientPandocumentkey\":\"string\",\"bankAccountId\":0,\"accountNumber\":\"string\",\"ifscCode\":\"string\",\"name\":\"string\",\"supportedForRepayment\":true,\"supportedForDisbursement\":true,\"bankAccountType\":\"string\"}")
              }))
  @GetMapping("/{leadId}")
  public Mono<ResponseEntity<Mono<?>>> getLead(
      @Parameter(
              name = "leadId",
              description = "ID of the lead to fetch",
              required = true,
              example = "00201")
          @PathVariable
          String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(leadService.getLeadData(leadId)));
  }

  @Operation(
      summary = "Fetch a client",
      description =
          "This operation fetch a client and associates related details against loanApplicationId.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class)))
  @GetMapping("/{clientId}/loanApplicationId/{loanApplicationId}")
  public Mono<ResponseEntity<Mono<LoanLevelClientDetailsCacheDTO>>> getLoanLevelClientDetails(
      @Parameter(
              name = "clientId",
              description = "ID of the client to fetch",
              required = true,
              example = "00201")
          @PathVariable
          String clientId,
      @Parameter(
              name = "loanApplicationId",
              description = "loanApplicationId of the client",
              required = true,
              example = "00201")
          @PathVariable
          String loanApplicationId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            leadService.getLoanLevelClientDetails(clientId, loanApplicationId, productCode)));
  }

  @Operation(
      summary = "Fetch KYC identifiers",
      description = "This operation fetch KYC identifier documents like PAN, Aadhaar")
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
                        "[{\"id\":0,\"clientId\":0,\"documentType\":{\"id\":0,\"name\":\"string\",\"isActive\":false,\"mandatory\":false,\"systemIdentifier\":\"string\"},\"documentKey\":\"string\",\"status\":\"string\",\"subCategoryType\":{\"id\":0,\"name\":\"string\",\"isActive\":false,\"mandatory\":false},\"clientIdentifierVerifiedDetailsData\":{\"isVerified\":false},\"proof\":[],\"authStatus\":\"string\",\"frontImageDocument\":{\"id\":0,\"parentEntityType\":\"string\",\"parentEntityId\":0,\"name\":\"string\",\"fileName\":\"string\",\"size\":0,\"type\":\"string\",\"description\":\"string\",\"reportIdentifier\":0,\"tagIdentifier\":0,\"tagValue\":\"string\",\"tagCatergory\":\"string\",\"createdDateTime\":\"string\",\"hasPassword\":false,\"isLocked\":false},\"verificationDetails\":{\"authenticationDetails\":{\"status\":\"string\",\"nameAsPerCard\":\"string\"}}}]")
              }))
  @ApiResponse(
      responseCode = "500",
      description = "Error/Fail",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @GetMapping("/{leadId}/kyc-identifiers")
  public Mono<ResponseEntity<Flux<?>>> getKycIdentifiers(
      @Parameter(
              name = "leadId",
              description = "ID of the lead against which identifiers are to fetched",
              required = true,
              example = "00201")
          @PathVariable
          String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(leadService.getKycIdentifiersAgainstLead(leadId)));
  }

  @Operation(
      summary = "Upload a selfie",
      description = "This operation uploads a selfie against a lead")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = SelfieUpload.class)),
      description = "Lead details to be provided in the request body")
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
  @Parameter(name = "leadId", description = "leadId against which selfie is to be uploaded")
  @PostMapping(value = "/{leadId}/image", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> uploadSelfieAgainstLead(
      @Valid @RequestBody SelfieUpload selfieUploadData,
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(leadService.uploadSelfieAgainstLead(selfieUploadData, leadId)));
  }

  @Operation(
      summary = "Upload lead documents",
      description = "This operation uploads multiple documents against a lead")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LeadBulkDocumentsUploadRequest.class)),
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
                        "{\"documents\":[{\"documentId\":0,\"documentDetails\":{\"isImageQualityRequired\":false,\"nameMatchRequired\":false,\"isDOBMatchRequired\":false,\"isOcrConfidenceScoreValidationRequired\":false,\"tag\":\"ADDRESS_PROOF/ADDITIONAL_ADDRESS_PROOF\",\"document\":{\"fileName\":\"string\",\"filePath\":\"string\",\"fileType\":\"string\",\"storageType\":\"string\"}}}]}")
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
      value = "/{leadId}/upload/documents",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> uploadDocumentsAgainstLead(
      @SecureInput @Valid @RequestBody
          LeadBulkDocumentsUploadRequest leadBulkDocumentsUploadRequest,
      @RequestHeader(name = PRODUCT_CODE) String productCode,
      @PathVariable(name = "leadId") String leadId) {
    return Mono.just(
        ResponseEntity.ok(
            leadService.uploadDocumentsAgainstLead(leadBulkDocumentsUploadRequest, leadId)));
  }

  @Operation(
      summary = "Endpoint for lead dedupe check",
      description = "This operation checks if lead with given pan number already exists")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = M2pClientCreationResponseDTO.class)))
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
  @PostMapping("/dedupe")
  public Mono<ResponseEntity<Mono<?>>> deDupeLead(
      @SecureInput @Valid @RequestBody DedupeLeadRequest dedupeLeadRequest,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(leadService.leadDedupeCheck(dedupeLeadRequest)));
  }

  @Operation(
      summary =
          "Endpoint for fetching loanApplicationReferenceId, loanId and externalId associated with"
              + " given client",
      description =
          "This operation returns all of the loanApplicationReferenceId, loanId and their"
              + " externalId if available for given client")
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
  @GetMapping("/{leadId}/details")
  public Mono<ResponseEntity<Mono<?>>> getLanLoanExternalId(
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(leadService.getLoanLoanExternalId(leadId)));
  }

  @Operation(
      summary = "Fetch Bank account details of lead",
      description = "This operation fetch Bank account details of a lead")
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
                        "[{\"id\":111,\"name\":\"TESTUSER\",\"accountNumber\":\"String\",\"accountType\":{\"id\":1,\"code\":\"String\",\"value\":\"String\",\"systemCode\":\"String\"},\"ifscCode\":\"String\",\"bankName\":\"String\",\"bankCity\":\"String\",\"status\":{\"id\":200,\"code\":\"String\",\"value\":\"String\"},\"branchName\":\"String\",\"isLocked\":false,\"verificationType\":{\"id\":0,\"code\":\"String\",\"value\":\"String\",\"systemCode\":\"String\"},\"verificationStatus\":{\"id\":0,\"code\":\"String\",\"value\":\"String\",\"systemCode\":\"String\"},\"isVerified\":false,\"createdDate\":\"Jul31,2024,12:18:11PM\",\"lastModifiedDate\":\"Jul31,2024,12:18:11PM\",\"useAsDefaultAccount\":false,\"supportedForRepayment\":true,\"supportedForDisbursement\":true,\"bankAccountAssociationId\":2412}]")
              }))
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
  @GetMapping(value = "/{leadId}/bank-account-details", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Flux<M2pBankDetailsResponseDTO>>> bankAccountDetails(
      @Parameter(
              name = "leadId",
              description = "ID of the client to fetch bank account details",
              required = true,
              example = "00201")
          @PathVariable(name = "leadId")
          String leadId) {
    return Mono.just(ResponseEntity.ok(leadService.fetchBankAccountDetails(leadId)));
  }

  @Operation(
      summary = "Add bank account",
      description = "This operation adds bank account and associates it with given Lead")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BankDetailsDTO.class)),
      description = "Bank details to be provided in the request body")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {
                @ExampleObject(name = "example", value = "{\"bankAccountDetailsId\":184}")
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
  @Parameter(name = "leadId", description = "leadId against which bank account is to be added")
  @PostMapping(value = "/{leadId}/add-bank-account", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> addBankAccount(
      @SecureInput @Valid @RequestBody BankDetailsDTO bankDetails,
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(leadService.addBankAccount(bankDetails.getM2pRequestDTO(), leadId)));
  }

  @Operation(
      summary = "Fetch Repayment Schedule Without Loan",
      description = "This operation fetch repayment schedule without loan")
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
  @PostMapping("/{leadId}/repayment-schedule")
  public Mono<ResponseEntity<Mono<?>>> getRepaymentScheduleWithoutLoan(
      @Parameter(
              name = "leadId",
              description = "ID of the lead against which repayment schedule to be fetched",
              required = true,
              example = "00201")
          @PathVariable
          String leadId,
      @SecureInput @Valid @RequestBody RepaymentScheduleRequest repaymentScheduleRequest,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            leadService.getRepaymentScheduleWithoutLoan(
                repaymentScheduleRequest, productCode, leadId)));
  }

  @Operation(
      summary =
          "Endpoint for fetching loanApplicationIds, created_date and submittedon_date, "
              + "lastmodified_date associated with given client",
      description =
          "This operation returns all of the loanApplicationIds, created_date "
              + "and submittedon_date, lastmodified_date and their"
              + " externalId if available for given client")
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
  @GetMapping("/{leadId}/all-loans")
  public Mono<ResponseEntity<Mono<?>>> getLoanIdsByLeadId(
      @PathVariable(name = "leadId") String leadId,
      @RequestHeader(PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(leadService.getLoanIdsByLeadId(leadId, productCode)));
  }

  @Operation(
      summary = "Update a lead",
      description = "This operation updates a lead and associates related details.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LeadUpdate.class)),
      description = "Lead details to be provided in the request body.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "{\"clientId\":0}")}))
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
  @PutMapping(value = "/all/ucic", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Flux<?>>> updateUcic(
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(leadService.updateAllUcic(productCode)));
  }

  @Operation(
      summary = "Fetch leadInfo against mobileNumber",
      description = "This API Fetches all the details of lead against a mobileNumber",
      parameters = {@Parameter(name = "mobileNumber", required = true, example = "9815678890")},
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
  @GetMapping("/info/{mobileNumber}")
  public Mono<ResponseEntity<Mono<?>>> getLeadInfo(
      @PathVariable String mobileNumber, @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(leadService.getLeadInfo(mobileNumber)));
  }

  @Operation(
      summary = "Fetch leadId against mobileNumber and dateOfBirth",
      description = "This API Fetches leadId of leads against a mobileNumber and dateOfBirth",
      parameters = {
        @Parameter(name = "mobileNumber", required = true, example = "9815678890"),
        @Parameter(name = "dateOfBirth", required = true, example = "30-06-2003")
      },
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
  @GetMapping("/info/{mobileNumber}/{dateOfBirth}")
  public Flux<LeadIdResponse> getLeadInfoDob(
      @PathVariable String mobileNumber, @PathVariable String dateOfBirth) {
    return leadService.getLeadInfoWithDOB(mobileNumber, dateOfBirth);
  }

  @Operation(
      summary = "Fetch leadId against mobileNumber, dateOfBirth and panLast4Digits",
      description =
          "This API Fetches leadId of leads against mobileNumber, dateOfBirth and panLast4Digits",
      parameters = {
        @Parameter(name = "mobileNumber", required = true, example = "9815678890"),
        @Parameter(name = "dateOfBirth", required = true, example = "30-06-2003"),
        @Parameter(name = "panLast4Digits", required = true, example = "1234")
      },
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
  @GetMapping("/info/{mobileNumber}/{dateOfBirth}/{panLast4Digits}")
  public Flux<LeadIdResponse> getLeadInfoDobPAN(
      @PathVariable String mobileNumber,
      @PathVariable String dateOfBirth,
      @PathVariable String panLast4Digits) {
    return leadService.getLeadInfoWithDOBAndPAN(mobileNumber, dateOfBirth, panLast4Digits);
  }

  @Operation(
      summary = "update bank verification details in datatable",
      description = "update bank verification details in datatable")
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
                    value = "{\"officeId\":1,\"clientId\":7,\"resourceId\":6}")
              }))
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
  @PostMapping(
      value = "/{leadId}/verify-bank/update-data-table",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> updatePennyDropDetails(
      @Parameter(name = "leadId", description = "lead Id", required = true, example = "00201")
          @PathVariable(name = "leadId")
          String leadId,
      @SecureInput @Valid @RequestBody Object data) {
    return Mono.just(ResponseEntity.ok(leadService.updateVerifyBankDetails(leadId, data)));
  }

  @Operation(
      summary = "Upload PAN details in data table for a given lead",
      description = "Stores or updates PAN details for the provided lead ID")
  @ApiResponse(responseCode = "200", description = "PAN details updated successfully")
  @ApiResponse(
      responseCode = "400",
      description = "Invalid request",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @ApiResponse(
      responseCode = "500",
      description = "Server error",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseDTO.class)))
  @PostMapping(value = "pan-details/{leadId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Void>> uploadPanDetails(
      @PathVariable(name = "leadId") String leadId, @RequestBody PanDetails panDetails) {
    return leadService.updatePanDetails(leadId, panDetails).thenReturn(ResponseEntity.ok().build());
  }

  @Operation(
      summary = "Fetch a lead for cp portal",
      description =
          "This operation fetch a lead and associates related details with loan account number.")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ClientDetailsCpResponseDto.class),
              examples =
                  @ExampleObject(
                      name = "example",
                      value =
                          """
                          {
                            "name": "John Doe",
                            "leadId": 123456789,
                            "age": 30,
                            "dateOfBirth": "1995-07-23",
                            "address": "123 Main St, Cityville",
                            "email": "john.doe@example.com",
                            "mobileNo": "9876543210",
                            "ucic": "UCIC123456",
                            "panNumber": "ABCDE1234F",
                            "loanAccounts": [
                              "LN0001",
                              "LN0002"
                            ]
                          }
                          """)))
  @GetMapping("cp/{leadId}")
  public Mono<ResponseEntity<Mono<?>>> getCpLeadDetails(
      @Parameter(
              name = "leadId",
              description = "ID of the lead to fetch",
              required = true,
              example = "00201")
          @PathVariable
          String leadId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(leadService.getCpLeadData(leadId)));
  }

  @GetMapping("cp/rps/{leadId}/accounts/{accountNo}")
  public Mono<ResponseEntity<Mono<?>>> getCpRpsLeadDetails(
      @Parameter(
              name = "leadId",
              description = "ID of the lead to fetch",
              required = true,
              example = "00201")
          @PathVariable
          String leadId,
      @Parameter(
              name = "accountNo",
              description = "Loan account number to fetch",
              required = true,
              example = "000011603")
          @PathVariable
          String accountNo) {

    return Mono.just(ResponseEntity.ok(leadService.getCpRpsLeadData(leadId, accountNo)));
  }
}
