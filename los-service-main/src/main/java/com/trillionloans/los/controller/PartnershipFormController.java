package com.trillionloans.los.controller;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.model.dto.LoanFormDTO;
import com.trillionloans.los.model.dto.PartnershipFormDTO;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.service.LoanFormService;
import com.trillionloans.los.service.PartnershipFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Form-Submission",
    description = "Operations related to submitting loan and partnership forms")
public class PartnershipFormController {

  private final LoanFormService loanFormService;
  private final PartnershipFormService partnershipFormService;

  @Operation(
      summary = "Submit loan form",
      description =
          "This operation submits the loan form details and returns a response with the loan ID.")
  @PostMapping("/loan")
  public Mono<ResponseEntity<ResponseDTO>> submitLoanForm(
      @Valid @RequestBody LoanFormDTO requestDTO,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return loanFormService.submitLoanForm(requestDTO).map(ResponseEntity::ok);
  }

  @Operation(
      summary = "Submit loan form",
      description =
          "This operation submits the partnership form details and returns a response with the"
              + " partnership ID.")
  @PostMapping("/partnership")
  public Mono<ResponseEntity<ResponseDTO>> submitPartnershipForm(
      @Valid @RequestBody PartnershipFormDTO requestDTO,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return partnershipFormService.submitPartnershipForm(requestDTO).map(ResponseEntity::ok);
  }
}
