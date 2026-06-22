package com.trillionloans.los.constant;

import lombok.Getter;

/**
 * Enum representing different types of bank accounts. Each type is associated with a unique
 * identifier and a descriptive value.
 */
@Getter
public enum BankAccountType {

  /** Savings account type. */
  SAVINGS(1, "Savings Account"),

  SB(1, "Savings Account"),

  /** Current account type. */
  CURRENT(2, "Current Account"),

  CA(2, "Current Account"),

  /** Overdraft account type. */
  OD(3, "OD Account"),

  /** Credit card account type. */
  CC(4, "CC Account"),

  OTHER(5, "OTHER");

  private final int id; // Unique identifier for the account type
  private final String value; // Descriptive name of the account type

  /**
   * Constructor for creating a BankAccountType instance.
   *
   * @param id the unique identifier for the account type
   * @param value the descriptive name of the account type
   */
  BankAccountType(int id, String value) {
    this.id = id;
    this.value = value;
  }
}
