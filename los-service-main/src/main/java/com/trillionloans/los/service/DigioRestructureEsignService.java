package com.trillionloans.los.service;

import com.trillionloans.los.api.partner.DigioApi;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.model.request.digio.DigioSignDocumentRequest;
import com.trillionloans.los.model.request.digio.DigioUploadPdfRequest;
import com.trillionloans.los.model.response.digio.DigioSignDocumentResponse;
import com.trillionloans.los.service.DocS3UploadService.S3UploadResult;
import com.trillionloans.los.util.DocumentDownloadUtil;
import java.util.Base64;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigioRestructureEsignService {

  private static final String ESIGNING_LOG_HEADER = "[ESIGNING]";

  private final DigioApi digioApi;
  private final DocS3UploadService docS3UploadService;

  @Value("${digio.esigning.identifier}")
  private String esigningIdentifier;

  @Value("${digio.esigning.reason}")
  private String esigningReason;

  @Value("${digio.esigning.key-store-name}")
  private String keyStoreName;

  @Value("${digio.esigning.expire-in-days:10}")
  private int expireInDays;

  /**
   * Processes RESTRUCTURE_LOAN_AGREEMENT document: downloads from URL, uploads to Digio for
   * e-signing, triggers sign, downloads signed document, uploads to S3 and returns the presigned
   * URL.
   *
   * @param filePath URL to download document from
   * @param fileName name for the saved file
   * @param loanId loan identifier for logging
   * @param productCode product code for S3 path
   * @return Mono emitting S3UploadResult with presigned URL and S3 path of the signed document
   */
  public Mono<S3UploadResult> processRestructureLoanAgreement(
      String filePath, String fileName, String loanId, String productCode) {
    log.info(
        "{}[DIGIO_ESIGN][START] loanId={}, fileName={}", ESIGNING_LOG_HEADER, loanId, fileName);

    return DocumentDownloadUtil.downloadFromUrl(filePath)
        .doOnSuccess(
            bytes ->
                log.info(
                    "{}[DOWNLOAD_FROM_URL][SUCCESS] loanId={}, size={} bytes",
                    ESIGNING_LOG_HEADER,
                    loanId,
                    bytes.length))
        .doOnError(
            e ->
                log.error(
                    "{}[DOWNLOAD_FROM_URL][ERROR] loanId={}, error={}",
                    ESIGNING_LOG_HEADER,
                    loanId,
                    e.getMessage()))
        .map(bytes -> Base64.getEncoder().encodeToString(bytes))
        .flatMap(
            base64Pdf -> {
              DigioUploadPdfRequest uploadRequest =
                  DigioUploadPdfRequest.builder()
                      .signers(
                          Collections.singletonList(
                              DigioUploadPdfRequest.Signer.builder()
                                  .identifier(esigningIdentifier)
                                  .name(esigningIdentifier)
                                  .reason(esigningReason)
                                  .signType("external")
                                  .build()))
                      .expireInDays(expireInDays)
                      .displayOnPage("all")
                      .sendSignLink(true)
                      .notifySigners(true)
                      .fileName(fileName)
                      .fileData(base64Pdf)
                      .build();

              return digioApi
                  .uploadPdfDocument(uploadRequest)
                  .doOnSuccess(
                      r ->
                          log.info(
                              "{}[UPLOAD_PDF][SUCCESS] loanId={}, documentId={}",
                              ESIGNING_LOG_HEADER,
                              loanId,
                              r.getId()))
                  .doOnError(
                      e ->
                          log.error(
                              "{}[UPLOAD_PDF][ERROR] loanId={}, error={}",
                              ESIGNING_LOG_HEADER,
                              loanId,
                              e.getMessage()));
            })
        .flatMap(
            uploadResponse -> {
              String documentId = uploadResponse.getId();
              if (documentId == null || documentId.isBlank()) {
                log.error(
                    "{}[UPLOAD_PDF][ERROR] loanId={}, no document id in response",
                    ESIGNING_LOG_HEADER,
                    loanId);
                return Mono.error(
                    new BaseException(
                        "Digio upload did not return document id",
                        null,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }

              DigioSignDocumentRequest signRequest =
                  DigioSignDocumentRequest.builder()
                      .identifier(esigningIdentifier)
                      .documentId(documentId)
                      .reason(esigningReason)
                      .keyStoreName(keyStoreName)
                      .build();

              return digioApi
                  .signDocument(signRequest)
                  .doOnSuccess(
                      r ->
                          log.info(
                              "{}[SIGN_DOCUMENT][SUCCESS] loanId={}, documentId={}",
                              ESIGNING_LOG_HEADER,
                              loanId,
                              documentId))
                  .doOnError(
                      e ->
                          log.error(
                              "{}[SIGN_DOCUMENT][ERROR] loanId={}, documentId={}, error={}",
                              ESIGNING_LOG_HEADER,
                              loanId,
                              documentId,
                              e.getMessage()))
                  .filter(DigioSignDocumentResponse::getSuccess)
                  .switchIfEmpty(
                      Mono.error(
                          new BaseException(
                              "Digio sign document did not return success",
                              null,
                              HttpStatus.INTERNAL_SERVER_ERROR)))
                  .thenReturn(documentId);
            })
        .flatMap(
            documentId ->
                digioApi
                    .downloadDocument(documentId)
                    .doOnSuccess(
                        bytes ->
                            log.info(
                                "{}[DOWNLOAD_SIGNED][SUCCESS] loanId={}, size={} bytes",
                                ESIGNING_LOG_HEADER,
                                loanId,
                                bytes.length))
                    .doOnError(
                        e ->
                            log.error(
                                "{}[DOWNLOAD_SIGNED][ERROR] loanId={}, documentId={}, error={}",
                                ESIGNING_LOG_HEADER,
                                loanId,
                                documentId,
                                e.getMessage())))
        .flatMap(
            signedBytes ->
                Mono.fromCallable(
                        () ->
                            docS3UploadService.uploadPdfAndGetS3Result(
                                Base64.getEncoder().encodeToString(signedBytes),
                                fileName,
                                productCode))
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnSuccess(
                        result ->
                            log.info(
                                "{}[UPLOAD_TO_S3][SUCCESS] loanId={}, fileName={}, s3Path={}",
                                ESIGNING_LOG_HEADER,
                                loanId,
                                fileName,
                                result.s3Path()))
                    .doOnError(
                        e ->
                            log.error(
                                "{}[UPLOAD_TO_S3][ERROR] loanId={}, fileName={}, error={}",
                                ESIGNING_LOG_HEADER,
                                loanId,
                                fileName,
                                e.getMessage())));
  }
}
