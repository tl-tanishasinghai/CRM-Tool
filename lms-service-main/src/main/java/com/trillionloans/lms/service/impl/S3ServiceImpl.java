package com.trillionloans.lms.service.impl;

import static com.trillionloans.lms.constant.StringConstants.PARTNER_ID;
import static com.trillionloans.lms.constant.StringConstants.TRACE_ID;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.service.S3Service;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class S3ServiceImpl implements S3Service {

  private final AmazonS3 s3Client;

  /**
   * Constructs an instance of S3ServiceImpl with the specified S3 client, bucket name, and KMS key
   * ID.
   *
   * @param s3Client the Amazon S3 client to handle S3 operations
   */
  public S3ServiceImpl(AmazonS3 s3Client) {
    this.s3Client = s3Client;
  }

  /**
   * Uploads a file to S3 asynchronously.
   *
   * @param filePart the file part representing the content to upload
   * @param partnerId the partner identifier for tagging in metadata
   * @param traceId a unique identifier to track the file in S3
   * @param s3BucketName the name of the S3 bucket to upload to
   * @param kmsKey the AWS KMS key ID used for encryption
   * @param destination application to which file received
   * @return a Mono containing the URL of the uploaded file in S3
   */
  @Override
  public Mono<String> uploadFile(
      FilePart filePart,
      String partnerId,
      String traceId,
      String s3BucketName,
      String kmsKey,
      String destination) {
    return DataBufferUtils.join(filePart.content())
        .map(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              DataBufferUtils.release(dataBuffer);
              return bytes;
            })
        .flatMap(
            bytes ->
                uploadContent(
                    bytes,
                    filePart.filename(),
                    Objects.requireNonNull(filePart.headers().getContentType()).toString(),
                    partnerId,
                    traceId,
                    s3BucketName,
                    kmsKey,
                    destination));
  }

  /**
   * Uploads byte array content to S3 with specified metadata and encryption.
   *
   * @param content the content to upload as a byte array
   * @param fileName the name of the file to store in S3
   * @param contentType the MIME type of the content
   * @param partnerId the partner identifier for metadata tagging
   * @param traceId a unique identifier to tag and locate the content in S3
   * @param s3BucketName the S3 bucket name for uploading the content
   * @param kmsKey the AWS KMS key ID for encryption
   * @param destination application to which file received
   * @return a Mono containing the URL of the uploaded file in S3
   * @throws BaseException if the upload to S3 fails
   */
  @Override
  public Mono<String> uploadContent(
      byte[] content,
      String fileName,
      String contentType,
      String partnerId,
      String traceId,
      String s3BucketName,
      String kmsKey,
      String destination) {
    return Mono.fromCallable(
            () -> {
              try {
                String standardizedFileName =
                    generateUniqueFileName(traceId, fileName, destination);
                String s3Key = LocalDate.now() + "/" + standardizedFileName;

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(content.length);
                metadata.setContentType(contentType);
                addStandardMetadata(metadata, partnerId, traceId, fileName);

                PutObjectRequest putObjectRequest =
                    new PutObjectRequest(
                            s3BucketName, s3Key, new ByteArrayInputStream(content), metadata)
                        .withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(kmsKey));

                s3Client.putObject(putObjectRequest);
                return generateS3Url(s3BucketName, s3Key);
              } catch (Exception e) {
                log.error("Failed to upload file to S3: {}", fileName, e);
                throw new BaseException(
                    "Failed to upload file to S3", e, HttpStatus.INTERNAL_SERVER_ERROR);
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  /**
   * Uploads byte array content to S3 with specified metadata and encryption.
   *
   * @param content the content to upload as a byte array
   * @param fileName the name of the file to store in S3
   * @param contentType the MIME type of the content
   * @param s3BucketName the S3 bucket name for uploading the content
   * @param kmsKey the AWS KMS key ID for encryption
   * @param destination application to which file received
   * @return a Mono containing the URL of the uploaded file in S3
   * @throws BaseException if the upload to S3 fails
   */
  @Override
  public Mono<String> uploadContentAndFetchPreSignedUrl(
      byte[] content,
      String fileName,
      String contentType,
      String s3BucketName,
      String kmsKey,
      String destination) {
    try {
      String s3Key = destination + "/" + fileName;
      ObjectMetadata metadata = new ObjectMetadata();
      metadata.setContentLength(content.length);
      metadata.setContentType(contentType);
      addStandardMetadata(metadata, fileName);
      PutObjectRequest putObjectRequest =
          new PutObjectRequest(s3BucketName, s3Key, new ByteArrayInputStream(content), metadata)
              .withSSEAwsKeyManagementParams(new SSEAwsKeyManagementParams(kmsKey));
      s3Client.putObject(putObjectRequest);
      return getPreSignedUrl(s3Key, s3BucketName);
    } catch (Exception e) {
      log.error("[{}] failed to upload file to S3: {}", "S3", fileName, e);
      throw new BaseException("failed to upload file to s3", e, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Override
  public Mono<String> fetchPreSignedUrl(String filePath, String bucketName) {
    return getPreSignedUrl(filePath, bucketName);
  }

  /**
   * Adds standard metadata to the ObjectMetadata for S3 storage.
   *
   * @param metadata the ObjectMetadata to add metadata to
   * @param originalFileName the original file name for reference
   */
  private void addStandardMetadata(ObjectMetadata metadata, String originalFileName) {
    Map<String, String> userMetadata = new HashMap<>();
    userMetadata.put("original_filename", originalFileName);
    userMetadata.put("upload_timestamp", LocalDateTime.now().toString());
    metadata.setUserMetadata(userMetadata);
  }

  /**
   * Adds standard metadata to the ObjectMetadata for S3 storage.
   *
   * @param metadata the ObjectMetadata to add metadata to
   * @param partnerId the partner identifier to include in metadata
   * @param traceId a trace identifier for tracking
   * @param originalFileName the original file name for reference
   */
  private void addStandardMetadata(
      ObjectMetadata metadata, String partnerId, String traceId, String originalFileName) {
    Map<String, String> userMetadata = new HashMap<>();
    userMetadata.put(PARTNER_ID, partnerId);
    userMetadata.put(TRACE_ID, traceId);
    userMetadata.put("original_filename", originalFileName);
    userMetadata.put("upload_timestamp", LocalDateTime.now().toString());
    metadata.setUserMetadata(userMetadata);
  }

  private Mono<String> getPreSignedUrl(String filePath, String bucketName) {
    Date expiration = new Date(System.currentTimeMillis() + 12 * 3600 * 1000);
    GeneratePresignedUrlRequest generatePresignedUrlRequest =
        new GeneratePresignedUrlRequest(bucketName, filePath)
            .withMethod(com.amazonaws.HttpMethod.GET)
            .withExpiration(expiration);
    URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
    return Mono.just(url.toString());
  }

  /**
   * Generates the public URL for accessing the uploaded content in S3.
   *
   * @param bucketName the name of the S3 bucket
   * @param s3Key the S3 key identifying the uploaded content
   * @return the generated URL as a String
   */
  private String generateS3Url(String bucketName, String s3Key) {
    return String.format(
        "https://%s.s3.%s.amazonaws.com/%s", bucketName, s3Client.getRegion(), s3Key);
  }

  /**
   * Generates a unique file name using the trace ID and original file name.
   *
   * @param traceId the trace ID to ensure uniqueness
   * @param originalFileName the original file name for reference
   * @return the generated unique file name
   */
  private String generateUniqueFileName(
      String traceId, String originalFileName, String destination) {
    return destination + "-" + traceId + "-" + originalFileName;
  }
}
