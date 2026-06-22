package com.trillionloans.lms.model.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class NotificationRequest {
  private String templateId;
  private List<NotificationRecipientRequest> recipients;
  private List<FailOverNotification> failOverNotifications;

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  public static class FailOverNotification {
    private String templateId;
    private List<NotificationRecipientRequest> recipients;
  }
}
