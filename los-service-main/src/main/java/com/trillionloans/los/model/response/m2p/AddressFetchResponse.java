package com.trillionloans.los.model.response.m2p;

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
public class AddressFetchResponse {
  private Long addressId;
  private String addressLineOne;
  private String addressLineTwo;
  private String landmark;
  private DistrictData districtData;
  private StateData stateData;
  private CountryData countryData;
  private String postalCode;
  private Integer noOfYrsAtCurrentResidence;
  private Ownership ownership;
  private Double latitude;
  private Double longitude;
  private List<AddressEntityData> addressEntityData;
  private Boolean isServicible;
  private Long documentId;
  private Boolean isLocked;
  private String villageTown;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DistrictData {
    private Long districtId;
    private Long stateId;
    private String districtName;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StateData {
    private Long stateId;
    private Long countryId;
    private String stateName;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CountryData {
    private Long countryId;
    private String countryName;
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Ownership {
    private String value; // e.g., "own", "rent"
  }

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AddressEntityData {
    private Long id;
    private Long addressId;
    private AddressType addressType;
    private Long entityId;
    private EntityType entityType;
    private Boolean isActive;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressType {
      private Long id;
      private String name;
      private Boolean isActive;
      private Boolean mandatory;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityType {
      private Long id;
      private String code;
      private String value;
    }
  }
}
