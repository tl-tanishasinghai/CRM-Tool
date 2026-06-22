package com.trillionloans.crm.service;

import org.springframework.stereotype.Service;

@Service
public class PiiMaskingService {

  public String maskMobile(String mobile) {
    if (mobile == null || mobile.isBlank()) {
      return "—";
    }
    String digits = mobile.replaceAll("\\D", "");
    if (digits.length() < 4) {
      return "****";
    }
    if (digits.length() <= 6) {
      return digits.charAt(0) + "****" + digits.charAt(digits.length() - 1);
    }
    return digits.substring(0, 2) + "****" + digits.substring(digits.length() - 4);
  }

  public String maskEmail(String email) {
    if (email == null || email.isBlank() || !email.contains("@")) {
      return "—";
    }
    String[] parts = email.split("@", 2);
    String local = parts[0];
    String domain = parts[1];
    if (local.length() <= 1) {
      return "*@" + domain;
    }
    return local.charAt(0) + "***@" + domain;
  }

  public String maskAddress(String address) {
    if (address == null || address.isBlank()) {
      return "—";
    }
    return shortenAddress(address);
  }

  public String shortenAddress(String address) {
    if (address == null || address.isBlank()) {
      return "—";
    }
    String[] parts = address.split(",");
    if (parts.length >= 2) {
      return parts[parts.length - 2].trim() + ", " + parts[parts.length - 1].trim();
    }
    return address.length() > 48 ? address.substring(0, 45) + "…" : address;
  }

  public String panLast4(String panNumber) {
    if (panNumber == null || panNumber.length() < 4) {
      return "—";
    }
    return "****" + panNumber.substring(panNumber.length() - 4);
  }
}
