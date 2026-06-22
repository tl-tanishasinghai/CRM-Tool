package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.model.dto.BreDTO;
import com.trillionloans.los.model.response.m2p.AddressFetchResponse;
import com.trillionloans.los.service.ClientFacadeService;
import com.trillionloans.los.service.CpvService;
import com.trillionloans.los.service.PublisherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/partners/api/v1/client")
@RequiredArgsConstructor
@RestController
@Slf4j
@Tag(name = "Client-Ops", description = "Operations related to clients")
public class ClientFacadeController {
  private final PublisherService publisherService;
  private final ClientFacadeService clientFacadeService;
  private final CpvService cpvService;
  private Environment environment;

  @Operation(
      summary = "Post BRE Data",
      description = "This operation uploads BRE request and fetches response from BRE service")
  @PostMapping(
      value = "/bre-data/{loanId}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Mono<?>>> postBreData(
      @RequestBody Object requestBody,
      @PathVariable(name = "loanId") String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    BreDTO breDTO = BreDTO.builder().loanId(loanId).requestBody(requestBody).build();
    return Mono.just(
        ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(clientFacadeService.registerBreByProduct(breDTO, productCode)));
  }

  @Operation(
      summary = "Fetch a CPV status",
      description = "This operation fetch CPV status associated with lead")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = Object.class),
              examples = {@ExampleObject(name = "example", value = "")}))
  @GetMapping("/cpv/status/{loanId}")
  public Mono<ResponseEntity<Mono<?>>> getCkycStatus(
      @Parameter(
              name = "loanId",
              description = "ID of the loan",
              required = true,
              example = "00201")
          @PathVariable
          String loanId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return Mono.just(ResponseEntity.ok(cpvService.getCpvStatus(loanId)));
  }

  @Operation(
      summary = "Fetch client addresses",
      description = "Returns normalized addresses for a client")
  @ApiResponse(
      responseCode = "200",
      description = "Success",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AddressFetchResponse.class)))
  @GetMapping(value = "/{clientId}/addresses", produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Flux<AddressFetchResponse>>> getClientAddresses(
      @Parameter(name = "clientId", description = "Client ID", required = true, example = "9172")
          @PathVariable
          String clientId) {
    return Mono.just(ResponseEntity.ok(clientFacadeService.fetchClientAddresses(clientId)));
  }
}
