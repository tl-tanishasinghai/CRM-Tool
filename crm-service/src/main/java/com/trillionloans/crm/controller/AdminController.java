package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.UpsertUserRequest;
import com.trillionloans.crm.service.AuthService;
import com.trillionloans.crm.service.CrmStore;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/admin/users")
public class AdminController {

  private final AuthService authService;
  private final CrmStore store;

  public AdminController(AuthService authService, CrmStore store) {
    this.authService = authService;
    this.store = store;
  }

  @GetMapping
  public List<StaffUser> listUsers(@RequestHeader("X-CRM-Token") String token) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    return store.listUsers();
  }

  @PostMapping
  public StaffUser createUser(
      @RequestHeader("X-CRM-Token") String token, @Valid @RequestBody UpsertUserRequest request) {
    authService.requireAnyRole(token, Role.ADMIN);
    return store.upsertUser(null, request);
  }

  @PatchMapping("/{userId}")
  public StaffUser updateUser(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String userId,
      @Valid @RequestBody UpsertUserRequest request) {
    authService.requireAnyRole(token, Role.ADMIN);
    return store.upsertUser(userId, request);
  }
}
