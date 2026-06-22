package com.trillionloans.los.model.entity;

import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Represents the Scienaptic entity in the database. This entity contains details about the request
 * and response between the system and an external Scienaptic service. It also keeps track of the
 * entity's creation time and whether it is active or not. The entity is mapped to the "scienaptic"
 * table in the database. Created on: 2024-09-24 Last updated on: 2024-09-24
 *
 * @author Ganesh Budhwant
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("scienaptic")
public class ScienapticEntity {
  @Id
  @Column("id")
  private Long id;

  @Column("external_id")
  private String externalId;

  @Column("bre_type")
  private String breType;

  @Column("request")
  private Json request;

  @Column("response")
  private Json response;

  @Column("is_active")
  private boolean isActive;

  @Column("created_at")
  private LocalDateTime createdAt;

  // This column stores status of the Scienaptic response
  @Column("scienaptic_status")
  private String scienapticStatus;

  @Column("partner_id")
  private String partnerId;

  public void setPartnerId(String partnerId) {
    if (partnerId == null || partnerId.trim().isEmpty()) {
      this.partnerId = null;
    } else {
      this.partnerId = partnerId;
    }
  }

  public static class ScienapticEntityBuilder {
    public ScienapticEntityBuilder partnerId(String partnerId) {
      if (partnerId == null || partnerId.trim().isEmpty()) {
        this.partnerId = null;
      } else {
        this.partnerId = partnerId;
      }
      return this;
    }
  }
}
