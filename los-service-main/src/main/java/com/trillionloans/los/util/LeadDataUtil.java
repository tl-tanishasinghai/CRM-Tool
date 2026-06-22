package com.trillionloans.los.util;

import static com.trillionloans.los.constant.StringConstants.DATE_FORMAT;
import static com.trillionloans.los.constant.StringConstants.DD_MMMM_YYYY;
import static com.trillionloans.los.constant.StringConstants.DD_MM_YYYY;
import static com.trillionloans.los.constant.StringConstants.EN;
import static com.trillionloans.los.constant.StringConstants.INDIVIDUAL;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.constant.AddressType;
import com.trillionloans.los.model.dto.AadhaarXmlDetailsDTO;
import com.trillionloans.los.model.dto.AdditionalDetailsDTO;
import com.trillionloans.los.model.dto.AddressDetailsDTO;
import com.trillionloans.los.model.dto.BankDetailsDTO;
import com.trillionloans.los.model.dto.ClientDetailsDTO;
import com.trillionloans.los.model.dto.ClientDetailsUpdateDTO;
import com.trillionloans.los.model.dto.ClientIdentifierDetailsDTO;
import com.trillionloans.los.model.dto.EmploymentDetailsDTO;
import com.trillionloans.los.model.dto.EmploymentDetailsUpdateDTO;
import com.trillionloans.los.model.dto.FamilyDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pAdditionalDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pAddressDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pBankDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pClientDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pClientDetailsUpdateDTO;
import com.trillionloans.los.model.partner.m2p.M2pClientIdentifierDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pEmploymentDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pFamilyDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadUpdateDTO;
import com.trillionloans.los.model.request.Lead;
import com.trillionloans.los.model.request.LeadUpdate;
import com.trillionloans.los.model.request.RepaymentScheduleRequest;
import com.trillionloans.los.model.request.m2p.M2pDedupeRequest;
import com.trillionloans.los.model.response.m2p.M2pProductDetailsResponseDTO;
import io.r2dbc.postgresql.codec.Json;
import jakarta.validation.Valid;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import reactor.core.publisher.Mono;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LeadDataUtil {
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private static List<String> getAddressTypeNames(List<AddressType> addressTypeEnums) {
    if (Objects.isNull(addressTypeEnums)) {
      return new ArrayList<>();
    }
    return addressTypeEnums.stream().map(AddressType::getDisplayName).toList();
  }

  public static M2pLeadRequestDTO getM2pLeadRequest(Lead leadData, String officeName) {
    return M2pLeadRequestDTO.builder()
        .locale("en")
        .dateFormat(DATE_FORMAT)
        .clientData(getM2pClientDetailsDTO(leadData.getClientDetails(), officeName))
        .addressData(getM2pAddressDetailsDTO(leadData.getAddressDetails()))
        .familyDetailsData(getM2pFamilyDetailsDTO(leadData.getFamilyDetails()))
        .clientIdentifierData(
            getM2pClientIdentifierDetailsDTO(leadData.getClientIdentifierDetails()))
        .bankDetailsData(getM2pBankDetailsDTO(leadData.getBankDetails()))
        .employmentDetailData(getM2pEmploymentDetailsDTO(leadData.getEmploymentDetails()))
        .additionalDetails(
            getM2pAdditionalDetailsDTO(
                leadData.getAdditionalDetails(), leadData.getClientIdentifierDetails()))
        .build();
  }

  public static M2pLeadUpdateDTO getM2pLeadUpdateRequest(LeadUpdate leadData) {
    return M2pLeadUpdateDTO.builder()
        .locale("en")
        .dateFormat(DATE_FORMAT)
        .clientData(getM2pClientDetailsUpdateDTO(leadData.getClientDetails()))
        .addressData(getM2pAddressDetailsDTO(leadData.getAddressDetails()))
        .familyDetailsData(getM2pFamilyDetailsDTO(leadData.getFamilyDetails()))
        .bankDetailsData(getM2pBankDetailsDTO(leadData.getBankDetails()))
        .employmentDetailData(getM2pEmploymentDetailsUpdateDTO(leadData.getEmploymentDetails()))
        .additionalDetails(getUpdateM2pAdditionalDetailsDTO(leadData.getAdditionalDetails()))
        .build();
  }

  private static M2pClientDetailsUpdateDTO getM2pClientDetailsUpdateDTO(
      ClientDetailsUpdateDTO clientDetails) {
    if (Objects.isNull(clientDetails)) {
      return null;
    }
    return M2pClientDetailsUpdateDTO.builder()
        .education(clientDetails.getEducation())
        .email(clientDetails.getEmail())
        .build();
  }

  private static List<M2pAdditionalDetailsDTO> getM2pAdditionalDetailsDTO(
      List<AdditionalDetailsDTO> additionalDetails,
      @Valid List<ClientIdentifierDetailsDTO> clientIdentifierDetails) {

    List<M2pAdditionalDetailsDTO> m2pAdditionalDetailsList = new ArrayList<>();
    String ucicValue = generateUcic(clientIdentifierDetails);
    m2pAdditionalDetailsList.add(
        M2pAdditionalDetailsDTO.builder()
            .appTable("m_client")
            .dataTableName("ucic")
            .ucic(ucicValue)
            .build());
    if (Objects.isNull(additionalDetails) || additionalDetails.isEmpty()) {
      return m2pAdditionalDetailsList;
    }
    for (AdditionalDetailsDTO detail : additionalDetails) {
      m2pAdditionalDetailsList.add(
          M2pAdditionalDetailsDTO.builder()
              .dataTableName(detail.getDataTableName())
              .appTable(detail.getAppTable())
              .mPin(detail.getMPin())
              .subIndustry(detail.getSubIndustry())
              .industry(detail.getIndustry())
              .businessType(detail.getBusinessType())
              .businessAddress(detail.getBusinessAddress())
              .businessAddressType(detail.getBusinessAddressType())
              .businessName(detail.getBusinessName())
              .state(detail.getState())
              .city(detail.getCity())
              .country(detail.getCountry())
              .postalCode(detail.getPostalCode())
              .vernacularPreference(detail.getVernacularPreference())
              .vcipStatus(detail.getVcipStatus())
              .vcipRejectionReason(detail.getVcipRejectionReason())
              .okycTimeStamp(detail.getOkycTimeStamp())
              .gstNumber(detail.getGstNumber())
              .businessDocument(detail.getBusinessDocument())
              .legalName(detail.getLegalName())
              .tradeName(detail.getTradeName())
              .bankBeneName(detail.getBankBeneName())
              .udyamNumber(detail.getUdhyamNumber())
              .dateOfIncorporation(detail.getDateOfIncorporation())
              .build());
    }
    return m2pAdditionalDetailsList;
  }

  private static String generateUcic(
      @Valid List<ClientIdentifierDetailsDTO> clientIdentifierDetails) {
    if (Objects.isNull(clientIdentifierDetails) || clientIdentifierDetails.isEmpty()) {
      return null;
    }
    String panNumber = null;
    for (ClientIdentifierDetailsDTO clientIdentifierDetailsData : clientIdentifierDetails) {
      if (Objects.equals(clientIdentifierDetailsData.getDocumentType(), "PAN")) {
        panNumber = clientIdentifierDetailsData.getDocumentKey();
      }
    }
    return panNumber == null ? null : generateUcic(panNumber);
  }

  public static String generateUcic(String panNumber) {
    StringBuilder result = new StringBuilder();
    for (char c : panNumber.toCharArray()) {
      if (Character.isLetter(c)) {
        char upperCaseChar = Character.toUpperCase(c);
        result.append(String.format("%02d", upperCaseChar - 'A' + 1));
      } else {
        result.append(c);
      }
    }
    return result.toString();
  }

  private static M2pEmploymentDetailsDTO getM2pEmploymentDetailsDTO(
      EmploymentDetailsDTO employmentDetails) {
    if (Objects.isNull(employmentDetails)) {
      return null;
    }
    return M2pEmploymentDetailsDTO.builder()
        .companyType(employmentDetails.getCompanyType())
        .employmentType(employmentDetails.getEmploymentType())
        .monthlySalary(employmentDetails.getMonthlySalary())
        .totalWorkExperience(employmentDetails.getTotalWorkExperience())
        .currentEmployerName(employmentDetails.getCurrentEmployerName())
        .existingIncomeObligation(employmentDetails.getExistingIncomeObligation())
        .occupationType(employmentDetails.getOccupationType())
        .build();
  }

  private static M2pEmploymentDetailsDTO getM2pEmploymentDetailsUpdateDTO(
      EmploymentDetailsUpdateDTO employmentDetails) {
    if (Objects.isNull(employmentDetails)) {
      return null;
    }
    return M2pEmploymentDetailsDTO.builder()
        .companyType(employmentDetails.getCompanyType())
        .employmentType(employmentDetails.getEmploymentType())
        .monthlySalary(employmentDetails.getMonthlySalary())
        .totalWorkExperience(employmentDetails.getTotalWorkExperience())
        .currentEmployerName(employmentDetails.getCurrentEmployerName())
        .build();
  }

  private static List<M2pBankDetailsDTO> getM2pBankDetailsDTO(List<BankDetailsDTO> bankDetails) {
    List<M2pBankDetailsDTO> m2pBankDetailsList = new ArrayList<>();
    if (Objects.isNull(bankDetails)) {
      return m2pBankDetailsList;
    }
    for (BankDetailsDTO bank : bankDetails) {
      m2pBankDetailsList.add(
          M2pBankDetailsDTO.builder()
              .accountType(bank.getAccountType())
              .name(bank.getName())
              .accountNumber(bank.getAccountNumber())
              .ifscCode(bank.getIfscCode())
              .supportedForDisbursement(bank.getSupportedForDisbursement())
              .supportedForRepayment(bank.getSupportedForRepayment())
              .build());
    }
    return m2pBankDetailsList;
  }

  private static List<M2pClientIdentifierDetailsDTO> getM2pClientIdentifierDetailsDTO(
      List<ClientIdentifierDetailsDTO> clientIdentifierDetails) {
    List<M2pClientIdentifierDetailsDTO> m2pClientIdentifierDetailsList = new ArrayList<>();
    if (Objects.isNull(clientIdentifierDetails)) {
      return m2pClientIdentifierDetailsList;
    }
    for (ClientIdentifierDetailsDTO clientIdentifier : clientIdentifierDetails) {
      m2pClientIdentifierDetailsList.add(
          M2pClientIdentifierDetailsDTO.builder()
              .documentKey(clientIdentifier.getDocumentKey())
              .issueDate(clientIdentifier.getIssueDate())
              .expiryDate(clientIdentifier.getExpiryDate())
              .documentType(clientIdentifier.getDocumentType())
              .build());
    }
    return m2pClientIdentifierDetailsList;
  }

  private static List<M2pFamilyDetailsDTO> getM2pFamilyDetailsDTO(
      List<FamilyDetailsDTO> familyDetails) {
    List<M2pFamilyDetailsDTO> m2pFamilyDetailsList = new ArrayList<>();
    if (Objects.isNull(familyDetails)) {
      return m2pFamilyDetailsList;
    }
    for (FamilyDetailsDTO family : familyDetails) {
      m2pFamilyDetailsList.add(
          M2pFamilyDetailsDTO.builder()
              .firstName(family.getFirstName())
              .lastName(family.getLastName())
              .documentType(family.getDocumentType())
              .documentKey(family.getDocumentKey())
              .dateOfBirth(family.getDateOfBirth())
              .relationship(
                  Objects.isNull(family.getRelationship())
                      ? null
                      : family.getRelationship().getDisplayName())
              .gender(
                  Objects.isNull(family.getGender()) ? null : family.getGender().getDisplayName())
              .build());
    }
    return m2pFamilyDetailsList;
  }

  private static List<M2pAddressDetailsDTO> getM2pAddressDetailsDTO(
      List<AddressDetailsDTO> addressDetails) {
    List<M2pAddressDetailsDTO> m2pAddressDetailsList = new ArrayList<>();
    if (Objects.isNull(addressDetails)) {
      return m2pAddressDetailsList;
    }
    for (AddressDetailsDTO address : addressDetails) {
      m2pAddressDetailsList.add(
          M2pAddressDetailsDTO.builder()
              .addressType(getAddressTypeNames(address.getAddressType()))
              .addressLineOne(address.getAddressLineOne())
              .addressLineTwo(address.getAddressLineTwo())
              .landmark(address.getLandmark())
              .postalCode(address.getPostalCode())
              .ownershipType(address.getOwnershipType())
              .build());
    }
    return m2pAddressDetailsList;
  }

  private static M2pClientDetailsDTO getM2pClientDetailsDTO(
      ClientDetailsDTO clientDetails, String officeName) {
    if (Objects.isNull(clientDetails)) {
      return null;
    }
    return M2pClientDetailsDTO.builder()
        .firstName(clientDetails.getFirstName())
        .middleName(clientDetails.getMiddleName())
        .lastName(clientDetails.getLastName())
        .gender(
            Objects.isNull(clientDetails.getGender())
                ? null
                : clientDetails.getGender().getDisplayName())
        .dateOfBirth(clientDetails.getDateOfBirth())
        .email(clientDetails.getEmail())
        .mobileNo(clientDetails.getMobileNo())
        .alternateMobileNo(clientDetails.getAlternateMobileNo())
        .officeName(officeName)
        .education(clientDetails.getEducation())
        .externalId(clientDetails.getExternalId())
        .build();
  }

  public static M2pDedupeRequest getM2pDedupeRequest(String panValue) {
    return M2pDedupeRequest.builder()
        .clientData(M2pDedupeRequest.ClientData.builder().mobileNo("").build())
        .clientIdentifierData(
            List.of(
                M2pDedupeRequest.ClientIdentifierData.builder()
                    .documentTypeId(45)
                    .documentKey(panValue)
                    .build()))
        .build();
  }

  public static void populateRepaymentScheduleRequest(
      RepaymentScheduleRequest request,
      M2pProductDetailsResponseDTO productDetails,
      String leadId) {

    request.setClientId(leadId);
    request.setProductId(productDetails.getLosLoanProductMapping().getLoanProductId());
    request.setOfficeId(productDetails.getLosProductOfficeMapping().get(0).getOfficeId());
    request.setLocale(EN);
    request.setDateFormat(DD_MMMM_YYYY);
    request.setLoanType(INDIVIDUAL);
    request.setExternalId("");
    request.setDisbursementData(new ArrayList<>());
    request.setRepeatsOnDayOfMonth(new ArrayList<>());
    request.setUserOverriddenTerms(new ArrayList<>());
    request.setGraceOnPrincipalPayment(0);
    request.setRepaymentFrequencyDayOfWeekType("");
    request.setIsTopup(false);
    if (request.getCharges() == null) {
      request.setCharges(new ArrayList<>());
    }
    request.setExpectedDisbursementDate(
        DateTimeConverterUtil.convertToGivenDateFormat(
            request.getExpectedDisbursementDate(), DD_MM_YYYY, DD_MMMM_YYYY));
    request.setSubmittedOnDate(
        DateTimeConverterUtil.convertToGivenDateFormat(
            request.getSubmittedOnDate(), DD_MM_YYYY, DD_MMMM_YYYY));
    request.setRepaymentsStartingFromDate(
        DateTimeConverterUtil.convertToGivenDateFormat(
            request.getRepaymentsStartingFromDate(), DD_MM_YYYY, DD_MMMM_YYYY));
  }

  private static List<M2pAdditionalDetailsDTO> getAdditionalRequestOfUpdateLead(String pan) {
    String ucicValue = generateUcic(pan);
    List<M2pAdditionalDetailsDTO> m2pAdditionalDetailsList = new ArrayList<>();
    m2pAdditionalDetailsList.add(
        M2pAdditionalDetailsDTO.builder()
            .appTable("m_client")
            .dataTableName("ucic")
            .ucic(ucicValue)
            .build());
    return m2pAdditionalDetailsList;
  }

  public static M2pLeadUpdateDTO getM2pLeadUpdateRequest(String pan) {
    return M2pLeadUpdateDTO.builder()
        .locale("en")
        .dateFormat(DATE_FORMAT)
        .additionalDetails(getAdditionalRequestOfUpdateLead(pan))
        .build();
  }

  private static List<M2pAdditionalDetailsDTO> getUpdateM2pAdditionalDetailsDTO(
      List<AdditionalDetailsDTO> additionalDetails) {

    List<M2pAdditionalDetailsDTO> m2pAdditionalDetailsList = new ArrayList<>();

    if (Objects.isNull(additionalDetails) || additionalDetails.isEmpty()) {
      return m2pAdditionalDetailsList;
    }
    for (AdditionalDetailsDTO detail : additionalDetails) {
      m2pAdditionalDetailsList.add(
          M2pAdditionalDetailsDTO.builder()
              .dataTableName(detail.getDataTableName())
              .appTable(detail.getAppTable())
              .mPin(detail.getMPin())
              .subIndustry(detail.getSubIndustry())
              .industry(detail.getIndustry())
              .businessType(detail.getBusinessType())
              .businessAddress(detail.getBusinessAddress())
              .businessAddressType(detail.getBusinessAddressType())
              .businessName(detail.getBusinessName())
              .state(detail.getState())
              .city(detail.getCity())
              .country(detail.getCountry())
              .postalCode(detail.getPostalCode())
              .vernacularPreference(detail.getVernacularPreference())
              .vcipStatus(detail.getVcipStatus())
              .vcipRejectionReason(detail.getVcipRejectionReason())
              .okycTimeStamp(detail.getOkycTimeStamp())
              .gstNumber(detail.getGstNumber())
              .businessDocument(detail.getBusinessDocument())
              .legalName(detail.getLegalName())
              .tradeName(detail.getTradeName())
              .bankBeneName(detail.getBankBeneName())
              .build());
    }
    return m2pAdditionalDetailsList;
  }

  public static M2pAdditionalDetailsDTO prepareMerchantAdditionDto(
      M2pLeadRequestDTO leadRequestDTO) {

    M2pAdditionalDetailsDTO requestDetails =
        leadRequestDTO.getAdditionalDetails().stream()
            .filter(dto -> "merchant_details".equals(dto.getDataTableName()))
            .findFirst()
            .orElse(null);

    M2pAdditionalDetailsDTO updatedDetails =
        M2pAdditionalDetailsDTO.builder()
            .dataTableName(check(requestDetails.getDataTableName()))
            .subIndustry(requestDetails.getSubIndustry())
            .industry(requestDetails.getIndustry())
            .businessType(requestDetails.getBusinessType())
            .businessAddress(requestDetails.getBusinessAddress())
            .state(requestDetails.getState())
            .city(requestDetails.getCity())
            .country(requestDetails.getCountry())
            .postalCode(requestDetails.getPostalCode())
            .businessAddressType(requestDetails.getBusinessAddressType())
            .businessName(requestDetails.getBusinessName())
            .gstNumber(check(requestDetails.getGstNumber()))
            .businessDocument(check(requestDetails.getBusinessDocument()))
            .legalName(check(requestDetails.getLegalName()))
            .tradeName(check(requestDetails.getTradeName()))
            .bankBeneName(check(requestDetails.getBankBeneName()))
            .udyamNumber(check((requestDetails.getUdyamNumber())))
            .build();

    if (!hasAnyValue(updatedDetails)) {
      return null;
    }
    return updatedDetails;
  }

  public static String extractFieldFromFamilyDetails(
      Json jsonNode, String targetRelationship, String targetField) {
    if (jsonNode == null) return StringUtils.EMPTY;

    try {
      JsonNode root = objectMapper.readTree(jsonNode.asString());

      if (root.isArray()) {
        for (JsonNode member : root) {
          String currentRelationship = member.path("relationship").asText();

          if (targetRelationship.equalsIgnoreCase(currentRelationship)) {
            return member.path(targetField).asText(StringUtils.EMPTY);
          }
        }
      }
    } catch (JsonProcessingException e) {
      log.error(
          "[ERROR][LEAD_UTIL][FAMILY_DETAILS] failed to parse familyDetails JSONB for relationship:"
              + " {}",
          targetRelationship,
          e);
    } catch (Exception e) {
      log.error(
          "[ERROR][LEAD_UTIL][FAMILY_DETAILS] unexpected error during familyDetails extraction from"
              + " JSONB",
          e);
    }
    return StringUtils.EMPTY;
  }

  public static String extractAddressField(
      Json jsonSource, String addressType, String targetField) {
    if (jsonSource == null || StringUtils.isAnyBlank(addressType, targetField)) {
      return StringUtils.EMPTY;
    }

    try {
      List<Map<String, Object>> dataList = JsonUtils.parseJsonToList(jsonSource);

      if (dataList == null || dataList.isEmpty()) {
        return StringUtils.EMPTY;
      }

      for (Map<String, Object> item : dataList) {
        if (item == null) continue;

        Object valObj = item.get("addressType");
        boolean isMatch = false;

        if (valObj instanceof List<?> list) {
          for (Object o : list) {
            if (addressType.equalsIgnoreCase(String.valueOf(o))) {
              isMatch = true;
              break;
            }
          }
        } else if (valObj != null) {
          isMatch = addressType.equalsIgnoreCase(String.valueOf(valObj));
        }

        if (isMatch) {
          Object result = item.get(targetField);
          return result == null ? StringUtils.EMPTY : String.valueOf(result);
        }
      }
    } catch (Exception e) {
      log.warn(
          "[ERROR][LEAD_UTIL][ADDRESS] Address extraction failed for field {} where type={}. Error:"
              + " {}",
          targetField,
          addressType,
          e.getMessage());
    }
    return StringUtils.EMPTY;
  }

  public static String extractPanFromClientIdentifierDetails(Json jsonNode) {
    if (jsonNode == null) return StringUtils.EMPTY;

    try {
      JsonNode root = objectMapper.readTree(jsonNode.asString());

      if (root != null && root.isArray()) {
        for (JsonNode identifier : root) {
          if (!identifier.isNull()
              && "PAN".equalsIgnoreCase(identifier.path("documentType").asText())) {

            return identifier.path("documentKey").asText(StringUtils.EMPTY);
          }
        }
      }
    } catch (JsonProcessingException e) {
      log.error(
          "[ERROR][LEAD_UTIL][PAN] Failed to parse clientIdentifierDetails JSONB for PAN"
              + " extraction",
          e);
    } catch (Exception e) {
      log.error("[ERROR][LEAD_UTIL][PAN] Unexpected error during PAN extraction from JSONB", e);
    }

    return StringUtils.EMPTY;
  }

  private static String check(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }

  private static boolean hasAnyValue(M2pAdditionalDetailsDTO dto) {
    return Stream.of(
            dto.getGstNumber(),
            dto.getBusinessDocument(),
            dto.getLegalName(),
            dto.getTradeName(),
            dto.getBankBeneName(),
            dto.getUdyamNumber(),
            dto.getDateOfIncorporation(),
            dto.getCategoryOfBusiness())
        .anyMatch(Objects::nonNull);
  }

  public static Mono<AadhaarXmlDetailsDTO> extractXmlFromBase64(String base64Xml) {
    return Mono.defer(
        () -> {
          String decodedXml;
          try {
            decodedXml = base64ToXmlDecoder(base64Xml);
          } catch (Exception e) {
            return Mono.error(new RuntimeException("failed to decode base64 xml", e));
          }
          return parseXml(decodedXml);
        });
  }

  public static String base64ToXmlDecoder(String base64) {
    byte[] decodedBytes = Base64.getDecoder().decode(base64);

    return new String(decodedBytes, StandardCharsets.UTF_8);
  }

  public static Mono<AadhaarXmlDetailsDTO> parseXml(String rawXml) {
    return Mono.defer(
        () -> {
          DocumentBuilder builder;
          try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
          } catch (ParserConfigurationException e) {
            return Mono.error(new RuntimeException("xml parser configuration failed", e));
          }

          Document doc;
          try {
            doc = builder.parse(new ByteArrayInputStream(rawXml.getBytes(StandardCharsets.UTF_8)));
          } catch (SAXException | IOException e) {
            return Mono.error(new RuntimeException("error parsing xml", e));
          }

          XPath xpath = XPathFactory.newInstance().newXPath();

          try {
            /*
            Handling careOf and landmark differently in-case of Okyc.
            */
            String careOf = xpath.evaluate("//@co", doc);
            careOf =
                (careOf == null || careOf.isEmpty()) ? xpath.evaluate("//@careof", doc) : careOf;

            String landmark = xpath.evaluate("//@lm", doc);
            landmark =
                (landmark == null || landmark.isEmpty())
                    ? xpath.evaluate("//@landmark", doc)
                    : landmark;

            return Mono.just(
                AadhaarXmlDetailsDTO.builder()
                    .careOf(careOf)
                    .country(xpath.evaluate("//@country", doc))
                    .district(xpath.evaluate("//@dist", doc))
                    .pincode(xpath.evaluate("//@pc", doc))
                    .state(xpath.evaluate("//@state", doc))
                    .vtc(xpath.evaluate("//@vtc", doc))
                    .house(xpath.evaluate("//@house", doc))
                    .street(xpath.evaluate("//@street", doc))
                    .locality(xpath.evaluate("//@loc", doc))
                    .subdistrict(xpath.evaluate("//@subdist", doc))
                    .name(xpath.evaluate("//@name", doc))
                    .dob(xpath.evaluate("//@dob", doc))
                    .dependent(xpath.evaluate("//@co", doc))
                    .landmark(landmark)
                    .photoBase64(xpath.evaluate("//Pht", doc))
                    .ts(xpath.evaluate("//@ts", doc))
                    .build());
          } catch (XPathExpressionException e) {
            return Mono.error(new RuntimeException("xpath evaluation failed", e));
          }
        });
  }
}
