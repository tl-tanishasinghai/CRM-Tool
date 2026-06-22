package com.trillionloans.los.model.response.m2p;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class M2pDocumentsUploadResponseDTO {
  public List<Doc> documents;

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class Doc {
    public int documentId;
    public DocumentDetails documentDetails;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class DocumentDetails {
    public boolean isImageQualityRequired;
    public boolean nameMatchRequired;
    public boolean isDOBMatchRequired;
    public boolean isOcrConfidenceScoreValidationRequired;
    public String tag;
    public Document document;
    public boolean ocrConfidenceScoreValidationRequired;
    public boolean imageQualityRequired;
    public boolean dobmatchRequired;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class Document {
    public String fileName;
    public String filePath;
    public String fileType;
    public String storageType;
  }
}
