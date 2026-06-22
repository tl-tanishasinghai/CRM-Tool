package com.trillionloans.customer_portal.model.dto;

import com.trillionloans.customer_portal.util.NoHtml;
import com.trillionloans.customer_portal.util.ValidFileContentType;
import com.trillionloans.customer_portal.util.ValidTotalFileSize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubmitFormRequest {

  @Pattern(
      regexp = "\\+91\\d{10}",
      message = "Mobile number must start with +91 followed by 10 digits")
  @NotBlank(message = "Mobile number is required")
  private String registeredMobileNumber;

  @Email(message = "Invalid email format")
  @NotBlank(message = "Email  is required")
  private String email;

  @NotBlank(message = "Concern category is required")
  @NoHtml
  private String concernCategory;

  @Size(min = 30, message = "Description must be at least 30 characters")
  @NotBlank(message = "Description is required")
  @NoHtml
  @Pattern(
      regexp = "^[^'<>]*$",
      message = "Description must not contain single quote (') or angle brackets (<, >)")
  private String description;

  @NoHtml private String loanId;

  @Pattern(regexp = "^[A-Z]{5}\\d{4}[A-Z]$", message = "Invalid PAN format (e.g. ABCDE1234R)")
  @NotBlank(message = "Pan Card is required")
  private String panCard;

  @Size(max = 5, message = "Maximum 5 attachments allowed")
  @ValidTotalFileSize
  private List<@Valid Attachment> attachments;

  public void setPanCard(String panCard) {
    this.panCard = panCard != null ? panCard.toUpperCase() : null;
  }

  @Setter
  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  @ValidFileContentType
  public static class Attachment {

    @NotBlank(message = "File name is required")
    @Pattern(
        regexp = "^(?!\\.)[a-zA-Z0-9 _\\-]+\\.(?i:pdf|jpg|jpeg|png)$",
        message = "Invalid file attachment format: must end with PDF/JPG/PNG/JPEG")
    private String fileName;

    @NotBlank(message = "File content is required")
    private String fileContent; // Base64 string
  }
}
