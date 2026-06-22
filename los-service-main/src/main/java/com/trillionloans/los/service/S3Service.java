package com.trillionloans.los.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface S3Service {
  String uploadJsonToS3(String message, String traceId, String externalId);

  Mono<String> uploadFile(
      FilePart filePart,
      String partnerId,
      String traceId,
      String bucketName,
      String kmsKey,
      String destination);

  Mono<String> uploadContent(
      byte[] content,
      String fileName,
      String contentType,
      String partnerId,
      String traceId,
      String bucketName,
      String kmsKey,
      String destination);

  Mono<String> uploadBytesReturningS3Key(
      byte[] content,
      String fileName,
      String contentType,
      String partnerId,
      String traceId,
      String destination);

  String buildPublicHttpsUrlForObjectKey(String s3Key);

  byte[] downloadFile(String bucketName, String key);
}
