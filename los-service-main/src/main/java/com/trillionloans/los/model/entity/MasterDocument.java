package com.trillionloans.los.model.entity;

import java.time.LocalDateTime;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("master_documents")
public class MasterDocument {
  @Id
  @Column("id")
  private Long id;

  @Column("document_tag")
  private String documentTag;

  @Column("is_psl")
  private Boolean isPsl;

  @Column("is_active")
  private Boolean isActive;

  @Column("created_at")
  private LocalDateTime createdAt;
}
