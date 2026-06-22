package com.trillionloans.los.model.response.m2p;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class M2pGetChargeDetailResponseDto {

  private Long id;

  @JsonProperty("loan_app_ref_id")
  private Long loanAppRefId;

  @JsonProperty("charge_id")
  private Long chargeId;

  @JsonProperty("charge_amount_or_percentage")
  private BigDecimal chargeAmountOrPercentage;

  @JsonProperty("is_mandatory")
  private Boolean isMandatory;

  @JsonProperty("is_amount_non_editable")
  private Boolean isAmountNonEditable;

  private Integer version;

  @JsonProperty("can_lend_charge")
  private Boolean canLendCharge;

  @JsonProperty("can_add_charge_to_principal_for_computation")
  private Boolean canAddChargeToPrincipalForComputation;

  @JsonProperty("lastmodified_date")
  private String lastModifiedDate;
}
