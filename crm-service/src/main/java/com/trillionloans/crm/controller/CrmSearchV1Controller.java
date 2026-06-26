package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.FieldSearchResult;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.service.AuthService;
import com.trillionloans.crm.service.CustomerSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/crm")
public class CrmSearchV1Controller {

  private final AuthService authService;
  private final CustomerSearchService customerSearchService;

  public CrmSearchV1Controller(
      AuthService authService, CustomerSearchService customerSearchService) {
    this.authService = authService;
    this.customerSearchService = customerSearchService;
  }

  @GetMapping("/search")
  public FieldSearchResult search(
      @RequestHeader("X-CRM-Token") String token,
      @RequestParam String field,
      @RequestParam String value) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return customerSearchService.search(field, value);
  }
}
