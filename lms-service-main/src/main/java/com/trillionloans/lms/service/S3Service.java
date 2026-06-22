package com.trillionloans.lms.service;

import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface S3Service {
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

  Mono<String> uploadContentAndFetchPreSignedUrl(
      byte[] content,
      String fileName,
      String contentType,
      String bucketName,
      String kmsKey,
      String destination);

  Mono<String> fetchPreSignedUrl(String filePath, String bucketName);
}
