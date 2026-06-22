package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class M2PFetchCreditLineResponseDTO {
  private String accountNumber;
  private String status;
  private Long createdOn;
  private Long activatedOn;
  private BigDecimal allowedLimit;
}
