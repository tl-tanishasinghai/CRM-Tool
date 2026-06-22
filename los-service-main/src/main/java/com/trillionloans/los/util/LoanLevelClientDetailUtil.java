package com.trillionloans.los.util;

import static com.trillionloans.los.constant.StringConstants.ACCOUNT_NUMBER;
import static com.trillionloans.los.constant.StringConstants.ACCOUNT_TYPE;
import static com.trillionloans.los.constant.StringConstants.ADDRESS_LINE_ONE;
import static com.trillionloans.los.constant.StringConstants.ADDRESS_LINE_TWO;
import static com.trillionloans.los.constant.StringConstants.ADDRESS_TYPE;
import static com.trillionloans.los.constant.StringConstants.ALTERNATE_MOBILE_NUMBER;
import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;
import static com.trillionloans.los.constant.StringConstants.DOB;
import static com.trillionloans.los.constant.StringConstants.EMAIL;
import static com.trillionloans.los.constant.StringConstants.EXTERNAL_ID;
import static com.trillionloans.los.constant.StringConstants.FIRST_NAME;
import static com.trillionloans.los.constant.StringConstants.GENDER;
import static com.trillionloans.los.constant.StringConstants.IFSC_CODE;
import static com.trillionloans.los.constant.StringConstants.LANDMARK;
import static com.trillionloans.los.constant.StringConstants.LAST_NAME;
import static com.trillionloans.los.constant.StringConstants.MIDDLE_NAME;
import static com.trillionloans.los.constant.StringConstants.MOBILE_NUMBER;
import static com.trillionloans.los.constant.StringConstants.NAME;
import static com.trillionloans.los.constant.StringConstants.OWNERSHIP_TYPE;
import static com.trillionloans.los.constant.StringConstants.POSTAL_CODE;
import static com.trillionloans.los.util.JsonUtils.parseJsonToList;
import static com.trillionloans.los.util.JsonUtils.parseJsonToMap;
import static com.trillionloans.los.util.LeadDataUtil.extractAddressField;
import static com.trillionloans.los.util.LeadDataUtil.extractFieldFromFamilyDetails;
import static com.trillionloans.los.util.LeadDataUtil.extractPanFromClientIdentifierDetails;
import static com.trillionloans.los.util.Util.safeParseInt;

