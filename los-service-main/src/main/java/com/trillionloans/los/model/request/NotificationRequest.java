package com.trillionloans.los.model.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class NotificationRequest {
  private String templateId;
  private List<NotificationRecipientRequest> recipients;
}
