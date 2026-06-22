package com.trillionloans.lms.model.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("client_consent")
public class ClientConsentEntity {

  @Id
  @Column("id")
  private Long id;

  @Column("client_id")
  private String clientId;

  @Column("consent_status")
  private Boolean consentStatus;

  @Column("ip_address")
  private String ipAddress;

  @Column("consent_key")
  private String consentKey;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
