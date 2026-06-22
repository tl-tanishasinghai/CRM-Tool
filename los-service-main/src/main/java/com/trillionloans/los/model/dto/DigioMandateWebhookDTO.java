package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Unified webhook DTO for all Digio events using HashMap for flexible payload handling. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DigioMandateWebhookDTO {

  @JsonProperty("id")
  @SerializedName("id")
  private String id;

  @JsonProperty("entities")
  @SerializedName("entities")
  private List<String> entities;

  @JsonProperty("payload")
  @SerializedName("payload")
  private HashMap<String, Object> payload;

  @JsonProperty("created_at")
  @SerializedName("created_at")
  private Long createdAt;

  @JsonProperty("event")
  @SerializedName("event")
  private String event;
}
