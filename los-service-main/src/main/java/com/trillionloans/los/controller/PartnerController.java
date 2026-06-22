package com.trillionloans.los.controller;

import com.trillionloans.los.model.request.PartnerPostPutRequest;
import com.trillionloans.los.service.PartnerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/partner/api/v1")
@AllArgsConstructor
@RestController
@Validated
@Tag(name = "Partner-Controller", description = "Operations related to Partners")
public class PartnerController {

  private PartnerService partnerService;

  @PostMapping(value = "/bre-callback/loans/{loanId}", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<Object> registerBreCallback(
      @RequestBody PartnerPostPutRequest partnerPostPutRequest,
      @PathVariable @NonNull String loanId) {
    return partnerService.registerBreCallback(partnerPostPutRequest);
  }
}
