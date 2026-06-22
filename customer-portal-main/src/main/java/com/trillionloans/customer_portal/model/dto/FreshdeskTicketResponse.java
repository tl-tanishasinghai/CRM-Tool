package com.trillionloans.customer_portal.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FreshdeskTicketResponse {

  private Long id;

  private String subject;

  private Integer status;

  @JsonProperty("created_at")
  private String createdAt;

  private String description;

  @JsonProperty("custom_fields")
  private Map<String, Object> customFields;

  private List<Attachment> attachments;

  @Getter
  @Setter
  public static class Attachment {

    private Long id;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("file_size")
    private Long fileSize;

    private String name;

    @JsonProperty("attachment_url")
    private String attachmentUrl;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
  }
}
