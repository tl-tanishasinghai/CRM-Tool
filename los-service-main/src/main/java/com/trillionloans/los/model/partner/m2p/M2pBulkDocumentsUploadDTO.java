package com.trillionloans.los.model.partner.m2p;

import com.trillionloans.los.constant.DocumentTag;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class M2pBulkDocumentsUploadDTO {

  private List<M2pBulkDocumentsUploadDTO.DocumentDetailsDTO> documents;

  @Builder
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class DocumentDetailsDTO {
    private DocumentTag tag;
    private M2pBulkDocumentsUploadDTO.DocumentInfoDTO document;
  }

  @Builder
  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  public static class DocumentInfoDTO {
    private String fileName;
    private String filePath;
    private String fileType;
    private String storageType;
  }
}
