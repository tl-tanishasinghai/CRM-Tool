package com.trillionloans.los.util;

import com.trillionloans.los.exception.BaseException;
import java.net.URI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/** Utility for downloading document content from URLs (including S3 presigned URLs). */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class DocumentDownloadUtil {

  private static final WebClient webClient = WebClient.builder().build();

  /**
   * Downloads document bytes from the given URL (http/https). Supports S3 presigned URLs.
   *
   * @param filePath the URL to download from
   * @return Mono of document bytes
   */
  public static Mono<byte[]> downloadFromUrl(String filePath) {
    return webClient
        .get()
        .uri(URI.create(filePath))
        .exchangeToMono(
            resp -> {
              if (resp.statusCode().is3xxRedirection()) {
                URI location = resp.headers().asHttpHeaders().getLocation();
                if (location == null) {
                  return Mono.error(
                      new BaseException(
                          "Failed to download file from URL: no redirect location",
                          null,
                          HttpStatus.BAD_GATEWAY));
                }
                return downloadFromUrl(location.toString());
              }
              if (!resp.statusCode().is2xxSuccessful()) {
                return Mono.error(
                    new BaseException(
                        "Failed to download file from URL, status: " + resp.statusCode(),
                        null,
                        HttpStatus.BAD_GATEWAY));
              }
              return DataBufferUtils.join(resp.bodyToFlux(DataBuffer.class))
                  .map(
                      dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                        return bytes;
                      });
            });
  }
}
