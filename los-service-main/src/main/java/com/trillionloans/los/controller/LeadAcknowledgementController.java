package com.trillionloans.los.controller;

import com.trillionloans.los.config.SecureInput;
import com.trillionloans.los.model.request.LeadAcknowledgementRequest;
import com.trillionloans.los.model.response.LeadAcknowledgementResponse;
import com.trillionloans.los.service.LeadAcknowledgementService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/los/api/v1/lead")
@AllArgsConstructor
@RestController
@Validated
public class LeadAcknowledgementController {

  private final LeadAcknowledgementService leadAcknowledgementService;

  @PostMapping(value = "/{loanId}/acknowledge")
  public Mono<LeadAcknowledgementResponse> acknowledgementLead(
      @SecureInput @Valid @RequestBody LeadAcknowledgementRequest leadAcknowledgementRequest,
      @PathVariable @NonNull String loanId) {
    return leadAcknowledgementService.acknowledgementLead(loanId, leadAcknowledgementRequest);
  }
}
