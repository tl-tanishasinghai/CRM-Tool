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
public class M2pKycIdentifiersResponseDTO {
  public Integer id;
  public Integer clientId;
  public DocumentType documentType;
  public String documentKey;
  public String status;
  public SubCategoryType subCategoryType;
  public ClientIdentifierVerifiedDetailsData clientIdentifierVerifiedDetailsData;
  public List<Proof> proof;
  public String authStatus;

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class DocumentType {
    public Integer id;
    public String name;
    public Boolean isActive;
    public Boolean mandatory;
    public String systemIdentifier;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class SubCategoryType {
    public Integer id;
    public String name;
    public Boolean isActive;
    public Boolean mandatory;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class ClientIdentifierVerifiedDetailsData {
    public Boolean isVerified;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Getter
  @Setter
  public static class Proof {
    public Integer id;
    public String name;
    public Integer position;
    public String description;
    public Boolean isActive;
    public Integer codeScore;
    public Boolean mandatory;
    public String systemIdentifier;
  }
}
