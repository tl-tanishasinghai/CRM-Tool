package com.trillionloans.los.util;

import com.trillionloans.los.model.dto.LivelinessScoreDataTableDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LivelinessScoreUtil {

  private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
  private static final Validator validator = factory.getValidator();

  public static boolean validateLivelinessScoreParameters(
      LivelinessScoreDataTableDTO livelinessScoreDataTableDTO) {

    // check whether to skip
    if (livelinessScoreDataTableDTO.getScore() == null) {
      log.info(
          "[LIVELINESS_SCORE_CREATE] Skipping liveliness score upload for LeadId {} because the"
              + " score is null",
          livelinessScoreDataTableDTO.getLead());
      return false;
    }

    // Validate the LivelinessScoreDataTableDTO object
    Set<ConstraintViolation<LivelinessScoreDataTableDTO>> violations =
        validator.validate(livelinessScoreDataTableDTO);

    // If there are violations, handle them (throw an exception, log errors, etc.)
    if (!violations.isEmpty()) {

      // Collect the validation messages from the violations
      for (ConstraintViolation<LivelinessScoreDataTableDTO> violation : violations) {
        log.error("[LIVELINESS_SCORE_CREATE] [ERROR] Validation error: {}", violation.getMessage());
      }
      return false;
    }
    return true;
  }
}
