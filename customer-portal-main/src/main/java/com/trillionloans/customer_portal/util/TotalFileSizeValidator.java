package com.trillionloans.customer_portal.util;

import com.trillionloans.customer_portal.model.dto.SubmitFormRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Base64;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TotalFileSizeValidator
    implements ConstraintValidator<ValidTotalFileSize, List<SubmitFormRequest.Attachment>> {

  private long maxTotalSize;

  @Override
  public void initialize(ValidTotalFileSize constraintAnnotation) {
    this.maxTotalSize = constraintAnnotation.maxSizeInMB() * 1024 * 1024;
  }

  @Override
  public boolean isValid(
      List<SubmitFormRequest.Attachment> attachments, ConstraintValidatorContext context) {
    if (attachments == null || attachments.isEmpty()) {
      return true;
    }

    long totalSize = 0;
    for (SubmitFormRequest.Attachment attachment : attachments) {
      String fileContent = attachment.getFileContent();
      if (fileContent == null || fileContent.isEmpty()) {
        continue;
      }
      try {
        totalSize += Base64.getDecoder().decode(fileContent).length;
      } catch (IllegalArgumentException e) {
        return false;
      }
    }

    return totalSize <= maxTotalSize;
  }
}
