package com.trillionloans.los.model.dto.internal;

import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;

import com.trillionloans.los.constant.Event;
import java.util.Map;
import lombok.*;
import org.slf4j.MDC;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventContext {
  private Event event;
  private String loanId;
  private String clientId;
  private String partnerId;
  private boolean publishEvent;
  private Map<String, Object> metadata;

  public EventContext(Event event, String loanId, String clientId) {
    this.event = event;
    this.loanId = loanId;
    this.clientId = clientId;
    this.partnerId = MDC.get(PARTNER_ID);
    this.publishEvent = true;
  }

  public EventContext(Event event, String loanId) {
    this.event = event;
    this.loanId = loanId;
    this.partnerId = MDC.get(PARTNER_ID);
    this.publishEvent = true;
  }

  public EventContext(Event event) {
    this.event = event;
    this.partnerId = MDC.get(PARTNER_ID);
    this.publishEvent = true;
  }
}
