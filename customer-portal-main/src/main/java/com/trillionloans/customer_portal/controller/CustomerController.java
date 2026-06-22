package com.trillionloans.customer_portal.controller;

import com.trillionloans.customer_portal.configuration.EncryptResponse;
import com.trillionloans.customer_portal.configuration.ProtectedPath;
import com.trillionloans.customer_portal.constant.LoanStatus;
import com.trillionloans.customer_portal.constant.ResponseStatus;
import com.trillionloans.customer_portal.model.dto.ClientConsentDTO;
import com.trillionloans.customer_portal.model.dto.LogoutRequestDTO;
import com.trillionloans.customer_portal.model.dto.SubmitFormRequest;
import com.trillionloans.customer_portal.model.response.ResponseDTO;
import com.trillionloans.customer_portal.service.AuthOTPService;
import com.trillionloans.customer_portal.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Validated
@RequestMapping("/customer")
public class CustomerController {

  private final CustomerService customerService;
  private final AuthOTPService authOTPService;

  @Autowired
  public CustomerController(CustomerService customerService, AuthOTPService authOTPService) {
    this.customerService = customerService;
    this.authOTPService = authOTPService;
  }

  @Operation(
      summary = "Fetch lead profile",
      description =
          "This operation fetch a lead and associates related details required for the customer"
              + " portal.")
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
                        "{\"name\": \"string\", \"clientId\": 0, \"age\": 0 , \"dateOfBirth\":"
                            + " \"string\", \"address\": \"string\", \"email\": \"string\","
                            + " \"mobileNo\": \"string\", \"ucic\": 0}")
              }))
  @ProtectedPath
  @EncryptResponse
  @GetMapping("/{leadId}")
  public Mono<ResponseEntity<?>> getLeadProfile(@PathVariable String leadId) {
    return Mono.just(ResponseEntity.ok(customerService.getDetails(leadId)));
  }

  @Operation(
      summary = "Fetch SOA PDF",
      description = "This API fetches SOA pdf by Loan Account Number",
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
  @ProtectedPath
  @GetMapping("/{loanAccountNumber}/document/SOA")
  public Mono<ResponseEntity<ByteArrayResource>> fetchSOA(@PathVariable String loanAccountNumber) {
    return customerService
        .getSOA(loanAccountNumber)
        .map(
            pdfBytes -> {
              ByteArrayResource resource = new ByteArrayResource(pdfBytes);
              return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(resource);
            });
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
  @ProtectedPath
  @GetMapping("/{loanAccountNumber}/document/NOC")
  public Mono<ResponseEntity<?>> fetchNOC(@PathVariable String loanAccountNumber) {
    return customerService
        .getNOC(loanAccountNumber)
        .map(
            pdfBytes -> {
              ByteArrayResource resource = new ByteArrayResource(pdfBytes);
              return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(resource);
            });
  }

  @Operation(
      summary = "Fetch document list",
      description = "This operation fetches the list of all the documents for a loanAccountNumber",
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
  @ProtectedPath
  @GetMapping("/{loanApplicationId}/documents")
  public Mono<ResponseEntity<?>> fetchAllDocumentDetails(@PathVariable String loanApplicationId) {
    return Mono.just(ResponseEntity.ok(customerService.getAllDocumentDetails(loanApplicationId)));
  }

  @Operation(
      summary = "Fetch document by loanApplicationId & documentId",
      description = "This operation fetches document by loanApplicationId & documentId",
      parameters = {
        @Parameter(name = "loanApplicationId", required = true),
        @Parameter(name = "documentId", required = true)
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
  @ProtectedPath
  @GetMapping("/{loanApplicationId}/documents/{documentId}")
  public Mono<ResponseEntity<?>> fetchDocumentById(
      @PathVariable String loanApplicationId, @PathVariable String documentId) {
    return customerService
        .getDocument(loanApplicationId, documentId)
        .map(
            pdfBytes -> {
              ByteArrayResource resource = new ByteArrayResource(pdfBytes);
              return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(resource);
            });
  }

  @Operation(
      summary = "Fetch leadId by mobileNumber",
      description = "This operation fetches leadId by mobileNumber",
      parameters = {@Parameter(name = "mobileNumber", required = true)},
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
  @ProtectedPath
  @EncryptResponse
  @GetMapping("/info/{mobileNumber}")
  public Mono<ResponseEntity<?>> fetchLeadId(@PathVariable String mobileNumber) {
    return Mono.just(
        ResponseEntity.ok(customerService.fetchLeadIdAgainstMobileNumber(mobileNumber)));
  }

  @Operation(
      summary = "Fetches transaction details",
      description =
          "This operation fetches all the transaction details for a particular loan Account Number",
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
  @ProtectedPath
  @GetMapping("/{loanAccountNumber}/transaction-details")
  public Mono<ResponseEntity<?>> fetchTransactionDetails(@PathVariable String loanAccountNumber) {
    return Mono.just(ResponseEntity.ok(customerService.getTransactions(loanAccountNumber)));
  }

  @Operation(
      summary = "Report API for loan",
      description =
          "This operation fetches all the loan details associated with a particular leadId,"
              + " optionally filtered by loan status.",
      parameters = {
        @Parameter(name = "leadId", required = true),
        @Parameter(
            name = "status",
            required = false,
            description =
                "Filter loans by status. Values can be ACTIVE, CLOSED, OVERPAID, WRITTEN_OFF.")
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
  @ProtectedPath
  @EncryptResponse
  @GetMapping("/{leadId}/loans")
  public Mono<ResponseEntity<?>> fetchAllLoanDetails(
      @PathVariable String leadId,
      @RequestParam(value = "status", required = false) String status) {
    LoanStatus loanStatus = null;
    if (status != null) {
      try {
        loanStatus = LoanStatus.valueOf(status.toUpperCase());
      } catch (IllegalArgumentException e) {
        return Mono.just(ResponseEntity.badRequest().body("Invalid loan status provided."));
      }
    }

    return Mono.just(ResponseEntity.ok(customerService.getLoanDetails(leadId, loanStatus)));
  }

  @Operation(
      summary = "Logout User",
      description =
          "This operation logs out the user associated against a particular mobile Number.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LogoutRequestDTO.class)),
      description = "LogoutRequestDTO to be provided in the request body.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "User successfully logged out"),
        @ApiResponse(responseCode = "400", description = "Bad Request")
      })
  @ProtectedPath
  @PostMapping("/auth/logout")
  public Mono<ResponseEntity<?>> logoutUser(@RequestBody LogoutRequestDTO logoutRequestDTO) {
    return authOTPService
        .logoutUser(logoutRequestDTO)
        .flatMap(
            logoutResponse -> {
              ResponseCookie clearedJWTCookie = authOTPService.clearJWTCookie();
              return Mono.just(
                  ResponseEntity.ok()
                      .header(HttpHeaders.SET_COOKIE, clearedJWTCookie.toString())
                      .build());
            });
  }

  @Operation(
      summary = "Get  Categories from freshdesk",
      description = "Get  Categories on ticket creation flow from freshdesk",
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
  @GetMapping("/support/tickets/categories")
  public Mono<ResponseEntity<?>> getTicketCategoryFromFreshDesk() {
    return customerService.getCategories().map(ResponseEntity::ok);
  }

  @Operation(
      summary = "Submit form for Fresh Desk ticket creation",
      description =
          "This operation submits the ford and creates a Fresh Desk Ticket for the logged in user ")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LogoutRequestDTO.class)),
      description = "SubmitFormRequest to be provided in the request body.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Form Submitted"),
        @ApiResponse(responseCode = "400", description = "Bad Request")
      })
  @ProtectedPath
  @PostMapping("/support/tickets/form/{leadId}")
  public Mono<ResponseEntity<ResponseDTO<String>>> submitForm(
      @Valid @RequestBody SubmitFormRequest request, @PathVariable String leadId) {
    return customerService
        .submitForm(request, leadId)
        .map(
            response -> {
              if (response.getStatus() == ResponseStatus.FAIL) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
              }
              return ResponseEntity.ok(response);
            });
  }

  @Operation(
      summary = "Fetch RPS PDF",
      description = "This API fetches RPS pdf by Loan Account Number and client id",
      parameters = {
        @Parameter(name = "loanAccountNumber", required = true),
        @Parameter(name = "leadId", required = true)
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
  @ProtectedPath
  @GetMapping("/{loanAccountNumber}/document/rps/{leadId}")
  public Mono<ResponseEntity<ByteArrayResource>> fetchRPS(
      @PathVariable String loanAccountNumber, @PathVariable String leadId) {
    return customerService
        .getRPS(loanAccountNumber, leadId)
        .map(
            pdfBytes -> {
              ByteArrayResource resource = new ByteArrayResource(pdfBytes);
              return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(resource);
            });
  }

  @Operation(
      summary = "Get Consent from ClientId",
      description = "Get Consent from ClientId",
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
  @ProtectedPath
  @GetMapping("/getConsent/{leadId}")
  public Mono<ResponseEntity<?>> getConsentByClientId(
      @PathVariable
          @Size(max = 100, message = "[GetConsent] clientId exceeds max length of 100")
          @Pattern(regexp = "^[0-9]+$", message = "clientId must be numeric")
          String leadId) {

    return Mono.just(ResponseEntity.ok(customerService.getConsentByClientId(leadId)));
  }

  @Operation(
      summary = "Save consent by clientId",
      description = "This operation Save consent of a customer ")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ClientConsentDTO.class)),
      description = "ClientConsentDTO to be provided in the request body.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Form Submitted"),
        @ApiResponse(responseCode = "400", description = "Bad Request")
      })
  @ProtectedPath
  @PostMapping("/saveConsent/{leadId}")
  public Mono<ResponseEntity<Mono<?>>> saveConsent(
      @Valid @RequestBody ClientConsentDTO request,
      @PathVariable @Size(max = 100, message = "[SaveConsent] clientId exceeds max length of 100")
      @Pattern(regexp = "^[0-9]+$", message = "clientId must be numeric")
      String leadId) {
    return Mono.just(ResponseEntity.ok(customerService.saveConsent(leadId, request)));
  }
}
