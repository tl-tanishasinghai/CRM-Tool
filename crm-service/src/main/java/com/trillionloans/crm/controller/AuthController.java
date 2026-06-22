package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.LoginRequest;
import com.trillionloans.crm.model.CrmModels.LoginResponse;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.email(), request.password());
  }

  @PostMapping("/logout")
  public void logout(@RequestHeader(name = "X-CRM-Token", required = false) String token) {
    authService.logout(token);
  }

  @GetMapping("/me")
  public StaffUser me(@RequestHeader("X-CRM-Token") String token) {
    return authService.requireUser(token);
  }
}
