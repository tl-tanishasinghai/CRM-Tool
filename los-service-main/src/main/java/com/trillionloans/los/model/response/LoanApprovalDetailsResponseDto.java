package com.trillionloans.los.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoanApprovalDetailsResponseDto {

  @JsonProperty("clientId")
  @SerializedName("clientId")
  private Integer clientId;

  @JsonProperty("statusEnum")
  @SerializedName("statusEnum")
  private Integer statusEnum;

  @JsonProperty("officeId")
  @SerializedName("officeId")
  private Long officeId;
}
