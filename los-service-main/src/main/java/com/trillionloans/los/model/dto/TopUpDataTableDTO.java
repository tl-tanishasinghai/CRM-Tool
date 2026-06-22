package com.trillionloans.los.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "TopUp DataTable DTO")
public class TopUpDataTableDTO {
  @JsonProperty("topupid")
  @SerializedName("topupid")
  private String topUpId;

  @JsonProperty("outstandingamount")
  @SerializedName("outstandingamount")
  private String outstandingAmount;

  private String locale;
  private String dateFormat;

  @JsonProperty("sourcingchannel")
  @SerializedName("sourcingchannel")
  private String sourcingChannel;
}
