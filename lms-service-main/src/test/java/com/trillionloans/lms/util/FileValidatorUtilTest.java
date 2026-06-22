package com.trillionloans.lms.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;

class FileValidatorUtilTest {

  private FilePart mockFilePart(String filename, MediaType mediaType) {
    FilePart filePart = mock(FilePart.class);
    when(filePart.filename()).thenReturn(filename);
    HttpHeaders headers = mock(HttpHeaders.class);

    when(filePart.filename()).thenReturn(filename);
    when(filePart.headers()).thenReturn(headers);
    when(headers.getContentType()).thenReturn(mediaType);

    return filePart;
  }

  @Test
  void testHasValidExtension_ValidExtensionsAndContentType() {
    assertTrue(
        FileValidatorUtil.hasValidExtensionAndContentType(
            mockFilePart("document.pdf", MediaType.APPLICATION_PDF)));

    assertTrue(
        FileValidatorUtil.hasValidExtensionAndContentType(
            mockFilePart(
                "spreadsheet.xlsx",
                MediaType.valueOf(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))));

    assertTrue(
        FileValidatorUtil.hasValidExtensionAndContentType(
            mockFilePart("image.jpeg", MediaType.IMAGE_JPEG)));

    assertTrue(
        FileValidatorUtil.hasValidExtensionAndContentType(
            mockFilePart("photo.png", MediaType.IMAGE_PNG)));

    assertTrue(
        FileValidatorUtil.hasValidExtensionAndContentType(
            mockFilePart("picture.jpg", MediaType.IMAGE_JPEG))); // jpg uses image/jpeg too
  }

  @Test
  void testHasValidExtension_AndContentType_InvalidExtensions() {
    MediaType dummyType = MediaType.APPLICATION_OCTET_STREAM; // common fallback type

    assertFalse(
        FileValidatorUtil.hasValidExtensionAndContentType(mockFilePart("document.exe", dummyType)));
    assertFalse(
        FileValidatorUtil.hasValidExtensionAndContentType(mockFilePart("file.txt", dummyType)));
    assertFalse(
        FileValidatorUtil.hasValidExtensionAndContentType(mockFilePart("archive.zip", dummyType)));
    assertFalse(
        FileValidatorUtil.hasValidExtensionAndContentType(mockFilePart("invalidfile", dummyType)));
    assertFalse(
        FileValidatorUtil.hasValidExtensionAndContentType(mockFilePart("noextension.", dummyType)));
  }

  @Test
  void testHasValidExtension_AndContentType_EmptyFilename() {
    MediaType dummyType = MediaType.APPLICATION_OCTET_STREAM;

    assertFalse(FileValidatorUtil.hasValidExtensionAndContentType(mockFilePart("", dummyType)));
  }
}