import com.trillionloans.los.constant.AddressType;
import com.trillionloans.los.constant.Gender;
import com.trillionloans.los.model.dto.AddressDetailsDTO;
import com.trillionloans.los.model.dto.BankDetailsDTO;
import com.trillionloans.los.model.dto.ClientDetailsDTO;
import com.trillionloans.los.model.dto.EmploymentDetailsDTO;
import com.trillionloans.los.model.dto.internal.FieldChange;
import com.trillionloans.los.model.dto.internal.LoanLevelClientDetailsCacheDTO;
import com.trillionloans.los.model.entity.ClientCreationRequestDetail;
import com.trillionloans.los.model.entity.LoanLevelClientDetail;
import com.trillionloans.los.model.partner.m2p.M2pAddressDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pBankDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pClientDetailsUpdateDTO;
import com.trillionloans.los.model.partner.m2p.M2pEmploymentDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadUpdateDTO;
import com.trillionloans.los.model.request.Lead;
import com.trillionloans.los.model.response.ClientDetailsResponseDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class LoanLevelClientDetailUtil {

  private static final ZoneId IST_ZONE = ZoneId.of(ASIA_KOLKATA);

  public static LoanLevelClientDetail mapToLoanLevelDetail(
      ClientCreationRequestDetail clientCreationRequestDetail,
      String loanAppId,
      String loanApplicationReferenceNo) {
    return LoanLevelClientDetail.builder()
        .clientCreationReferenceId(String.valueOf(clientCreationRequestDetail.getId()))
        .loanApplicationId(loanAppId)
        .clientId(clientCreationRequestDetail.getClientId())
        .productCode(clientCreationRequestDetail.getProductCode())
        .loanApplicationReferenceNo(loanApplicationReferenceNo)
        .firstName(clientCreationRequestDetail.getFirstName())
        .middleName(clientCreationRequestDetail.getMiddleName())
        .lastName(clientCreationRequestDetail.getLastName())
        .gender(clientCreationRequestDetail.getGender())
        .dateOfBirth(clientCreationRequestDetail.getDateOfBirth())
        .email(clientCreationRequestDetail.getEmail())
        .mobileNo(clientCreationRequestDetail.getMobileNo())
        .alternateMobileNo(clientCreationRequestDetail.getAlternateMobileNo())
        .education(clientCreationRequestDetail.getEducation())
        .externalId(clientCreationRequestDetail.getExternalId())
        .addressDetails(clientCreationRequestDetail.getAddressDetails())
        .familyDetails(clientCreationRequestDetail.getFamilyDetails())
        .clientIdentifierDetails(clientCreationRequestDetail.getClientIdentifierDetails())
        .bankDetails(clientCreationRequestDetail.getBankDetails())
        .employmentDetails(clientCreationRequestDetail.getEmploymentDetails())
        .additionalDetails(clientCreationRequestDetail.getAdditionalDetails())
        .createdAt(LocalDateTime.now(IST_ZONE))
        .updatedAt(LocalDateTime.now(IST_ZONE))
        .build();
  }

  // converts LoanLevelClientDetail (entity) -> LoanLevelClientDetailsCacheDTO
  public static LoanLevelClientDetailsCacheDTO buildLoanLevelClientCacheObject(
      LoanLevelClientDetail loanLevelClientDetail) {

    Integer clientId = safeParseInt(loanLevelClientDetail.getClientId());
    Integer loanApplicationId = safeParseInt(loanLevelClientDetail.getLoanApplicationId());

    String productCode =
        Optional.ofNullable(loanLevelClientDetail.getProductCode()).orElse(StringUtils.EMPTY);
    // Name
    String firstName =
        Optional.ofNullable(loanLevelClientDetail.getFirstName()).orElse(StringUtils.EMPTY);
    String middleName =
        Optional.ofNullable(loanLevelClientDetail.getMiddleName()).orElse(StringUtils.EMPTY);
    String lastName =
        Optional.ofNullable(loanLevelClientDetail.getLastName()).orElse(StringUtils.EMPTY);

    // Date of birth
    String dateOfBirth =
        Optional.ofNullable(loanLevelClientDetail.getDateOfBirth()).orElse(StringUtils.EMPTY);

    // Father's name
    String fatherFirstName =
        extractFieldFromFamilyDetails(
            loanLevelClientDetail.getFamilyDetails(), "FATHER", FIRST_NAME);
    String fatherLastName =
        extractFieldFromFamilyDetails(
            loanLevelClientDetail.getFamilyDetails(), "FATHER", LAST_NAME);

    // PAN number
    String panNumber =
        extractPanFromClientIdentifierDetails(loanLevelClientDetail.getClientIdentifierDetails());

    String mobileNo =
        Optional.ofNullable(loanLevelClientDetail.getMobileNo()).orElse(StringUtils.EMPTY);

    String pincode =
        extractAddressField(loanLevelClientDetail.getAddressDetails(), "PERMANENT", POSTAL_CODE);

    if (pincode == null) {
      pincode =
          Optional.ofNullable(loanLevelClientDetail.getAadhaarPincode()).orElse(StringUtils.EMPTY);
    }

    // Build a cache object
    return LoanLevelClientDetailsCacheDTO.builder()
        .clientId(clientId)
        .loanApplicationId(loanApplicationId)
        .productCode(productCode)
        .firstName(firstName)
        .middleName(middleName)
        .lastName(lastName)
        .fatherFirstName(fatherFirstName)
        .fatherLastName(fatherLastName)
        .dateOfBirth(dateOfBirth)
        .panNumber(panNumber)
        .mobileNo(mobileNo)
        .pincode(pincode)
        .build();
  }

  // convert ClientDetailsResponseDto → LoanLevelClientDetailsCacheDTO
  public static LoanLevelClientDetailsCacheDTO toLoanLevelClientCacheDTO(
      ClientDetailsResponseDto client, String loanApplicationId, String productCode) {

    if (client == null) {
      return null;
    }
    Integer loanApplicationIdInt = safeParseInt(loanApplicationId);

    String pincode = Optional.ofNullable(client.getPostalCode()).orElse(StringUtils.EMPTY);

    return LoanLevelClientDetailsCacheDTO.builder()
        .loanApplicationId(loanApplicationIdInt)
        .clientId(client.getClientId())
        .productCode(productCode)
        .firstName(client.getFirstName())
        .middleName(client.getMiddleName())
        .lastName(client.getLastName())
        .fatherFirstName(client.getFfirstName())
        .fatherLastName(client.getFlastName())
        .dateOfBirth(client.getDateOfBirth())
        .panNumber(client.getClientPandocumentkey())
        .mobileNo(client.getMobileNo())
        .pincode(pincode)
        .build();
  }

  public static M2pLeadUpdateDTO buildMClientUpdateDto(
      LoanLevelClientDetail entity,
      Lead currentLead,
      Map<String, Object> changes,
      List<FieldChange> auditTrail) {
    if (changes.isEmpty()) return null;

    M2pLeadUpdateDTO updateDto = new M2pLeadUpdateDTO();
    updateDto.setLocale("en");
    updateDto.setDateFormat("yyyy-MM-dd");

    // Address Data
    if (changes.containsKey("addressDetailsData")) {
      log.info(
          "[LOAN_LEVEL_CLIENT_DATA][M_CLIENT_UPDATE][ADDRESS] updating address details to m2p");
      List<M2pAddressDetailsDTO> fullList =
          JsonUtils.parseJsonToList(entity.getAddressDetails()).stream()
              .map(
                  m -> {
                    List<String> internalTypes = (List<String>) m.get(ADDRESS_TYPE);
                    List<String> m2pTypes = Collections.emptyList();

                    if (internalTypes != null) {
                      m2pTypes =
                          internalTypes.stream()
                              .map(AddressType::getDisplayName)
                              .collect(Collectors.toList());
                    }

                    return M2pAddressDetailsDTO.builder()
                        .addressType(m2pTypes)
                        .addressLineOne((String) m.get(ADDRESS_LINE_ONE))
                        .addressLineTwo((String) m.get(ADDRESS_LINE_TWO))
                        .postalCode(String.valueOf(m.get(POSTAL_CODE)))
                        .landmark((String) m.get(LANDMARK))
                        .ownershipType((String) m.get(OWNERSHIP_TYPE))
                        .build();
                  })
              .collect(Collectors.toList());
      updateDto.setAddressData(fullList);
      auditTrail.add(
          FieldChange.builder()
              .field("addressDetails")
              .oldValue(currentLead.getAddressDetails())
              .newValue(parseJsonToList(entity.getAddressDetails()))
              .build());
    }

    // Employment Data
    if (changes.keySet().stream().anyMatch(k -> k.startsWith("employmentDetailData."))) {
      M2pEmploymentDetailsDTO emp = new M2pEmploymentDetailsDTO();
      Map<String, Object> entityEmp = parseJsonToMap(entity.getEmploymentDetails());
      // required parameter
      emp.setEmploymentType((String) entityEmp.get("employmentType"));
      if (changes.containsKey("employmentDetailData.employmentType")) {
        auditTrail.add(
            FieldChange.builder()
                .field("employmentDetail.employmentType")
                .oldValue(currentLead.getEmploymentDetails().getEmploymentType())
                .newValue(entityEmp.get("employmentType"))
                .build());
      }
      if (changes.containsKey("employmentDetailData.companyType")) {
        emp.setCompanyType((String) entityEmp.get("companyType"));
        auditTrail.add(
            FieldChange.builder()
                .field("employmentDetail.companyType")
                .oldValue(currentLead.getEmploymentDetails().getCompanyType())
                .newValue(entityEmp.get("companyType"))
                .build());
      }
      if (changes.containsKey("employmentDetailData.monthlySalary")) {
        emp.setMonthlySalary((Double) entityEmp.get("monthlySalary"));
        auditTrail.add(
            FieldChange.builder()
                .field("employmentDetail.monthlySalary")
                .oldValue(currentLead.getEmploymentDetails().getMonthlySalary())
                .newValue(entityEmp.get("monthlySalary"))
                .build());
      }
      if (changes.containsKey("employmentDetailData.totalWorkExperience")) {
        emp.setTotalWorkExperience((Integer) entityEmp.get("totalWorkExperience"));
        auditTrail.add(
            FieldChange.builder()
                .field("employmentDetail.totalWorkExperience")
                .oldValue(currentLead.getEmploymentDetails().getTotalWorkExperience())
                .newValue(entityEmp.get("totalWorkExperience"))
                .build());
      }
      if (changes.containsKey("employmentDetailData.currentEmployerName")) {
        emp.setCurrentEmployerName((String) entityEmp.get("currentEmployerName"));
        auditTrail.add(
            FieldChange.builder()
                .field("employmentDetail.currentEmployerName")
                .oldValue(currentLead.getEmploymentDetails().getCurrentEmployerName())
                .newValue(entityEmp.get("currentEmployerName"))
                .build());
      }
      if (changes.containsKey("employmentDetailData.existingIncomeObligation")) {
        emp.setExistingIncomeObligation((String) entityEmp.get("existingIncomeObligation"));
        auditTrail.add(
            FieldChange.builder()
                .field("employmentDetail.existingIncomeObligation")
                .oldValue(currentLead.getEmploymentDetails().getExistingIncomeObligation())
                .newValue(entityEmp.get("existingIncomeObligation"))
                .build());
      }
      if (changes.containsKey("employmentDetailData.occupationType")) {
        emp.setOccupationType((String) entityEmp.get("occupationType"));
        auditTrail.add(
            FieldChange.builder()
                .field("employmentDetail.occupationType")
                .oldValue(currentLead.getEmploymentDetails().getOccupationType())
                .newValue(entityEmp.get("occupationType"))
                .build());
      }
      updateDto.setEmploymentDetailData(emp);
    }

    // Bank Data
    if (changes.containsKey("bankDetailsData")) {
      log.info(
          "[LOAN_LEVEL_CLIENT_DATA][M_CLIENT_UPDATE][BANK] updating bank details for clientId: {}",
          entity.getClientId());

      List<M2pBankDetailsDTO> fullBankList =
          parseJsonToList(entity.getBankDetails()).stream()
              .map(
                  m -> {
                    M2pBankDetailsDTO bank = new M2pBankDetailsDTO();
                    bank.setAccountNumber((String) m.get(ACCOUNT_NUMBER));
                    bank.setIfscCode((String) m.get(IFSC_CODE));
                    bank.setName((String) m.get(NAME));
                    bank.setAccountType((String) m.get(ACCOUNT_TYPE));
                    bank.setSupportedForRepayment((Boolean) m.get("supportedForRepayment"));
                    bank.setSupportedForDisbursement((Boolean) m.get("supportedForDisbursement"));
                    return bank;
                  })
              .collect(Collectors.toList());

      updateDto.setBankDetailsData(fullBankList);
      auditTrail.add(
          FieldChange.builder()
              .field("bankDetails")
              .oldValue(currentLead.getBankDetails())
              .newValue(parseJsonToList(entity.getBankDetails()))
              .build());
    }

    return updateDto;
  }

  public static M2pClientDetailsUpdateDTO buildClientDetailsUpdateDto(
      LoanLevelClientDetail entity,
      Lead currentLead,
      Map<String, Object> changes,
      List<FieldChange> auditTrail) {
    if (changes.isEmpty()) return null;

    boolean hasClientChanges = changes.keySet().stream().anyMatch(k -> k.startsWith("clientData."));
    if (!hasClientChanges) {
      return null;
    }

    M2pClientDetailsUpdateDTO client = new M2pClientDetailsUpdateDTO();
    client.setLocale("en");
    client.setDateFormat("yyyy-MM-dd");

    // required parameter
    client.setFirstname(entity.getFirstName());
    if (changes.containsKey("clientData.firstName")) {
      auditTrail.add(
          FieldChange.builder()
              .field(FIRST_NAME)
              .oldValue(currentLead.getClientDetails().getFirstName())
              .newValue(entity.getFirstName())
              .build());
    }
    if (changes.containsKey("clientData.middleName")) {
      client.setMiddlename(entity.getMiddleName());
      auditTrail.add(
          FieldChange.builder()
              .field(MIDDLE_NAME)
              .oldValue(currentLead.getClientDetails().getMiddleName())
              .newValue(entity.getMiddleName())
              .build());
    }
    if (changes.containsKey("clientData.lastName")) {
      client.setLastname(entity.getLastName());
      auditTrail.add(
          FieldChange.builder()
              .field(LAST_NAME)
              .oldValue(currentLead.getClientDetails().getLastName())
              .newValue(entity.getLastName())
              .build());
    }
    if (changes.containsKey("clientData.gender")) {
      Integer genderId = Gender.getGenderId(entity.getGender());
      client.setGenderId(genderId);
      auditTrail.add(
          FieldChange.builder()
              .field(GENDER)
              .oldValue(currentLead.getClientDetails().getGender())
              .newValue(entity.getGender())
              .build());
    }
    if (changes.containsKey("clientData.mobileNo")) {
      client.setMobileNo(entity.getMobileNo());
      auditTrail.add(
          FieldChange.builder()
              .field(MOBILE_NUMBER)
              .oldValue(currentLead.getClientDetails().getMobileNo())
              .newValue(entity.getMobileNo())
              .build());
    }
    if (changes.containsKey("clientData.alternateMobileNo")) {
      client.setAlternateMobileNo(entity.getAlternateMobileNo());
      auditTrail.add(
          FieldChange.builder()
              .field(ALTERNATE_MOBILE_NUMBER)
              .oldValue(currentLead.getClientDetails().getAlternateMobileNo())
              .newValue(entity.getAlternateMobileNo())
              .build());
    }
    if (changes.containsKey("clientData.email")) {
      client.setEmail(entity.getEmail());
      auditTrail.add(
          FieldChange.builder()
              .field(EMAIL)
              .oldValue(currentLead.getClientDetails().getEmail())
              .newValue(entity.getEmail())
              .build());
    }
    if (changes.containsKey("clientData.dateOfBirth")) {
      client.setDateOfBirth(normalizeDate(entity.getDateOfBirth()));
      auditTrail.add(
          FieldChange.builder()
              .field(DOB)
              .oldValue(currentLead.getClientDetails().getDateOfBirth())
              .newValue(entity.getDateOfBirth())
              .build());
    }
    if (changes.containsKey("clientData.externalId")) {
      client.setExternalId(entity.getExternalId());
      auditTrail.add(
          FieldChange.builder()
              .field(EXTERNAL_ID)
              .oldValue(currentLead.getClientDetails().getExternalId())
              .newValue(entity.getExternalId())
              .build());
    }
    return client;
  }

  public static Map<String, Object> calculateClientDetailFieldChanges(
      LoanLevelClientDetail entity, Lead currentLead) {

    Map<String, Object> changes = new HashMap<>();
    if (currentLead == null) return changes;

    // Client Details
    ClientDetailsDTO leadDetails = currentLead.getClientDetails();
    if (leadDetails != null) {
      compareAndAdd(
          changes,
          "clientData.firstName",
          normalize(leadDetails.getFirstName()),
          normalize(entity.getFirstName()));
      compareAndAdd(
          changes,
          "clientData.middleName",
          normalize(leadDetails.getMiddleName()),
          normalize(entity.getMiddleName()));
      compareAndAdd(
          changes,
          "clientData.lastName",
          normalize(leadDetails.getLastName()),
          normalize(entity.getLastName()));
      compareAndAdd(
          changes,
          "clientData.dateOfBirth",
          normalizeDate(leadDetails.getDateOfBirth()),
          normalizeDate(entity.getDateOfBirth()));
      compareAndAdd(
          changes,
          "clientData.gender",
          normalize(leadDetails.getGender()),
          normalize(entity.getGender()));
      compareAndAdd(
          changes, "clientData.mobileNo", leadDetails.getMobileNo(), entity.getMobileNo());
      compareAndAdd(
          changes,
          "clientData.alternateMobileNo",
          leadDetails.getAlternateMobileNo(),
          entity.getAlternateMobileNo());
      compareAndAdd(changes, "clientData.email", leadDetails.getEmail(), entity.getEmail());
      compareAndAdd(
          changes, "clientData.externalId", leadDetails.getExternalId(), entity.getExternalId());
    }

    // Address Details
    List<Map<String, Object>> entityAddrs = JsonUtils.parseJsonToList(entity.getAddressDetails());
    List<AddressDetailsDTO> leadAddrs =
        currentLead.getAddressDetails() != null
            ? currentLead.getAddressDetails()
            : Collections.emptyList();

    boolean addressMismatch = false;

    for (Map<String, Object> dbAddrMap : entityAddrs) {
      Object typeObj = dbAddrMap.get("addressType");
      String dbType = null;
      if (typeObj instanceof List<?> list && !list.isEmpty()) {
        dbType = String.valueOf(list.get(0));
      } else if (typeObj != null) {
        dbType = String.valueOf(typeObj);
      }
      if (dbType == null) continue;
      final String currentType = dbType;

      Optional<AddressDetailsDTO> m2pAddr =
          leadAddrs.stream()
              .filter(
                  addressDetailsDTO -> {
                    if (addressDetailsDTO == null
                        || addressDetailsDTO.getAddressType() == null
                        || addressDetailsDTO.getAddressType().isEmpty()) {
                      return false;
                    }
                    Object addrType = addressDetailsDTO.getAddressType().get(0);

                    String calculatedType =
                        (addrType instanceof Enum<?> e) ? e.name() : String.valueOf(addrType);

                    return currentType.equalsIgnoreCase(calculatedType);
                  })
              .findFirst();

      // Case: Type Mismatch
      if (m2pAddr.isEmpty()) {
        addressMismatch = true;
        break;
      }

      // Case: Normalized Content Comparison
      AddressDetailsDTO lead = m2pAddr.get();

      boolean isDifferent =
          !Objects.equals(
                  normalize(lead.getAddressLineOne()), normalize(dbAddrMap.get("addressLineOne")))
              || !Objects.equals(lead.getPostalCode(), dbAddrMap.get("postalCode"));

      if (isDifferent) {
        addressMismatch = true;
        break;
      }
    }

    if (addressMismatch) {
      changes.put("addressDetailsData", "NORMALIZED_TYPE_MISMATCH");
    }

    // Employment Details
    Map<String, Object> entityEmp = parseJsonToMap(entity.getEmploymentDetails());
    if (currentLead.getEmploymentDetails() != null && !entityEmp.isEmpty()) {
      String prefix = "employmentDetailData.";
      EmploymentDetailsDTO leadEmp = currentLead.getEmploymentDetails();
      compareAndAdd(
          changes,
          prefix + "currentEmployerName",
          normalize(leadEmp.getCurrentEmployerName()),
          normalize(entityEmp.get("currentEmployerName")));
      compareAndAdd(
          changes,
          prefix + "monthlySalary",
          leadEmp.getMonthlySalary(),
          (Double) entityEmp.get("monthlySalary"));
      compareAndAdd(
          changes,
          prefix + "companyType",
          normalize(leadEmp.getCompanyType()),
          normalize(entityEmp.get("companyType")));
      compareAndAdd(
          changes,
          prefix + "employmentType",
          normalize(leadEmp.getEmploymentType()),
          normalize(entityEmp.get("employmentType")));
      compareAndAdd(
          changes,
          prefix + "occupationType",
          normalize(leadEmp.getOccupationType()),
          normalize(entityEmp.get("occupationType")));
      compareAndAdd(
          changes,
          prefix + "totalWorkExperience",
          leadEmp.getTotalWorkExperience(),
          (Integer) entityEmp.get("totalWorkExperience"));
      compareAndAdd(
          changes,
          prefix + "existingIncomeObligation",
          leadEmp.getExistingIncomeObligation(),
          (String) entityEmp.get("existingIncomeObligation"));
    }

    // Bank Details (Account Number Set Comparison)
    List<Map<String, Object>> entityBanksList = JsonUtils.parseJsonToList(entity.getBankDetails());
    Map<String, Map<String, Object>> dbBanksMap =
        (entityBanksList == null)
            ? Collections.emptyMap()
            : entityBanksList.stream()
                .filter(m -> StringUtils.isNotBlank(Objects.toString(m.get("accountNumber"), "")))
                .collect(
                    Collectors.toMap(
                        m -> Objects.toString(m.get("accountNumber"), ""),
                        m -> m,
                        (existing, replacement) -> existing // Handle duplicates if any
                        ));

    List<BankDetailsDTO> leadBanks =
        currentLead.getBankDetails() != null
            ? currentLead.getBankDetails()
            : Collections.emptyList();

    boolean isMismatch = false;

    if (leadBanks.size() < dbBanksMap.size()) {
      isMismatch = true;
    } else {
      for (BankDetailsDTO leadBank : leadBanks) {
        String accNo = leadBank.getAccountNumber();
        if (StringUtils.isBlank(accNo)) continue;

        Map<String, Object> dbBank = dbBanksMap.get(accNo);

        if (dbBank != null) {
          boolean fieldsMatch =
              Objects.equals(
                      leadBank.getAccountType(), Objects.toString(dbBank.get("accountType"), null))
                  && Objects.equals(normalize(leadBank.getName()), normalize(dbBank.get("name")))
                  && Objects.equals(
                      leadBank.getIfscCode(), Objects.toString(dbBank.get("ifscCode"), null))
                  && Objects.equals(
                      leadBank.getSupportedForRepayment(), dbBank.get("supportedForRepayment"))
                  && Objects.equals(
                      leadBank.getSupportedForDisbursement(),
                      dbBank.get("supportedForDisbursement"));

          if (!fieldsMatch) {
            isMismatch = true;
            break;
          }
        }
      }

      Set<String> leadAccSet =
          leadBanks.stream().map(BankDetailsDTO::getAccountNumber).collect(Collectors.toSet());
      if (!leadAccSet.containsAll(dbBanksMap.keySet())) {
        isMismatch = true;
      }
    }

    if (isMismatch) {
      changes.put("bankDetailsData", "BANK_DETAILS_FIELD_MISMATCH");
    }

    return changes;
  }

  private static void compareAndAdd(
      Map<String, Object> changes, String key, Object oldVal, Object newVal) {
    if (!Objects.equals(oldVal, newVal) && newVal != null) {
      changes.put(key, newVal);
    }
  }

  private static String normalize(Object val) {
    if (val == null) return "";
    return String.valueOf(val).toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
  }

  private static String normalizeDate(Object val) {
    if (val == null || StringUtils.isBlank(String.valueOf(val))) return "";

    String dateStr = String.valueOf(val).trim();
    String[] patterns = {"yyyy-MM-dd", "dd-MM-yyyy", "yyyy/MM/dd", "dd/MM/yyyy"};

    for (String pattern : patterns) {
      try {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        LocalDate date = LocalDate.parse(dateStr, formatter);
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
      } catch (DateTimeParseException ignored) {
        // Try next pattern
      }
    }
    // Fallback: Strip everything except digits
    return dateStr.replaceAll("[^0-9]", "");
  }
}
