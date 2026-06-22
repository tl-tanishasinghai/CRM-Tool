package com.trillionloans.los.model.partner.m2p;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class M2pLeadUpdateDTO {
  private String locale;
  private String dateFormat;
  private M2pClientDetailsUpdateDTO clientData;
  private List<M2pAddressDetailsDTO> addressData;
  private List<M2pFamilyDetailsDTO> familyDetailsData;
  private List<M2pBankDetailsDTO> bankDetailsData;
  private M2pEmploymentDetailsDTO employmentDetailData;
  private List<M2pAdditionalDetailsDTO> additionalDetails;
}
