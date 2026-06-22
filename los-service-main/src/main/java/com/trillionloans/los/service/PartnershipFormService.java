package com.trillionloans.los.service;

import com.trillionloans.los.constant.PartnershipType;
import com.trillionloans.los.constant.ResponseStatus;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.PartnershipFormDTO;
import com.trillionloans.los.model.entity.PartnershipFormEntity;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.repository.PartnershipFormRepository;
import com.trillionloans.los.util.EncryptionUtil;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class PartnershipFormService {

  private static final Object PARTNERSHIP_FORM_SUBMITTED = "PARTNERSHIP_FORM_SUBMITTED";
  private static final Object SUBMIT_PARTNERSHIP_FORM = "SUBMIT_PARTNERSHIP_FORM";
  private final PartnershipFormRepository partnershipFormRepository;
  private final EncryptionUtil encryptionUtil;

  public Mono<ResponseDTO<Object>> submitPartnershipForm(PartnershipFormDTO dto) {

    if (!PartnershipType.isValid(dto.getPartnershipType())) {
      log.error(
          "[{}] invalid partnership type: {}", SUBMIT_PARTNERSHIP_FORM, dto.getPartnershipType());
      return Mono.error(
          new BaseException(
              "invalid partnership type", "partnership type is not valid", HttpStatus.BAD_REQUEST));
    }

    if (!Boolean.TRUE.equals(dto.getConsent())) {
      log.error(
          "[{}] consent is required, invalid consent value: {}",
          SUBMIT_PARTNERSHIP_FORM,
          dto.getConsent());
      return Mono.error(
          new BaseException(
              "consent is required", "client consent is needed", HttpStatus.BAD_REQUEST));
    }

    return partnershipFormRepository
        .save(mapPartnerShipFormDtoToEntity(dto))
        .map(
            saved -> {
              log.info(
                  "[{}] partnership form submitted successfully, ID: {}",
                  PARTNERSHIP_FORM_SUBMITTED,
                  saved.getId());
              return ResponseDTO.builder()
                  .status(ResponseStatus.SUCCESS)
                  .message("partnership form has been submitted successfully")
                  .data(saved.getId())
                  .build();
            })
        .onErrorResume(
            error -> {
              log.error("[{}] error while saving partnership form", SUBMIT_PARTNERSHIP_FORM);
              return Mono.error(
                  new BaseException(
                      "error saving partnership form",
                      "error while saving partnership form",
                      HttpStatus.BAD_REQUEST));
            });
  }

  private PartnershipFormEntity mapPartnerShipFormDtoToEntity(
      PartnershipFormDTO partnershipFormDTO) {
    return PartnershipFormEntity.builder()
        .partnershipType(partnershipFormDTO.getPartnershipType())
        .firstName(encryptionUtil.encrypt(partnershipFormDTO.getFirstName()))
        .lastName(encryptionUtil.encrypt(partnershipFormDTO.getLastName()))
        .email(encryptionUtil.encrypt(partnershipFormDTO.getEmail()))
        .phone(encryptionUtil.encrypt(partnershipFormDTO.getPhoneNumber()))
        .organizationName(partnershipFormDTO.getOrganizationName())
        .designationName(partnershipFormDTO.getDesignation())
        .consent(Boolean.TRUE.equals(partnershipFormDTO.getConsent()))
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .isDeleted(0)
        .build();
  }
}
