package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.model.request.VehicleDetailsRequest;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.service.VehicleLoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Controller class for handling vehicle loan application related endpoints. */
@RequestMapping("/partners/api/v1/vehicle-loan")
@AllArgsConstructor
@RestController
@Validated
@Tag(name = "Vehicle-Loan-Ops", description = "Operations related to vehicle loan applications")
public class VehicleLoanApplicationV1Controller {
  private final VehicleLoanApplicationService vehicleLoanApplicationService;

  /**
   * Endpoint to stamp vehicle details along with loan details.
   *
   * @param vehicleDetailsRequest The request containing vehicle details.
   * @param loanId The unique identifier for the loan.
   * @param productCode The product code associated with the loan product, obtained from request
   *     header.
   * @return A Mono wrapping ResponseEntity with the outcome of vehicle details stamping process.
   */
  @Operation(
      summary = "Stamps vehicle details along with loan details",
      description = "This operation posts the vehicle details, and loan details")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = VehicleDetailsRequest.class)),
      description = "Vehicle details to be provided in the request body.")
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
  @PostMapping(value = "/details/{loanId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> stampVehicleDetails(
      @SecureInput @Valid @RequestBody VehicleDetailsRequest vehicleDetailsRequest,
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(
        ResponseEntity.ok(
            vehicleLoanApplicationService.stampVehicleDetails(
                vehicleDetailsRequest, loanId, productCode)));
  }
}
