package com.trillionloans.los.model.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Public contract for a stored drawdown/invoice document reference (GET line documents API). Does
 * not expose persistence layout ({@code entity_type}, {@code entity_id}, etc.).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredDrawdownDocumentDto {

  private Long id;
  private String documentType;
  private String tag;
  private String filePath;
  private String s3Path;
  private Integer m2pDocumentId;
  private Object metadata;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
