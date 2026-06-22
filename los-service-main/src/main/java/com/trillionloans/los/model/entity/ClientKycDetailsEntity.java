package com.trillionloans.los.model.entity;

import com.trillionloans.los.constant.DocumentType;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("client_kyc_details")
public class ClientKycDetailsEntity {
  @Id
  @Column("id")
  private Long id;

  @Column("client_id")
  private String clientId;

  @Column("document_type")
  private DocumentType documentType;

  @Column("document_id")
  private String documentId;
}
