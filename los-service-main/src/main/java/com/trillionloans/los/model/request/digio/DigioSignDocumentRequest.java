package com.trillionloans.los.model.request.digio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DigioSignDocumentRequest {

  private String identifier;

  @JsonProperty("document_id")
  @SerializedName("document_id")
  private String documentId;

  private String reason;

  @JsonProperty("key_store_name")
  @SerializedName("key_store_name")
  private String keyStoreName;
}
