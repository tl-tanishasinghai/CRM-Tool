package com.trillionloans.los.service;

import com.trillionloans.los.constant.ResponseStatus;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.dto.LoanFormDTO;
import com.trillionloans.los.model.entity.LoanFormEntity;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.repository.LoanFormRepository;
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
public class LoanFormService {

  private final LoanFormRepository loanFormRepository;
  private final EncryptionUtil encryptionUtil;
  private static final Object SUBMIT_LOAN_FORM = "SUBMIT_LOAN_FORM";
  private static final Object LOAN_FORM_SUBMITTED = "LOAN_FORM_SUBMITTED";

  public Mono<ResponseDTO<Object>> submitLoanForm(LoanFormDTO dto) {

    if (!Boolean.TRUE.equals(dto.getConsent())) {
      log.error(
          "[{}] Consent is required, invalid consent value: {}",
          SUBMIT_LOAN_FORM,
          dto.getConsent());
      return Mono.error(
          new BaseException(
              "consent is required", "client consent is needed", HttpStatus.BAD_REQUEST));
    }

    return loanFormRepository
        .save(mapLoanFormDtoToEntity(dto))
        .map(
            saved -> {
              log.info(
                  "[{}] loan form submitted successfully, ID: {}",
                  LOAN_FORM_SUBMITTED,
                  saved.getId());
              return ResponseDTO.builder()
                  .status(ResponseStatus.SUCCESS)
                  .message("loan form has been submitted successfully")
                  .data(saved.getId())
                  .build();
            })
        .onErrorResume(
            error -> {
              log.error("[{}] error while saving loan form", SUBMIT_LOAN_FORM);
              return Mono.error(
                  new BaseException(
                      "error saving loan form",
                      "error while saving loan form",
                      HttpStatus.BAD_REQUEST));
            });
  }

  private LoanFormEntity mapLoanFormDtoToEntity(LoanFormDTO dto) {
    return LoanFormEntity.builder()
        .firstName(encryptionUtil.encrypt(dto.getFirstName()))
        .lastName(encryptionUtil.encrypt(dto.getLastName()))
        .email(encryptionUtil.encrypt(dto.getEmail()))
        .phone(encryptionUtil.encrypt(dto.getPhoneNumber()))
        .loanType(dto.getLoanType())
        .consent(Boolean.TRUE.equals(dto.getConsent()))
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .isDeleted(0)
        .build();
  }
}
