package com.trillionloans.los.controller.internal;

import static com.trillionloans.los.constant.StringConstants.PRODUCT_CODE;

import com.trillionloans.los.service.LspService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RequestMapping("/authenticate")
@AllArgsConstructor
@RestController
@Hidden
@Validated
@Slf4j
public class LspController {

  private final LspService lspService;

  @GetMapping("/partners/loanApplications/{loanApplicationId}")
  public Mono<?> getPartnerMappingByLoanApplicationId(
      @PathVariable String loanApplicationId,
      @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return lspService.getPartnerMappingByLoanApplicationId(loanApplicationId);
  }

  @GetMapping("/partners/loans/{lanId}")
  public Mono<?> getPartnerMappingByLoan(
      @PathVariable String lanId, @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return lspService.getPartnerMappingByLanId(lanId, productCode);
  }

  @GetMapping("/partners/creditLines/{lineId}")
  public Mono<?> getPartnerMappingByCreditLine(
      @PathVariable String lineId, @RequestHeader(name = PRODUCT_CODE) String productCode) {
    return lspService.getPartnerMappingByLineId(lineId);
  }
}
