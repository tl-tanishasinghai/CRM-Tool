package com.trillionloans.lms.controller;

import com.trillionloans.lms.model.dto.restructure.ApproveRestructureResponseDTO;
import com.trillionloans.lms.service.RestructureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * Controller class for handling loan restructure operations.
 *
 * @author Pawan Kumar
 */
@Slf4j
@RestController
@RequestMapping("/partners/api/v1/restructure")
@AllArgsConstructor
@Tag(name = "Restructure", description = "All the Operations related to Loan Restructure")
@Validated
public class RestructureController {

  private final RestructureService restructureService;

  @Operation(
      summary = "Get restructure data based on type",
      description =
          "Fetches restructure data based on lead ID and type. "
              + "Type can be 'eligibility', 'offer', 'rps', or 'status'. "
              + "requestId is required for type 'rps' and 'status'.",
      parameters = {
        @Parameter(
            name = "lan",
            required = true,
            description = "Loan account number (LAN) for which data needs to be fetched"),
        @Parameter(
            name = "type",
            required = true,
            description = "Type of data to fetch: eligibility, offer, rps, or status",
            schema = @Schema(allowableValues = {"eligibility", "offer", "rps", "status"})),
        @Parameter(
            name = "requestId",
            required = false,
            description =
                "Required for rps and status. Request ID from loan_application_restructure_details")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Data retrieved successfully",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid type parameter",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "404",
            description = "Lead not found",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json"))
      })
  @GetMapping(value = "lan/{lan}", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<?>> getRestructureData(
      @PathVariable("lan") String lan,
      @RequestParam("type") String type,
      @RequestParam(value = "requestId", required = false) String requestId) {
      return (Mono<ResponseEntity<?>>) restructureService.getRestructureDetails(lan, type, requestId);
  }

  @Operation(
      summary = "Approve restructured loan request",
      description = "Approves/executes a restructured loan request with M2P.",
      parameters = {
        @Parameter(name = "lan", required = true, description = "Loan Account Number (LAN)"),
        @Parameter(
            name = "requestId",
            required = true,
            description = "Reschedule Request ID (returned from eligibility API)")
      },
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Restructure approved successfully",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApproveRestructureResponseDTO.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid LAN or requestId",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "404",
            description = "Restructure request not found",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(mediaType = "application/json"))
      })
  @PostMapping(
      value = "lan/{lan}/requestId/{requestId}",
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<ApproveRestructureResponseDTO>> approveRestructure(
      @PathVariable("lan") String lan, @PathVariable("requestId") String requestId) {
    log.info("Approving restructure for lan: {}, requestId: {}", lan, requestId);
    return restructureService.approveRestructure(lan, requestId).map(ResponseEntity::ok);
  }
}
