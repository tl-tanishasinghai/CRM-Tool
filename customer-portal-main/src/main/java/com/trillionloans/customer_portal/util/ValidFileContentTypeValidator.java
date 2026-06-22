package com.trillionloans.customer_portal.util;

import com.trillionloans.customer_portal.model.dto.SubmitFormRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.apache.tika.Tika;

public class ValidFileContentTypeValidator
    implements ConstraintValidator<ValidFileContentType, SubmitFormRequest.Attachment> {

  private static final Tika tika = new Tika();
  private static final String[] allowedTypes = {"application/pdf", "image/jpeg", "image/png"};

  @Override
  public boolean isValid(
      SubmitFormRequest.Attachment attachment, ConstraintValidatorContext context) {
    if (attachment == null || attachment.getFileContent() == null) return true;
    boolean valid = true;
    try {
      byte[] fileBytes = Base64.getDecoder().decode(attachment.getFileContent());
      String detectedType = tika.detect(fileBytes);

      // Allowed types check
      if (!Arrays.asList(allowedTypes).contains(detectedType)) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate(
                "File content type is not allowed (" + detectedType + ")")
            .addPropertyNode("fileContent")
            .addConstraintViolation();
        valid = false;
      } else if (!matchesMagicHeader(fileBytes, detectedType)) {
        context.disableDefaultConstraintViolation();
        context
            .buildConstraintViolationWithTemplate(
                "File does not have valid magic header for " + detectedType)
            .addPropertyNode("fileContent")
            .addConstraintViolation();
        valid = false;
      } else {
        // Extension check
        String fileName = attachment.getFileName();
        if (fileName != null) {
          String ext = getExtension(fileName).toLowerCase();
          if (!mimeMatchesExtension(detectedType, ext)) {
            context.disableDefaultConstraintViolation();
            context
                .buildConstraintViolationWithTemplate(
                    "File extension '"
                        + ext
                        + "' does not match file content type '"
                        + detectedType
                        + "'")
                .addPropertyNode("fileName")
                .addConstraintViolation();
            valid = false;
          }
        }
      }
    } catch (Exception e) {
      context.disableDefaultConstraintViolation();
      context
          .buildConstraintViolationWithTemplate("Invalid base64 or error reading file data")
          .addPropertyNode("fileContent")
          .addConstraintViolation();
      valid = false;
    }
    return valid;
  }

  private boolean matchesMagicHeader(byte[] fileBytes, String detectedType) {
    if ("application/pdf".equals(detectedType)) {
      String header =
          new String(fileBytes, 0, Math.min(fileBytes.length, 4), StandardCharsets.US_ASCII);
      return header.startsWith("%PDF");
    } else if ("image/jpeg".equals(detectedType)) {
      return fileBytes.length > 2 && fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8;
    } else if ("image/png".equals(detectedType)) {
      return fileBytes.length > 4
          && fileBytes[0] == (byte) 0x89
          && fileBytes[1] == (byte) 0x50
          && fileBytes[2] == (byte) 0x4E
          && fileBytes[3] == (byte) 0x47;
    }
    return false;
  }

  private String getExtension(String fileName) {
    int i = fileName.lastIndexOf('.');
    return (i > 0) ? fileName.substring(i + 1) : "";
  }

  private boolean mimeMatchesExtension(String mimeType, String ext) {
    switch (mimeType) {
      case "application/pdf":
        return ext.equals("pdf");
      case "image/jpeg":
        return ext.equals("jpg") || ext.equals("jpeg");
      case "image/png":
        return ext.equals("png");
      default:
        return false;
    }
  }
}
