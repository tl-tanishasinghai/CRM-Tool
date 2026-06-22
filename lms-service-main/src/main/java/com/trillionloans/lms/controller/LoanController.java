package com.trillionloans.lms.controller;

import static com.trillionloans.lms.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.lms.config.SecureInput;
import com.trillionloans.lms.model.request.DocumentUploadRequest;
import com.trillionloans.lms.service.LoanService;
import com.trillionloans.lms.util.FileValidatorUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Set;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Controller class for handling collection-related operations.
 *
 * @author Himat Bhanushali
 */
@RestController
@RequestMapping("/partners/api/v1/loan")
@AllArgsConstructor
@Tag(name = "Loan", description = "All the Operations related to Loan")
@Validated
public class LoanController {
  private final LoanService loanService;

  @Operation(
      summary = "Upload documents against loan",
      description = "API to upload documents against loan",
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
            description = "Not Found",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "500",
            description = "Error/Fail",
            content = @Content(mediaType = "application/json"))
      })
  @PostMapping("/{loanId}/documents")
  public ResponseEntity<Mono<?>> uploadDocumentAgainstLoan(
      @RequestPart("name") String name,
      @RequestPart("tagIdentifier") String tagIdentifier,
      @SecureInput @Valid @RequestPart("file") FilePart file,
      @PathVariable("loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {

    Mono<?> resultMono =
        FileValidatorUtil.hasValidExtensionAndMimeType(file)
            .flatMap(
                isValid -> {
                  if (Boolean.TRUE.equals(isValid)) {
                    DocumentUploadRequest documentUploadRequest =
                        DocumentUploadRequest.builder()
                            .file(file)
                            .name(name)
                            .tagIdentifier(tagIdentifier)
                            .build();

                    return loanService.uploadDocumentAgainstLoan(documentUploadRequest, loanId);
                  } else {
                    return Mono.error(new IllegalArgumentException("Invalid file type or content"));
                  }
                });

    return ResponseEntity.ok(resultMono);
  }

  @Operation(
      summary = "Retrieve loan details",
      description = "Fetches detailed loan information based on the provided loan ID",
      parameters = {
        @Parameter(
            name = "loanId",
            required = true,
            description = "Loan ID for which details need to be retrieved"),
        @Parameter(
            name = "staffInSelectedOfficeOnly",
            description = "Flag to filter staff in selected office"),
        @Parameter(
            name = "associations",
            description =
                "Comma-separated list of loan associations to include (e.g., 'all',"
                    + " 'repaymentSchedule', 'futureSchedule', 'originalSchedule', 'transactions',"
                    + " 'charges', 'guarantors', 'collateral', 'notes', 'linkedAccount',"
                    + " 'multiDisburseDetails', etc.)"),
        @Parameter(
            name = "exclude",
            description = "Comma-separated list of loan associations to exclude"),
        @Parameter(
            name = "fields",
            description = "Comma-separated list of fields to include in the response")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Loan details retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "404",
            description = "Loan not found",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping(value = "/{loanId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Object>> retrieveLoan(
      @PathVariable Long loanId,
      @RequestParam(defaultValue = "false") boolean staffInSelectedOfficeOnly,
      @RequestParam(required = false) Set<String> associations,
      @RequestParam(required = false) Set<String> exclude,
      @RequestParam(required = false) Set<String> fields) {

    return loanService
        .retrieveLoan(loanId, staffInSelectedOfficeOnly, associations, exclude, fields)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
  }
}
