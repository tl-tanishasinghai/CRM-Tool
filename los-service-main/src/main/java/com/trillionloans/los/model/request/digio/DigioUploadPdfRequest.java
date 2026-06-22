package com.trillionloans.los.model.request.digio;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DigioUploadPdfRequest {

  private List<Signer> signers;

  @JsonProperty("expire_in_days")
  @SerializedName("expire_in_days")
  private Integer expireInDays;

  @JsonProperty("display_on_page")
  @SerializedName("display_on_page")
  private String displayOnPage;

  @JsonProperty("send_sign_link")
  @SerializedName("send_sign_link")
  private Boolean sendSignLink;

  @JsonProperty("notify_signers")
  @SerializedName("notify_signers")
  private Boolean notifySigners;

  @JsonProperty("file_name")
  @SerializedName("file_name")
  private String fileName;

  @JsonProperty("file_data")
  @SerializedName("file_data")
  private String fileData;

  @Data
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Signer {
    private String identifier;
    private String name;
    private String reason;

    @JsonProperty("sign_type")
    @SerializedName("sign_type")
    private String signType;
  }
}
