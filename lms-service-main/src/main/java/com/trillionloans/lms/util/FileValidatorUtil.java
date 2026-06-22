package com.trillionloans.lms.util;

import static org.apache.tika.metadata.HttpHeaders.CONTENT_TYPE;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

/**
 * Utility class for file validation operations. This class is designed to validate file extensions.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class FileValidatorUtil {

  private static final Map<String, MediaType> extensionToMediaType = new HashMap<>();
  private static final Set<MediaType> supportedMediaTypes = new HashSet<>();

  static {
    // Initialize the extension-to-media-type mapping
    extensionToMediaType.put("pdf", MediaType.APPLICATION_PDF);
    extensionToMediaType.put("jpeg", MediaType.IMAGE_JPEG);
    extensionToMediaType.put("jpg", MediaType.IMAGE_JPEG);
    extensionToMediaType.put("png", MediaType.IMAGE_PNG);
    extensionToMediaType.put(
        "xlsx",
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    extensionToMediaType.put("xls", MediaType.parseMediaType("application/vnd.ms-excel"));
    extensionToMediaType.put("txt", MediaType.parseMediaType("text/plain"));
    extensionToMediaType.put("csv", MediaType.parseMediaType("text/csv"));
    supportedMediaTypes.addAll(extensionToMediaType.values());
  }

  /**
   * Checks if the given filename has a valid file extension.
   *
   * @param file the file to validate
   * @return true if the file has a valid extension, false otherwise
   */
  public static boolean hasValidExtensionAndContentType(FilePart file) {
    String filename = file.filename();
    int lastDotIndex = filename.lastIndexOf('.');
    if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
      // No extension found or filename ends with a dot (invalid)
      return false;
    }
    String extension = filename.substring(lastDotIndex + 1).toLowerCase();
    MediaType fileMediaType = file.headers().getContentType();
    return extensionToMediaType.containsKey(extension)
        && supportedMediaTypes.contains(fileMediaType);
  }

  public static Mono<Boolean> hasValidExtensionAndMimeType(FilePart filePart) {
    String filename = filePart.filename();
    if (filename == null || !filename.contains(".")) {
      log.info("[ERROR][SECURITY ALERT] Invalid filename: {}", filename);
      return Mono.just(false);
    }

    String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    MediaType expectedMediaType = extensionToMediaType.get(extension);

    if (expectedMediaType == null) {
      log.info("[ERROR][SECURITY ALERT] Unsupported extension: {}", extension);
      return Mono.just(false);
    }

    return DataBufferUtils.join(filePart.content())
        .flatMap(
            dataBuffer -> {
              byte[] fileBytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(fileBytes);
              DataBufferUtils.release(dataBuffer);

              return Mono.fromCallable(
                  () -> {
                    try (InputStream input = new ByteArrayInputStream(fileBytes)) {
                      // Excel check
                      if ("xlsx".equals(extension)) {
                        try (XSSFWorkbook workbook = new XSSFWorkbook(input)) {
                          return true;
                        } catch (IOException e) {
                          log.warn("[ERROR][SECURITY ALERT] Invalid XLSX content: {}", filename);
                          return false;
                        }
                      } else if ("xls".equals(extension)) {
                        try (HSSFWorkbook workbook = new HSSFWorkbook(input)) {
                          return true;
                        } catch (IOException e) {
                          log.warn("[ERROR][SECURITY ALERT] Invalid XLS content: {}", filename);
                          return false;
                        }
                      }

                      // MIME detection
                      AutoDetectParser parser = new AutoDetectParser();
                      Metadata metadata = new Metadata();
                      BodyContentHandler handler = new BodyContentHandler(-1);
                      parser.parse(
                          new ByteArrayInputStream(fileBytes),
                          handler,
                          metadata,
                          new ParseContext());
                      String detectedMimeType = metadata.get(CONTENT_TYPE);
                      if (!isSupported(detectedMimeType)) {
                        log.warn(
                            "[ERROR][SECURITY ALERT] Unsupported MIME type: {}", detectedMimeType);
                        return false;
                      }
                      // compare detected vs expected MIME
                      MediaType detectedMediaType = MediaType.parseMediaType(detectedMimeType);
                      MediaType normalizedDetected =
                          new MediaType(
                              detectedMediaType.getType(), detectedMediaType.getSubtype());
                      MediaType normalizedExpected =
                          new MediaType(
                              expectedMediaType.getType(), expectedMediaType.getSubtype());

                      if (!normalizedDetected.equals(normalizedExpected)) {
                        log.warn(
                            "[ERROR][SECURITY ALERT] Extension '{}' does not match MIME type {}"
                                + " (expected {})",
                            extension,
                            detectedMediaType,
                            expectedMediaType);
                        return false;
                      }

                      // 4. Magic number validation
                      try (InputStream magicInput = new ByteArrayInputStream(fileBytes)) {
                        if (!validateMagicNumber(extension, magicInput)) {
                          log.warn(
                              "[ERROR][SECURITY ALERT] Magic number mismatch for extension: {}",
                              extension);
                          return false;
                        }
                      }

                      return true;
                    } catch (InvalidMediaTypeException e) {
                      log.error("[ERROR][SECURITY ALERT] Error validating file: {}", filename, e);
                      return false;
                    }
                  });
            });
  }

  private static boolean isSupported(String detectedMimeType) {
    try {
      if (detectedMimeType == null) {
        return false;
      }
      MediaType mediaType = MediaType.parseMediaType(detectedMimeType);
      MediaType baseType = new MediaType(mediaType.getType(), mediaType.getSubtype());
      Boolean supportedType = supportedMediaTypes.contains(baseType);
      if (Boolean.TRUE.equals(supportedType)) {
        return true;
      } else {
        log.warn(
            "[ERROR][SECURITY ALERT] detected MIME type: {} and media type: {} and base type: {} ",
            detectedMimeType,
            mediaType,
            baseType);
        return false;
      }

    } catch (InvalidMediaTypeException ex) {
      log.error("[ERROR][SECURITY ALERT] error in isSupported(): {}", detectedMimeType, ex);
      return false;
    }
  }

  private static boolean validateMagicNumber(String extension, InputStream input)
      throws IOException {
    return switch (extension) {
      case "pdf" -> hasPdfMagic(input);
      case "jpg", "jpeg" -> hasJpegMagic(input);
      case "png" -> hasPngMagic(input);
      default -> true;
    };
  }

  private static boolean hasPdfMagic(InputStream input) throws IOException {
    byte[] header = new byte[4];
    if (input.read(header) != 4) return false;
    return new String(header, StandardCharsets.US_ASCII).equals("%PDF");
  }

  private static boolean hasJpegMagic(InputStream input) throws IOException {
    byte[] header = new byte[3];
    if (input.read(header) != 3) return false;
    return (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
  }

  private static boolean hasPngMagic(InputStream input) throws IOException {
    byte[] header = new byte[8];
    if (input.read(header) != 8) return false;
    byte[] expected = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    return Arrays.equals(header, expected);
  }
}
