package com.trillionloans.los.model.entity;

import java.time.OffsetDateTime;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("anchor_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnchorMaster {

  @Id private Long id;

  @Column("anchor_id")
  private String anchorId;

  @Column("anchor_name")
  private String anchorName;

  @Column("pan")
  private String pan;

  @Column("gst")
  private String gst;

  @Column("created_at")
  private OffsetDateTime createdAt;
}
