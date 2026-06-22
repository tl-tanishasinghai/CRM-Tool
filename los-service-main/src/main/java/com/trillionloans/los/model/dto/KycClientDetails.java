package com.trillionloans.los.model.dto;

import com.trillionloans.los.model.response.ReuseAddressDetailsDTO;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class KycClientDetails {
  private String name;
  private String dob;
  private String fatherName;
  private List<ReuseAddressDetailsDTO> addressInfo;
  private String dependent;
}
