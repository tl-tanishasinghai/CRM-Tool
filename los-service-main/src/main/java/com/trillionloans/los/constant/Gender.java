package com.trillionloans.los.constant;

import java.util.stream.Stream;
import lombok.Getter;

@Getter
public enum Gender {
  MALE(23, "Male"),
  FEMALE(24, "Female"),
  OTHER(405, "Others");

  private final int genderId;
  private final String displayName;

  Gender(int genderId, String displayName) {
    this.genderId = genderId;
    this.displayName = displayName;
  }

  public static Integer getGenderId(String input) {
    if (input == null) return null;

    return Stream.of(values())
        .filter(gender -> gender.name().equalsIgnoreCase(input))
        .map(Gender::getGenderId)
        .findFirst()
        .orElse(null);
  }
}
