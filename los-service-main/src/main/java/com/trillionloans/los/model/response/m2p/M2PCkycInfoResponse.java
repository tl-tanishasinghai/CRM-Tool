package com.trillionloans.los.model.response.m2p;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class M2PCkycInfoResponse {

  private PersonalInfo personalInfo;
  private List<AddressInfo> addressInfo;
  private List<IdentifierInfo> identifierInfo;
  private List<DocumentInfo> documentInfo;

  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @Getter
  @Setter
  public static class PersonalInfo {
    private String name;
    private String dob;
    private String gender;
    private String mobile;
    private String fatherName;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @Getter
  @Setter
  public static class AddressInfo {
    private String addressLine1;
    private String addressLine2;
    private String addressType;
    private String city;
    private String pincode;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @Getter
  @Setter
  public static class IdentifierInfo {
    private String documentType;
    private String id;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @Getter
  @Setter
  public static class DocumentInfo {
    private String documentType;
    private String documentId;
    private String documentExtension;
    private String documentBase64;
    private boolean isVideo;
  }
}
