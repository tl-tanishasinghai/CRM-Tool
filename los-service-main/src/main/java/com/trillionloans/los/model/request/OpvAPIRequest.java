package com.trillionloans.los.model.request;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Represents the payload for NSDL PAN Verification API. */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OpvAPIRequest {

  /**
   * inputData (List of PanRequest) - Mandatory - Can contain up to 5 records of PAN verification
   * requests
   */
  private List<NsdlPanVerificationRequest> inputData;

  /**
   * signature (String) - Mandatory - PKCS#7 detached signature of the JSON representation of
   * `inputData` - Signed using the entity’s signing certificate (.pfx file)
   */
  @SerializedName("Signature")
  private String signature;
}
