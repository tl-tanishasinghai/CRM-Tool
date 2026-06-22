package com.trillionloans.los.model.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** dto class for the merchant code for update in loan application */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Schema(description = "merchant/anchor code - request body")
@Setter
public class MerchantChangeRequest {
  private Associations associations;

  @Getter
  @Setter
  public static class Associations {
    private String anchor;
  }
}
