package com.trillionloans.los.service.drawdownorchestrator;

import static com.trillionloans.los.constant.StringConstants.APPLICATION_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.trillionloans.los.constant.DocumentTag;
import com.trillionloans.los.exception.drawdown.DrawdownDocumentPersistenceException;
import com.trillionloans.los.exception.drawdown.DrawdownNotFoundException;
import com.trillionloans.los.model.entity.Drawdown;
import com.trillionloans.los.model.entity.DrawdownDocument;
import com.trillionloans.los.model.entity.DrawdownInvoiceMapping;
import com.trillionloans.los.model.request.BulkDocumentsUploadRequest;
import com.trillionloans.los.model.request.InvoiceData;
import com.trillionloans.los.model.response.DrawdownDocumentsGroupDto;
import com.trillionloans.los.model.response.InvoiceDrawdownDocumentsGroupDto;
import com.trillionloans.los.model.response.LineDrawdownDocumentsDto;
import com.trillionloans.los.model.response.StoredDrawdownDocumentDto;
import com.trillionloans.los.model.response.m2p.M2pDocumentsUploadResponseDTO;
import com.trillionloans.los.repository.drawdown.DrawdownDocumentRepository;
import com.trillionloans.los.repository.drawdown.DrawdownInvoiceMappingRepository;
import com.trillionloans.los.repository.drawdown.DrawdownRepository;
import com.trillionloans.los.service.S3Service;
import com.trillionloans.los.util.FileValidatorUtil;
import io.r2dbc.postgresql.codec.Json;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawdownDocumentService {

  public static final String ENTITY_INVOICE = "INVOICE";
  public static final String ENTITY_DRAWDOWN = "DRAWDOWN";

  public static final String DOC_TYPE_INVOICE_DOC = "INVOICE_DOC";
  public static final String DOC_TYPE_DRAWDOWN_AGREEMENT = "DRAWDOWN_AGREEMENT";

  private static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  private final DrawdownDocumentRepository drawdownDocumentRepository;
  private final DrawdownRepository drawdownRepository;
  private final DrawdownInvoiceMappingRepository drawdownInvoiceMappingRepository;
  private final S3Service s3Service;

  /**
   * Returns document references for drawdowns on a credit line: per-invoice attachments (from
   * {@link DrawdownInvoiceMapping}) plus drawdown-level agreement docs.
   *
   * <p>When {@code drawdownId} is set, only that drawdown is returned (single element in {@code
   * drawdowns}). When omitted, all drawdowns for the partner on that line are returned, using
   * batched queries.
   */
  public Mono<LineDrawdownDocumentsDto> listDocumentsForLine(
      String lineId, Long drawdownId, String partnerId) {
    if (drawdownId != null) {
      return drawdownRepository
          .findById(drawdownId)
          .switchIfEmpty(Mono.error(new DrawdownNotFoundException("Drawdown not found")))
          .flatMap(
              drawdown -> {
                if (!lineId.equals(drawdown.getLineId())
                    || !partnerId.equals(drawdown.getPartnerId())) {
                  return Mono.error(new DrawdownNotFoundException("Drawdown not found"));
                }
                return buildScopedDocuments(drawdown)
                    .map(
                        scoped ->
                            LineDrawdownDocumentsDto.builder()
                                .lineId(lineId)
                                .drawdowns(List.of(scoped))
                                .build());
              });
    }
    return loadAllDrawdownsOnLine(lineId, partnerId);
  }

  private Mono<LineDrawdownDocumentsDto> loadAllDrawdownsOnLine(String lineId, String partnerId) {
    return drawdownRepository
        .findByLineId(lineId)
        .filter(d -> partnerId.equals(d.getPartnerId()))
        .collectList()
        .flatMap(
            drawdowns -> {
              if (drawdowns.isEmpty()) {
                return Mono.just(
                    LineDrawdownDocumentsDto.builder().lineId(lineId).drawdowns(List.of()).build());
              }
              drawdowns.sort(Comparator.comparing(Drawdown::getId).reversed());
              List<Long> drawdownIds =
                  drawdowns.stream().map(Drawdown::getId).collect(Collectors.toList());
              return drawdownInvoiceMappingRepository
                  .findAllByDrawdownIdIn(drawdownIds)
                  .collectList()
                  .flatMap(
                      mappings -> {
                        Map<Long, List<Long>> invoiceIdsByDrawdown =
                            buildInvoiceIdsByDrawdown(mappings);
                        Set<Long> allInvoiceIds = new HashSet<>();
                        for (List<Long> ids : invoiceIdsByDrawdown.values()) {
                          allInvoiceIds.addAll(ids);
                        }
                        return Mono.zip(
                                fetchInvoiceDocsByInvoiceId(allInvoiceIds, lineId, partnerId),
                                fetchDrawdownDocsByDrawdownId(drawdownIds, lineId))
                            .map(
                                tuple -> {
                                  Map<Long, List<StoredDrawdownDocumentDto>> invByInvoice =
                                      tuple.getT1();
                                  Map<Long, List<StoredDrawdownDocumentDto>> ddByDrawdown =
                                      tuple.getT2();
                                  List<DrawdownDocumentsGroupDto> scoped = new ArrayList<>();
                                  for (Drawdown d : drawdowns) {
                                    Long did = d.getId();
                                    List<Long> invIds =
                                        invoiceIdsByDrawdown.getOrDefault(did, List.of());
                                    List<InvoiceDrawdownDocumentsGroupDto> invoiceBlocks =
                                        invIds.stream()
                                            .map(
                                                id ->
                                                    InvoiceDrawdownDocumentsGroupDto.builder()
                                                        .invoiceId(id)
                                                        .documents(
                                                            invByInvoice.getOrDefault(
                                                                id, List.of()))
                                                        .build())
                                            .collect(Collectors.toList());
                                    scoped.add(
                                        DrawdownDocumentsGroupDto.builder()
                                            .drawdownId(did)
                                            .drawdownDocuments(
                                                ddByDrawdown.getOrDefault(did, List.of()))
                                            .invoices(invoiceBlocks)
                                            .build());
                                  }
                                  return LineDrawdownDocumentsDto.builder()
                                      .lineId(lineId)
                                      .drawdowns(scoped)
                                      .build();
                                });
                      });
            });
  }

  private static Map<Long, List<Long>> buildInvoiceIdsByDrawdown(
      List<DrawdownInvoiceMapping> mappings) {
    Map<Long, List<Long>> map = new LinkedHashMap<>();
    for (DrawdownInvoiceMapping m : mappings) {
      map.computeIfAbsent(m.getDrawdownId(), k -> new ArrayList<>()).add(m.getInvoiceId());
    }
    return map;
  }

  private Mono<Map<Long, List<StoredDrawdownDocumentDto>>> fetchInvoiceDocsByInvoiceId(
      Set<Long> invoiceIds, String lineId, String partnerId) {
    if (invoiceIds.isEmpty()) {
      return Mono.just(Map.of());
    }
    return drawdownDocumentRepository
        .findAllByEntityTypeAndEntityIdInAndLineIdOrderByCreatedAtDesc(
            ENTITY_INVOICE, new ArrayList<>(invoiceIds), lineId)
        .filter(doc -> partnerId.equals(doc.getPartnerId()))
        .collectList()
        .map(
            list ->
                list.stream()
                    .collect(
                        Collectors.groupingBy(
                            DrawdownDocument::getEntityId,
                            LinkedHashMap::new,
                            Collectors.mapping(this::toStoredDocumentDto, Collectors.toList()))));
  }

  private Mono<Map<Long, List<StoredDrawdownDocumentDto>>> fetchDrawdownDocsByDrawdownId(
      List<Long> drawdownIds, String lineId) {
    if (drawdownIds.isEmpty()) {
      return Mono.just(Map.of());
    }
    return drawdownDocumentRepository
        .findAllByEntityTypeAndEntityIdInAndLineIdOrderByCreatedAtDesc(
            ENTITY_DRAWDOWN, drawdownIds, lineId)
        .collectList()
        .map(
            list ->
                list.stream()
                    .collect(
                        Collectors.groupingBy(
                            DrawdownDocument::getEntityId,
                            LinkedHashMap::new,
                            Collectors.mapping(this::toStoredDocumentDto, Collectors.toList()))));
  }

  private Mono<DrawdownDocumentsGroupDto> buildScopedDocuments(Drawdown d) {
    Long drawdownId = d.getId();
    String lineId = d.getLineId();
    String partnerId = d.getPartnerId();

    Mono<List<Long>> invoiceIdsMono =
        drawdownInvoiceMappingRepository
            .findAllByDrawdownId(drawdownId)
            .map(DrawdownInvoiceMapping::getInvoiceId)
            .distinct()
            .collectList();

    Mono<List<StoredDrawdownDocumentDto>> drawdownDocsMono =
        drawdownDocumentRepository
            .findAllByEntityTypeAndEntityIdAndLineIdOrderByCreatedAtDesc(
                ENTITY_DRAWDOWN, drawdownId, lineId)
            .map(this::toStoredDocumentDto)
            .collectList();

    return Mono.zip(
            invoiceIdsMono.flatMap(ids -> buildInvoiceSections(ids, lineId, partnerId)),
            drawdownDocsMono)
        .map(
            tuple ->
                DrawdownDocumentsGroupDto.builder()
                    .drawdownId(drawdownId)
                    .invoices(tuple.getT1())
                    .drawdownDocuments(tuple.getT2())
                    .build());
  }

  private Mono<List<InvoiceDrawdownDocumentsGroupDto>> buildInvoiceSections(
      List<Long> invoiceIds, String lineId, String partnerId) {
    if (invoiceIds.isEmpty()) {
      return Mono.just(List.of());
    }
    return drawdownDocumentRepository
        .findAllByEntityTypeAndEntityIdInAndLineIdOrderByCreatedAtDesc(
            ENTITY_INVOICE, invoiceIds, lineId)
        .filter(doc -> partnerId.equals(doc.getPartnerId()))
        .collectList()
        .map(
            allDocs -> {
              Map<Long, List<DrawdownDocument>> byInvoice =
                  allDocs.stream()
                      .collect(
                          Collectors.groupingBy(
                              DrawdownDocument::getEntityId,
                              LinkedHashMap::new,
                              Collectors.toList()));
              return invoiceIds.stream()
                  .map(
                      id -> {
                        List<DrawdownDocument> rows = byInvoice.getOrDefault(id, List.of());
                        List<StoredDrawdownDocumentDto> dtos =
                            rows.stream()
                                .map(this::toStoredDocumentDto)
                                .collect(Collectors.toList());
                        return InvoiceDrawdownDocumentsGroupDto.builder()
                            .invoiceId(id)
                            .documents(dtos)
                            .build();
                      })
                  .collect(Collectors.toList());
            });
  }

  private StoredDrawdownDocumentDto toStoredDocumentDto(DrawdownDocument e) {
    Object meta = null;
    if (e.getMetadata() != null) {
      try {
        meta = OBJECT_MAPPER.readValue(e.getMetadata().asString(), Object.class);
      } catch (Exception ex) {
        log.warn(
            "[DRAWDOWN_DOCUMENTS][FETCH] Failed to deserialize metadata for id={}", e.getId(), ex);
      }
    }
    return StoredDrawdownDocumentDto.builder()
        .id(e.getId())
        .documentType(e.getDocumentType())
        .tag(e.getTag())
        .filePath(e.getFilePath())
        .s3Path(e.getS3Path())
        .m2pDocumentId(e.getM2pDocumentId())
        .metadata(meta)
        .createdAt(e.getCreatedAt())
        .updatedAt(e.getUpdatedAt())
        .build();
  }

  public Mono<Void> saveInvoiceDocumentReferences(
      Long invoiceId, InvoiceData invoiceData, String partnerId, String lineId) {
    if (invoiceId == null || invoiceData == null) {
      return Mono.empty();
    }

    BulkDocumentsUploadRequest docs = invoiceData.getDocument();
    if (docs == null || docs.getDocuments() == null || docs.getDocuments().isEmpty()) {
      return Mono.empty();
    }

    return Flux.fromIterable(docs.getDocuments())
        .concatMap(
            docDetails -> {
              if (docDetails == null || docDetails.getDocument() == null) {
                return Mono.empty();
              }
              return uploadInvoiceDocumentAndSetPathsForValidation(
                  docDetails.getDocument(), partnerId, invoiceId);
            })
        .then(
            FileValidatorUtil.validateDocumentsForUpload(docs, "invoice-" + invoiceId)
                .doOnNext(
                    results ->
                        results.stream()
                            .filter(e -> !e.getValue())
                            .forEach(
                                e ->
                                    log.warn(
                                        "[DRAWDOWN_DOCUMENTS][INVOICE] File validation failed for"
                                            + " {} (invoiceId={})",
                                        e.getKey().getDocument().getFileName(),
                                        invoiceId))))
        .then(
            Mono.defer(
                () ->
                    Flux.fromIterable(docs.getDocuments())
                        .concatMap(
                            docDetails -> {
                              if (docDetails == null || docDetails.getDocument() == null) {
                                return Mono.empty();
                              }

                              BulkDocumentsUploadRequest.DocumentInfoDTO doc =
                                  docDetails.getDocument();
                              DocumentTag documentTag = docDetails.getTag();
                              String tag =
                                  documentTag != null ? documentTag.name() : StringUtils.EMPTY;

                              Map<String, Object> metadata = new HashMap<>();
                              metadata.put("storageType", doc.getStorageType());
                              metadata.put("filePath", doc.getFilePath());
                              metadata.put("fileType", doc.getFileType());
                              metadata.put("fileName", doc.getFileName());

                              String s3Key =
                                  StringUtils.isNotBlank(doc.getS3Path())
                                      ? doc.getS3Path().trim()
                                      : null;
                              String filePathForRow =
                                  StringUtils.isNotBlank(doc.getFilePath())
                                      ? doc.getFilePath().trim()
                                      : null;

                              DrawdownDocument entity =
                                  DrawdownDocument.builder()
                                      .entityType(ENTITY_INVOICE)
                                      .entityId(invoiceId)
                                      .documentType(DOC_TYPE_INVOICE_DOC)
                                      .partnerId(partnerId)
                                      .lineId(lineId)
                                      .tag(tag)
                                      .filePath(filePathForRow)
                                      .s3Path(s3Key)
                                      .m2pDocumentId(null)
                                      .metadata(toJson(metadata))
                                      .build();

                              return save(entity).then();
                            })
                        .then()));
  }

  /**
   * Uploads base64 content when needed, sets {@code s3Path} (object key) and {@code filePath}
   * (HTTPS URL in our bucket) on the DTO for {@link FileValidatorUtil#validateDocumentsForUpload}.
   */
  private Mono<Void> uploadInvoiceDocumentAndSetPathsForValidation(
      BulkDocumentsUploadRequest.DocumentInfoDTO doc, String partnerId, Long invoiceId) {
    if (StringUtils.isNotBlank(doc.getS3Path())) {
      String key = doc.getS3Path().trim();
      if (StringUtils.isBlank(doc.getFilePath())) {
        doc.setFilePath(s3Service.buildPublicHttpsUrlForObjectKey(key));
      }
      return Mono.empty();
    }
    if (StringUtils.isBlank(doc.getEncodedFile())) {
      log.warn(
          "[DRAWDOWN_DOCUMENTS][INVOICE] No s3Path or encodedFile; invoiceId={}, fileName={}",
          invoiceId,
          doc.getFileName());
      return Mono.empty();
    }
    return Mono.fromCallable(() -> Base64.getDecoder().decode(doc.getEncodedFile().trim()))
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(
            IllegalArgumentException.class,
            e -> {
              log.error(
                  "[DRAWDOWN_DOCUMENTS][INVOICE] Invalid base64 for invoiceId={}, fileName={}",
                  invoiceId,
                  doc.getFileName(),
                  e);
              return Mono.empty();
            })
        .flatMap(
            bytes ->
                s3Service
                    .uploadBytesReturningS3Key(
                        bytes,
                        invoiceFileNameOrDefault(doc.getFileName()),
                        contentTypeForInvoiceDoc(doc),
                        partnerId,
                        invoiceId + "-" + UUID.randomUUID(),
                        APPLICATION_NAME)
                    .doOnNext(
                        key -> {
                          doc.setS3Path(key);
                          doc.setFilePath(s3Service.buildPublicHttpsUrlForObjectKey(key));
                        })
                    .then()
                    .onErrorResume(
                        e -> {
                          log.error(
                              "[DRAWDOWN_DOCUMENTS][INVOICE] S3 upload failed for invoiceId={},"
                                  + " fileName={}",
                              invoiceId,
                              doc.getFileName(),
                              e);
                          return Mono.empty();
                        }));
  }

  private static String invoiceFileNameOrDefault(String fileName) {
    if (StringUtils.isBlank(fileName)) {
      return "document";
    }
    return fileName;
  }

  private static String contentTypeForInvoiceDoc(BulkDocumentsUploadRequest.DocumentInfoDTO doc) {
    if (StringUtils.isNotBlank(doc.getFileType())) {
      return doc.getFileType().trim();
    }
    return "application/octet-stream";
  }

  public Mono<Void> saveDrawdownAgreementDocumentReferences(
      Long drawdownId,
      BulkDocumentsUploadRequest agreement,
      String lineId,
      String productCode,
      String partnerId) {
    return saveDrawdownAgreementDocumentReferences(
        drawdownId, agreement, lineId, productCode, partnerId, null);
  }

  /**
   * Persists agreement document refs after M2P upload. When {@code m2pUploadResponse} is present,
   * stores {@link M2pDocumentsUploadResponseDTO.Doc#getDocumentId()} matched by document tag.
   */
  public Mono<Void> saveDrawdownAgreementDocumentReferences(
      Long drawdownId,
      BulkDocumentsUploadRequest agreement,
      String lineId,
      String productCode,
      String partnerId,
      M2pDocumentsUploadResponseDTO m2pUploadResponse) {
    if (drawdownId == null || agreement == null || agreement.getDocuments() == null) {
      return Mono.empty();
    }

    return Flux.fromIterable(agreement.getDocuments())
        .concatMap(
            docDetails -> {
              if (docDetails == null || docDetails.getDocument() == null) {
                return Mono.empty();
              }

              BulkDocumentsUploadRequest.DocumentInfoDTO doc = docDetails.getDocument();
              DocumentTag tag = docDetails.getTag();

              Integer m2pDocumentId = resolveM2pDocumentId(docDetails, m2pUploadResponse);
              String s3Path =
                  StringUtils.isNotBlank(doc.getS3Path()) ? doc.getS3Path().trim() : null;
              String filePathForRow =
                  StringUtils.isNotBlank(doc.getFilePath()) ? doc.getFilePath().trim() : null;

              Map<String, Object> metadata = new HashMap<>();
              metadata.put("productCode", productCode);
              metadata.put("storageType", doc.getStorageType());
              metadata.put("filePath", doc.getFilePath());
              metadata.put("fileType", doc.getFileType());
              metadata.put("fileName", doc.getFileName());

              DrawdownDocument entity =
                  DrawdownDocument.builder()
                      .entityType(ENTITY_DRAWDOWN)
                      .entityId(drawdownId)
                      .documentType(DOC_TYPE_DRAWDOWN_AGREEMENT)
                      .partnerId(partnerId)
                      .lineId(lineId)
                      .tag(tag != null ? tag.name() : null)
                      .filePath(filePathForRow)
                      .s3Path(s3Path)
                      .m2pDocumentId(m2pDocumentId)
                      .metadata(toJson(metadata))
                      .build();

              return save(entity).then();
            })
        .then();
  }

  private static Integer resolveM2pDocumentId(
      BulkDocumentsUploadRequest.DocumentDetailsDTO requestDoc,
      M2pDocumentsUploadResponseDTO response) {
    if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
      return null;
    }
    DocumentTag tag = requestDoc != null ? requestDoc.getTag() : null;
    String tagName = tag != null ? tag.name() : null;
    if (tagName == null) {
      return null;
    }
    for (M2pDocumentsUploadResponseDTO.Doc doc : response.getDocuments()) {
      if (doc == null || doc.getDocumentDetails() == null) {
        continue;
      }
      String responseTag = doc.getDocumentDetails().getTag();
      if (responseTag != null && tagName.equalsIgnoreCase(responseTag.trim())) {
        return doc.getDocumentId();
      }
    }
    return null;
  }

  /**
   * Returns all drawdown-scoped documents for the given drawdown, most recent first. Used by
   * callers that need direct access to the raw entities (e.g. async notification triggers).
   */
  public Flux<DrawdownDocument> findAllDrawdownDocuments(Long drawdownId, String lineId) {
    return drawdownDocumentRepository.findAllByEntityTypeAndEntityIdAndLineIdOrderByCreatedAtDesc(
        ENTITY_DRAWDOWN, drawdownId, lineId);
  }

  private Mono<DrawdownDocument> save(DrawdownDocument entity) {
    return drawdownDocumentRepository
        .save(entity)
        .onErrorResume(
            error -> {
              log.error(
                  "[DRAWDOWN_DOCUMENTS][SAVE][ERROR] entityType={}, entityId={}, documentType={},"
                      + " tag={}: {}",
                  entity.getEntityType(),
                  entity.getEntityId(),
                  entity.getDocumentType(),
                  entity.getTag(),
                  error.getMessage(),
                  error);
              return Mono.error(
                  new DrawdownDocumentPersistenceException(
                      "Error while saving document reference."));
            });
  }

  private Json toJson(Map<String, Object> map) {
    try {
      return Json.of(OBJECT_MAPPER.writeValueAsString(map));
    } catch (Exception e) {
      throw new DrawdownDocumentPersistenceException("Failed to serialize document metadata.");
    }
  }
}
