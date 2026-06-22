package com.trillionloans.crm.service;

import com.trillionloans.crm.model.CrmModels.LoginResponse;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.UserStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final CrmStore store;

  public AuthService(CrmStore store) {
    this.store = store;
  }

  public LoginResponse login(String email, String password) {
    StaffUser user =
        store.findUserByEmail(email)
            .filter(existing -> existing.status() == UserStatus.ACTIVE)
            .orElseThrow(() -> new AccessDeniedException("Invalid CRM credentials"));

    // MVP auth keeps seeded staff credentials simple. Swap with SSO/password store before prod.
    if (!"password".equals(password)) {
      throw new AccessDeniedException("Invalid CRM credentials");
    }

    String token = UUID.randomUUID().toString();
    store.saveSession(token, user.id());
    return new LoginResponse(token, user);
  }

  public void logout(String token) {
    store.deleteSession(token);
  }

  public StaffUser requireUser(String token) {
    String userId =
        store.findUserIdByToken(token)
            .orElseThrow(() -> new AccessDeniedException("Missing or expired CRM token"));
    return store.getUser(userId);
  }

  public StaffUser requireAnyRole(String token, Role... roles) {
    StaffUser user = requireUser(token);
    for (Role role : roles) {
      if (user.role() == role) {
        return user;
      }
    }
    throw new AccessDeniedException("User is not allowed to perform this action");
  }

  public StaffUser seedSystemUser() {
    return new StaffUser(
        "system", "System", "system@trillionloans.com", Role.ADMIN, UserStatus.ACTIVE, null, Instant.now());
  }
}
