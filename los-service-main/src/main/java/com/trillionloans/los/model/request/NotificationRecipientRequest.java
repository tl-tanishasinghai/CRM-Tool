package com.trillionloans.los.model.request;

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
@Setter
public class NotificationRecipientRequest {
  private String sms;
  private String whatsapp;
  private String email;
  private String url;
  private String referenceId;
  private String referenceType;
  private List<String> params;
}
