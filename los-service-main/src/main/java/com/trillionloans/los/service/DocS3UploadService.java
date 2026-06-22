package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.INVALID_PDF;
import static com.trillionloans.los.constant.StringConstants.SHORT_FILE_CONTENT;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.trillionloans.los.exception.BaseException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DocS3UploadService {

  public record S3UploadResult(String presignedUrl, String s3Path) {}

  private final AmazonS3 s3Client;

  @Value("${doc.upload.bucket-name}")
  private String bucketName;

  public DocS3UploadService(AmazonS3Client s3Client) {
    this.s3Client = s3Client;
  }

  public S3UploadResult uploadPdfAndGetS3Result(
      String base64Pdf, String fileName, String productCode) {
    try {
      log.info("[DOC_S3_UPLOAD] starting pdf upload for fileName: {}", fileName);
      byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);

      if (pdfBytes.length < 20) {
        throw new BaseException(SHORT_FILE_CONTENT, SHORT_FILE_CONTENT, HttpStatus.BAD_REQUEST);
      }

      String header = new String(Arrays.copyOfRange(pdfBytes, 0, 5), StandardCharsets.US_ASCII);
      if (!header.equals("%PDF-")) {
        throw new BaseException(INVALID_PDF, INVALID_PDF, HttpStatus.BAD_REQUEST);
      }

      int scanLength = Math.min(pdfBytes.length, 1024);
      int startIndex = pdfBytes.length - scanLength;

      String footerSection =
          new String(
              Arrays.copyOfRange(pdfBytes, startIndex, pdfBytes.length), StandardCharsets.US_ASCII);

      if (!footerSection.contains("%%EOF")) {
        log.error("[DOC_S3_UPLOAD] pdf missing eof marker: {}", fileName);
        throw new BaseException(
            INVALID_PDF, "content is truncated or corrupted", HttpStatus.BAD_REQUEST);
      }

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
      String timestamp = LocalDateTime.now().format(formatter);
      String uniqueFileName = fileName + "_" + timestamp;
      String s3Path = productCode + "/uploads/pdfs/" + uniqueFileName;

      log.info("[DOC_S3_UPLOAD] uploading file to s3 path : {}", s3Path);

      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(pdfBytes);

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(pdfBytes.length);
      metadata.setContentType("application/pdf");

      s3Client.putObject(bucketName, s3Path, byteArrayInputStream, metadata);

      Date expiration = new Date();
      expiration.setTime(expiration.getTime() + 2 * 60 * 60 * 1000);

      GeneratePresignedUrlRequest generatePresignedUrlRequest =
          new GeneratePresignedUrlRequest(bucketName, s3Path)
              .withMethod(HttpMethod.GET)
              .withExpiration(expiration);

      String preSignedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
      log.info("[DOC_S3_UPLOAD] pre-signed url generated successfully");

      return new S3UploadResult(preSignedUrl, s3Path);

    } catch (IllegalArgumentException e) {
      log.error(
          "[DOC_S3_UPLOAD] invalid base64 content for file name: {}. Error: {}",
          fileName,
          e.getMessage());
      throw new BaseException(INVALID_PDF, INVALID_PDF, HttpStatus.BAD_REQUEST);

    } catch (BaseException e) {
      log.error("[DOC_S3_UPLOAD] invalid pdf file : {}. Error: {}", fileName, e.getMessage());
      throw e;

    } catch (Exception e) {
      log.error(
          "[DOC_S3_UPLOAD] failed to upload pdf to S3 for file name: {}. Error: {}",
          fileName,
          e.getMessage(),
          e);
      throw new BaseException(
          "failed to upload document",
          "failed to upload document",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Uploads raw PDF bytes to {@code doc.upload.bucket-name} under {@code
   * {productCode}/{folderUnderProduct}/} with a presigned GET URL (2h expiry). For base64 uploads
   * use {@link #uploadPdfAndGetS3Result}.
   */
  public S3UploadResult uploadPdfBytesAndGetS3Result(
      byte[] pdfBytes, String productCode, String folderUnderProduct, String fileBaseName) {
    try {
      log.info("[DOC_S3_UPLOAD] starting pdf bytes upload fileBaseName={}", fileBaseName);
      validatePdfBytes(pdfBytes, fileBaseName);

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
      String timestamp = LocalDateTime.now().format(formatter);
      String uniqueFileName = fileBaseName + "_" + timestamp;
      String s3Path = productCode + "/" + folderUnderProduct + "/" + uniqueFileName;

      log.info("[DOC_S3_UPLOAD] uploading file to s3 path : {}", s3Path);

      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(pdfBytes);

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(pdfBytes.length);
      metadata.setContentType("application/pdf");

      s3Client.putObject(bucketName, s3Path, byteArrayInputStream, metadata);

      Date expiration = new Date();
      expiration.setTime(expiration.getTime() + 2 * 60 * 60 * 1000);

      GeneratePresignedUrlRequest generatePresignedUrlRequest =
          new GeneratePresignedUrlRequest(bucketName, s3Path)
              .withMethod(HttpMethod.GET)
              .withExpiration(expiration);

      String preSignedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
      log.info("[DOC_S3_UPLOAD] pre-signed url generated successfully");

      return new S3UploadResult(preSignedUrl, s3Path);
    } catch (BaseException e) {
      log.error("[DOC_S3_UPLOAD] invalid pdf file : {}. Error: {}", fileBaseName, e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error(
          "[DOC_S3_UPLOAD] failed to upload pdf to S3 for file name: {}. Error: {}",
          fileBaseName,
          e.getMessage(),
          e);
      throw new BaseException(
          "failed to upload document",
          "failed to upload document",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Same as {@link #uploadPdfBytesAndGetS3Result} but uploads to {@code targetBucketName} with
   * SSE-KMS ({@code kmsKeyId}) and returns a presigned GET URL for that object.
   */
  public S3UploadResult uploadPdfBytesWithKmsAndGetS3Result(
      byte[] pdfBytes,
      String productCode,
      String folderUnderProduct,
      String fileBaseName,
      String targetBucketName,
      String kmsKeyId) {
    try {
      log.info(
          "[DOC_S3_UPLOAD] KMS pdf upload fileBaseName={} bucket={}",
          fileBaseName,
          targetBucketName);
      validatePdfBytes(pdfBytes, fileBaseName);

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
      String timestamp = LocalDateTime.now().format(formatter);
      String uniqueFileName = fileBaseName + "_" + timestamp;
      String s3Path = productCode + "/" + folderUnderProduct + "/" + uniqueFileName;

      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(pdfBytes.length);
      metadata.setContentType("application/pdf");

      PutObjectRequest putObjectRequest =
          new PutObjectRequest(
                  targetBucketName, s3Path, new ByteArrayInputStream(pdfBytes), metadata)
              .withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(kmsKeyId));

      s3Client.putObject(putObjectRequest);

      Date expiration = new Date();
      expiration.setTime(expiration.getTime() + 2 * 60 * 60 * 1000);

      GeneratePresignedUrlRequest generatePresignedUrlRequest =
          new GeneratePresignedUrlRequest(targetBucketName, s3Path)
              .withMethod(HttpMethod.GET)
              .withExpiration(expiration);

      String preSignedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
      log.info("[DOC_S3_UPLOAD] KMS pre-signed url generated successfully");

      return new S3UploadResult(preSignedUrl, s3Path);
    } catch (BaseException e) {
      log.error("[DOC_S3_UPLOAD] invalid pdf file : {}. Error: {}", fileBaseName, e.getMessage());
      throw e;
    } catch (Exception e) {
      log.error(
          "[DOC_S3_UPLOAD] failed KMS pdf upload for file name: {}. Error: {}",
          fileBaseName,
          e.getMessage(),
          e);
      throw new BaseException(
          "failed to upload document",
          "failed to upload document",
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private void validatePdfBytes(byte[] pdfBytes, String fileName) {
    if (pdfBytes.length < 20) {
      throw new BaseException(SHORT_FILE_CONTENT, SHORT_FILE_CONTENT, HttpStatus.BAD_REQUEST);
    }

    String header = new String(Arrays.copyOfRange(pdfBytes, 0, 5), StandardCharsets.US_ASCII);
    if (!header.equals("%PDF-")) {
      throw new BaseException(INVALID_PDF, INVALID_PDF, HttpStatus.BAD_REQUEST);
    }

    int scanLength = Math.min(pdfBytes.length, 1024);
    int startIndex = pdfBytes.length - scanLength;

    String footerSection =
        new String(
            Arrays.copyOfRange(pdfBytes, startIndex, pdfBytes.length), StandardCharsets.US_ASCII);

    if (!footerSection.contains("%%EOF")) {
      log.error("[DOC_S3_UPLOAD] pdf missing eof marker: {}", fileName);
      throw new BaseException(
          INVALID_PDF, "content is truncated or corrupted", HttpStatus.BAD_REQUEST);
    }
  }

  public String uploadPdfAndGetPresignedUrl(String base64Pdf, String fileName, String productCode) {
    return uploadPdfAndGetS3Result(base64Pdf, fileName, productCode).presignedUrl();
  }
}
