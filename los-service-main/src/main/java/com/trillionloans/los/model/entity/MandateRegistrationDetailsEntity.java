package com.trillionloans.los.model.entity;

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
@Table("mandate_registration_details")
public class MandateRegistrationDetailsEntity {
  // add indexing
  @Id private Long id;

  // add indexing
  @Column("client_id")
  private String clientId;

  // add indexing
  @Column("loan_id")
  private String loanId;

  @Column("partner_id")
  private String partnerId;

  // add indexing
  @Column("mandate_id")
  private String mandateId;

  @Column("auth_mode")
  private String authMode;

  @Column("amount")
  private String amount;

  @Column("frequency_type")
  private String frequencyType;

  @Column("vendor_name")
  private String vendorName;

  @Column("is_recurring")
  private Boolean isRecurring;

  // add indexing
  @Column("state")
  private String state;

  @Column("first_collection_date")
  private LocalDateTime firstCollectionDate;

  @Column("final_collection_date")
  private LocalDateTime finalCollectionDate;

  @Column("generate_access_token")
  private Boolean generateAccessToken;

  @Column("notify_customer")
  private Boolean notifyCustomer;

  // add indexing
  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;

  @Column("updated_at_los")
  private LocalDateTime updatedAtLos;

  // auto-incrementing version field
  // default value is 1
  @Column("version")
  private Integer version;

  @Builder.Default
  @Column("is_deleted")
  private Boolean isDeleted = false;
}
