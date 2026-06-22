package com.trillionloans.los.model.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "client_miscellaneous_details")
public class ClientMiscellaneousDetails {

  @Id
  @Column("id")
  private Long id;

  @Column("client_id")
  private Integer clientId;

  @Column("partner_id")
  private Integer partnerId;

  @Column("details")
  private String details;

  @Column("created_at")
  private LocalDateTime createdAt;
}
