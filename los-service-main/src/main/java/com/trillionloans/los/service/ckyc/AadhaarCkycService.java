package com.trillionloans.los.service.ckyc;

import com.trillionloans.los.constant.RelationshipType;
import com.trillionloans.los.model.dto.AadhaarXmlDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pFamilyDetailsDTO;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AadhaarCkycService {

  public Optional<M2pFamilyDetailsDTO> fetchFamilyDetails(
      AadhaarXmlDetailsDTO aadhaarXmlDetailsDTO) {

    if (aadhaarXmlDetailsDTO.getCareOf() != null && !aadhaarXmlDetailsDTO.getCareOf().isBlank()) {
      log.info("[AADHAAR_CKYC_SEARCH] Successfully fetched dependent via aadhaar");
      List<String> fatherName =
          splitName(aadhaarXmlDetailsDTO.getCareOf().replaceAll("[\\p{Punct}&&[^ ]]", ""));
      return Optional.of(
          M2pFamilyDetailsDTO.builder()
              .firstName(fatherName.get(0))
              .lastName(fatherName.get(1))
              .relationship(getRelationship(aadhaarXmlDetailsDTO.getCareOf()).getDisplayName())
              .build());
    }
    log.warn("[AADHAAR_CKYC_SEARCH] failed to extract dependent via aadhaar");
    return Optional.empty();
  }

  public List<String> splitName(String fullName) {
    if (fullName == null || fullName.trim().isEmpty()) {
      return Collections.emptyList();
    }

    String trimmedName = fullName.trim();
    int lastSpaceIndex = trimmedName.lastIndexOf(' ');

    // Check if a space exists
    if (lastSpaceIndex != -1) {
      String firstName = trimmedName.substring(0, lastSpaceIndex);
      String lastName = trimmedName.substring(lastSpaceIndex + 1);
      return Arrays.asList(firstName, lastName);
    } else {
      // Case: Only one word found
      return Arrays.asList(trimmedName, " ");
    }
  }

  private RelationshipType getRelationship(String input) {
    if (input == null || input.isEmpty()) {
      return RelationshipType.FATHER;
    }

    String normalized = input.toUpperCase(Locale.UK);

    if (normalized.startsWith("S/O")
        || normalized.startsWith("C/O")
        || normalized.startsWith("D/O")) {
      return RelationshipType.FATHER;
    } else if (normalized.startsWith("W/O")) {
      return RelationshipType.HUSBAND;
    } else {
      log.info("[AADHAAR_CKYC_SEARCH] unable to extract relationship");
      return RelationshipType.FATHER;
    }
  }
}
