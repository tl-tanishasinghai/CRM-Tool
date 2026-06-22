package com.trillionloans.customer_portal.util;

import static com.trillionloans.customer_portal.constant.StringConstants.MMM_D_YYYY;
import static com.trillionloans.customer_portal.util.DateTimeUtil.convertDate;

import com.trillionloans.customer_portal.model.dto.LeadDetailsDTO;
import com.trillionloans.customer_portal.model.dto.LeadDetailsResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class LeadDetailsUtil {

  private static String buildFullName(String firstName, String middleName, String lastName) {
    StringBuilder fullName = new StringBuilder();
    if (firstName != null && !firstName.isEmpty()) fullName.append(firstName);
    if (middleName != null && !middleName.isEmpty()) fullName.append(" ").append(middleName);
    if (lastName != null && !lastName.isEmpty()) fullName.append(" ").append(lastName);
    return fullName.toString().trim();
  }

  private static int calculateAge(String dobString) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(MMM_D_YYYY);
    LocalDate dob = LocalDate.parse(dobString, formatter);
    LocalDate currentDate = LocalDate.now();
    int age = (int) ChronoUnit.YEARS.between(dob, currentDate);
    if (dob.plusYears(age).isAfter(currentDate)) {
      age--;
    }
    return age;
  }

  private static String buildAddress(
      String addressLineOne, String addressLineTwo, String landmark, String postalCode) {
    StringBuilder fullAddress = new StringBuilder();
    if (addressLineOne != null && !addressLineOne.isEmpty())
      fullAddress.append(addressLineOne).append(",");
    if (addressLineTwo != null && !addressLineTwo.isEmpty())
      fullAddress.append(" ").append(addressLineTwo).append(",");
    if (landmark != null && !landmark.isEmpty())
      fullAddress.append(" ").append(landmark).append(",");
    if (postalCode != null && !postalCode.isEmpty()) fullAddress.append(" ").append(postalCode);
    return fullAddress.toString().trim();
  }

  public static List<String> convertCommaSeparatedStringToList(String commaSeparatedString) {
    if (commaSeparatedString == null || commaSeparatedString.isBlank()) {
      return Collections.emptyList();
    }
    return Arrays.stream(commaSeparatedString.split(",")).map(String::trim).toList();
  }

  public static LeadDetailsDTO transformLeadDetails(LeadDetailsResponse leadDetailsResponse) {

    LeadDetailsDTO leadDetailsDTO = new LeadDetailsDTO();
    leadDetailsDTO.setLeadId(leadDetailsResponse.getLeadId());
    leadDetailsDTO.setEmail(leadDetailsResponse.getEmail());
    leadDetailsDTO.setMobileNo(leadDetailsResponse.getMobileNo());
    leadDetailsDTO.setUcic(leadDetailsResponse.getUcic());
    leadDetailsDTO.setName(leadDetailsResponse.getName());
    String dob = leadDetailsResponse.getDateOfBirth();
    String dateOfBirth = convertDate(dob);
    Integer age = calculateAge(dob);
    leadDetailsDTO.setDateOfBirth(dateOfBirth);
    leadDetailsDTO.setAge(age);
    leadDetailsDTO.setAddress(leadDetailsResponse.getAddress());
    leadDetailsDTO.setPanNumber(leadDetailsResponse.getPanNumber());
    leadDetailsDTO.setLoanAccounts(
        convertCommaSeparatedStringToList(leadDetailsResponse.getLoanAccounts()));
    return leadDetailsDTO;
  }
}
