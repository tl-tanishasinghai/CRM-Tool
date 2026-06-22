package com.trillionloans.los.model.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientImageResponse {
  private Long id;
  private String accountNo;
  private String externalId;
  private Status status;
  private boolean active;
  private List<Integer> activationDate;
  private String firstname;
  private String middlename;
  private String lastname;
  private String displayName;
  private String mobileNo;
  private String alternateMobileNo;
  private List<Integer> dateOfBirth;
  private Gender gender;
  private SimpleFlag clientType;
  private SimpleFlag clientClassification;
  private SimpleFlag salutation;
  private SimpleFlag nationality;
  private Education education;
  private SimpleFlag riskCategory;
  private String emailId;
  private Long officeId;
  private String officeName;
  private Long imageId;
  private boolean imagePresent;
  private Timeline timeline;
  private LegalForm legalForm;
  private List<Object> groups;
  private ClientNonPersonDetails clientNonPersonDetails;
  private boolean isLocked;
  private boolean isWorkflowEnabled;
  private MaritalStatus maritalStatus;
  private boolean isVerified;
  private boolean isWorkflowEnableForBranch;
  private SimpleFlag clientReligion;
  private boolean isMobileVerified;
  private String officeHierarchy;
  private boolean isBlackListed;
  private List<Object> clientTags;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Status {
    private Long id;
    private String code;
    private String value;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Gender {
    private Long id;
    private String name;
    private boolean isActive;
    private boolean mandatory;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SimpleFlag {
    private boolean isActive;
    private boolean mandatory;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Education {
    private Long id;
    private String name;
    private boolean isActive;
    private boolean mandatory;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Timeline {
    private List<Integer> submittedOnDate;
    private String submittedByUsername;
    private String submittedByFirstname;
    private String submittedByLastname;
    private List<Integer> activatedOnDate;
    private String activatedByUsername;
    private String activatedByFirstname;
    private String activatedByLastname;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LegalForm {
    private Long id;
    private String code;
    private String value;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ClientNonPersonDetails {
    private SimpleFlag constitution;
    private SimpleFlag mainBusinessLine;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MaritalStatus {
    private Long id;
    private String name;
    private boolean isActive;
    private boolean mandatory;
  }
}
