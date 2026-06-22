package com.trillionloans.los.model.dto.internal;

import java.time.Instant;
import java.util.Map;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventBusDTO {

  private String clientId;
  private String loanAppId;
  private String partnerId;
  private String eventName;
  private String evenType;
  private Instant eventDate;
  private Map<String, String> metadata;
}
