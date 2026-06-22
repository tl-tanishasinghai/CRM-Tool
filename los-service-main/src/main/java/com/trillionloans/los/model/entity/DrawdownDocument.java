package com.trillionloans.los.model.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.trillionloans.los.util.R2dbcJsonJacksonConfig;
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
 * Reference row for a document tied to an invoice or a drawdown (no file bytes stored). See {@link
 * com.trillionloans.los.service.drawdownorchestrator.DrawdownDocumentService} for how rows are
 * written.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("drawdown_documents")
public class DrawdownDocument {

  @Id private Long id;

  /**
   * What {@link #entityId} points to: e.g. {@code INVOICE} (invoice attachment docs) or {@code
   * DRAWDOWN} (drawdown attachment doc).
   */
  @Column("entity_type")
  private String entityType;

  /**
   * {@code invoices.id} when {@code entity_type} is invoice-scoped, or {@code drawdowns.id} when
   * drawdown-scoped.
   */
  @Column("entity_id")
  private Long entityId;

  /** Category of document for this row, e.g. {@code INVOICE_DOC} or {@code DRAWDOWN_AGREEMENT} */
  @Column("document_type")
  private String documentType;

  @Column("partner_id")
  private String partnerId;

  @Column("line_id")
  private String lineId;

  /** Document tag */
  @Column("tag")
  private String tag;

  /**
   * Retrievable file reference (HTTPS URL, presigned URL, or client-provided path/URL). Pairs with
   * {@link #s3Path}; may be null when only a key is known.
   */
  @Column("file_path")
  private String filePath;

  /**
   * S3 object key. For drawdown agreement docs: key from client or post-upload. For invoice docs:
   * key of the uploaded attachment (see {@code DrawdownDocumentService}).
   */
  @Column("s3_path")
  private String s3Path;

  /**
   * M2P document id returned after upload to M2P. Set for drawdown agreement docs; null for invoice
   * docs (no M2P upload on that path as of now).
   */
  @Column("m2p_document_id")
  private Integer m2pDocumentId;

  @JsonSerialize(using = R2dbcJsonJacksonConfig.Serializer.class)
  @JsonDeserialize(using = R2dbcJsonJacksonConfig.Deserializer.class)
  private Json metadata;

  @Column("created_at")
  private LocalDateTime createdAt;

  @Column("updated_at")
  private LocalDateTime updatedAt;
}
