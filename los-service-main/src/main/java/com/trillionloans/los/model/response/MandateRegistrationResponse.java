package com.trillionloans.los.model.response;

import com.trillionloans.los.constant.MandateType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MandateRegistrationResponse {

  private String mandateId;

  private String customerUrl;

  private String state;

  private MandateType type;

  private String createdAt;

  private String expireAt;
}
