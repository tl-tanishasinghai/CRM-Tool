package com.trillionloans.customer_portal.dto;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.trillionloans.customer_portal.model.dto.LeadDetailsDTO;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class LeadDetailsDTOTest {

  @Test
  public void testLeadDetailsDTOConstructor() {
    // Arrange
    String name = "John Doe";
    BigInteger leadId = new BigInteger("123456789");
    Integer age = 30;
    String dateOfBirth = "1993-06-15";
    String address = "123 Main St, Springfield";
    String email = "johndoe@example.com";
    String mobileNo = "+11234567890";
    String ucic = "ABC123456";
    String panNumber = "MUTS9287T";
    List<String> loanAccounts = new ArrayList<>(List.of("13123123", "123123123"));

    // Act
    LeadDetailsDTO leadDetails =
        LeadDetailsDTO.builder()
            .name(name)
            .leadId(leadId)
            .age(age)
            .dateOfBirth(dateOfBirth)
            .address(address)
            .email(email)
            .mobileNo(mobileNo)
            .ucic(ucic)
            .panNumber(panNumber)
            .loanAccounts(loanAccounts)
            .build();

    // Assert
    assertThat(leadDetails.getName()).isEqualTo(name);
    assertThat(leadDetails.getLeadId()).isEqualTo(leadId);
    assertThat(leadDetails.getAge()).isEqualTo(age);
    assertThat(leadDetails.getDateOfBirth()).isEqualTo(dateOfBirth);
    assertThat(leadDetails.getAddress()).isEqualTo(address);
    assertThat(leadDetails.getEmail()).isEqualTo(email);
    assertThat(leadDetails.getMobileNo()).isEqualTo(mobileNo);
    assertThat(leadDetails.getUcic()).isEqualTo(ucic);
    assertThat(leadDetails.getPanNumber()).isEqualTo(panNumber);
    assertThat(leadDetails.getLoanAccounts()).isEqualTo(loanAccounts);
  }

  @Test
  public void testLeadDetailsDTOWithConstructor() {
    // Arrange
    String name = "Jane Doe";
    BigInteger leadId = new BigInteger("987654321");
    Integer age = 25;
    String dateOfBirth = "1998-01-01";
    String address = "456 Elm St, Springfield";
    String email = "janedoe@example.com";
    String mobileNo = "+11234567891";
    String ucic = "XYZ987654";
    String panNumber = "MUTS9287T";
    List<String> loanAccounts = new ArrayList<>(List.of("13123123", "123123123"));

    // Act
    LeadDetailsDTO leadDetails =
        LeadDetailsDTO.builder()
            .name(name)
            .leadId(leadId)
            .age(age)
            .dateOfBirth(dateOfBirth)
            .address(address)
            .email(email)
            .mobileNo(mobileNo)
            .ucic(ucic)
            .panNumber(panNumber)
            .loanAccounts(loanAccounts)
            .build();
    // Assert
    assertThat(leadDetails.getName()).isEqualTo(name);
    assertThat(leadDetails.getLeadId()).isEqualTo(leadId);
    assertThat(leadDetails.getAge()).isEqualTo(age);
    assertThat(leadDetails.getDateOfBirth()).isEqualTo(dateOfBirth);
    assertThat(leadDetails.getAddress()).isEqualTo(address);
    assertThat(leadDetails.getEmail()).isEqualTo(email);
    assertThat(leadDetails.getMobileNo()).isEqualTo(mobileNo);
    assertThat(leadDetails.getUcic()).isEqualTo(ucic);
  }

  @Test
  public void testEqualsAndHashCode() {
    // Arrange
    LeadDetailsDTO leadDetails1 =
        LeadDetailsDTO.builder()
            .name("John Doe")
            .leadId(new BigInteger("123456789"))
            .age(30)
            .dateOfBirth("1993-06-15")
            .address("123 Main St")
            .email("johndoe@example.com")
            .mobileNo("+11234567890")
            .ucic("ABC123456")
            .panNumber("MUTS9287T")
            .loanAccounts(List.of("12345", "67890"))
            .build();

    LeadDetailsDTO leadDetails2 =
        LeadDetailsDTO.builder()
            .name("John Doe")
            .leadId(new BigInteger("123456789"))
            .age(30)
            .dateOfBirth("1993-06-15")
            .address("123 Main St")
            .email("johndoe@example.com")
            .mobileNo("+11234567890")
            .ucic("ABC123456")
            .panNumber("MUTS9287T")
            .loanAccounts(List.of("12345", "67890"))
            .build();

    // Act & Assert
    assertEquals(leadDetails1, leadDetails2);
    assertEquals(leadDetails1.hashCode(), leadDetails2.hashCode());
  }
}
