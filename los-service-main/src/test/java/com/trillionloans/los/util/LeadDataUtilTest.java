package com.trillionloans.los.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.los.constant.AddressType;
import com.trillionloans.los.constant.Gender;
import com.trillionloans.los.constant.RelationshipType;
import com.trillionloans.los.model.dto.AdditionalDetailsDTO;
import com.trillionloans.los.model.dto.AddressDetailsDTO;
import com.trillionloans.los.model.dto.BankDetailsDTO;
import com.trillionloans.los.model.dto.ClientDetailsDTO;
import com.trillionloans.los.model.dto.ClientDetailsUpdateDTO;
import com.trillionloans.los.model.dto.ClientIdentifierDetailsDTO;
import com.trillionloans.los.model.dto.EmploymentDetailsDTO;
import com.trillionloans.los.model.dto.EmploymentDetailsUpdateDTO;
import com.trillionloans.los.model.dto.FamilyDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pClientDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pClientDetailsUpdateDTO;
import com.trillionloans.los.model.partner.m2p.M2pClientIdentifierDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pEmploymentDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadUpdateDTO;
import com.trillionloans.los.model.request.Lead;
import com.trillionloans.los.model.request.LeadUpdate;
import com.trillionloans.los.model.request.m2p.M2pDedupeRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeadDataUtilTest {
  /** Method under test: {@link LeadDataUtil#getM2pLeadRequest(Lead,String)} */
  @Test
  void testGetM2pLeadRequest() {
    M2pLeadRequestDTO actualM2pLeadRequest = LeadDataUtil.getM2pLeadRequest(new Lead(), "");

    assertEquals("dd-MM-yyyy", actualM2pLeadRequest.getDateFormat());
    assertEquals("en", actualM2pLeadRequest.getLocale());
    assertNull(actualM2pLeadRequest.getClientData());
    assertNull(actualM2pLeadRequest.getEmploymentDetailData());
    assertEquals(1, actualM2pLeadRequest.getAdditionalDetails().size());
    assertTrue(actualM2pLeadRequest.getAddressData().isEmpty());
    assertTrue(actualM2pLeadRequest.getBankDetailsData().isEmpty());
    assertTrue(actualM2pLeadRequest.getClientIdentifierData().isEmpty());
    assertTrue(actualM2pLeadRequest.getFamilyDetailsData().isEmpty());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadRequest(Lead,String)} */
  @Test
  void testGetM2pLeadRequest2() {
    Lead.LeadBuilder builderResult = Lead.builder();
    ArrayList<BankDetailsDTO> bankDetails = new ArrayList<>();
    Lead.LeadBuilder bankDetailsResult = builderResult.bankDetails(bankDetails);
    ClientDetailsDTO clientDetails =
        ClientDetailsDTO.builder()
            .alternateMobileNo("Alternate Mobile No")
            .dateOfBirth("2020-03-01")
            .education("Education")
            .email("jane.doe@example.org")
            .externalId("42")
            .firstName("Jane")
            .gender(Gender.MALE)
            .lastName("Doe")
            .middleName("Middle Name")
            .mobileNo("Mobile No")
            .build();
    Lead.LeadBuilder clientDetailsResult = bankDetailsResult.clientDetails(clientDetails);
    Lead.LeadBuilder clientIdentifierDetailsResult =
        clientDetailsResult.clientIdentifierDetails(new ArrayList<>());
    EmploymentDetailsDTO employmentDetails =
        EmploymentDetailsDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .existingIncomeObligation("Existing Income Obligation")
            .monthlySalary(10.0d)
            .occupationType("Occupation Type")
            .totalWorkExperience(1)
            .build();
    Lead.LeadBuilder employmentDetailsResult =
        clientIdentifierDetailsResult.employmentDetails(employmentDetails);
    Lead leadData = employmentDetailsResult.familyDetails(new ArrayList<>()).build();

    M2pLeadRequestDTO actualM2pLeadRequest =
        LeadDataUtil.getM2pLeadRequest(leadData, "Head Office");

    M2pClientDetailsDTO clientData = actualM2pLeadRequest.getClientData();
    assertEquals("2020-03-01", clientData.getDateOfBirth());
    assertEquals("42", clientData.getExternalId());
    assertEquals("Alternate Mobile No", clientData.getAlternateMobileNo());
    M2pEmploymentDetailsDTO employmentDetailData = actualM2pLeadRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    assertEquals("Doe", clientData.getLastName());
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("Existing Income Obligation", employmentDetailData.getExistingIncomeObligation());
    assertEquals("Head Office", clientData.getOfficeName());
    assertEquals("Jane", clientData.getFirstName());
    assertEquals("Male", clientData.getGender());
    assertEquals("Middle Name", clientData.getMiddleName());
    assertEquals("Mobile No", clientData.getMobileNo());
    assertEquals("Occupation Type", employmentDetailData.getOccupationType());
    assertEquals("dd-MM-yyyy", actualM2pLeadRequest.getDateFormat());
    assertEquals("en", actualM2pLeadRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadRequest.getAdditionalDetails().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(bankDetails, actualM2pLeadRequest.getAddressData());
    assertEquals(bankDetails, actualM2pLeadRequest.getBankDetailsData());
    assertEquals(bankDetails, actualM2pLeadRequest.getClientIdentifierData());
    assertEquals(bankDetails, actualM2pLeadRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadRequest(Lead,String)} */
  @Test
  void testGetM2pLeadRequest3() {
    ClientDetailsDTO clientDetails =
        ClientDetailsDTO.builder()
            .alternateMobileNo("Alternate Mobile No")
            .dateOfBirth("2020-03-01")
            .education("Education")
            .email("jane.doe@example.org")
            .externalId("42")
            .firstName("Jane")
            .gender(Gender.MALE)
            .lastName("Doe")
            .middleName("Middle Name")
            .mobileNo("Mobile No")
            .build();
    ArrayList<AddressDetailsDTO> addressDetails = new ArrayList<>();
    ArrayList<FamilyDetailsDTO> familyDetails = new ArrayList<>();
    ArrayList<ClientIdentifierDetailsDTO> clientIdentifierDetails = new ArrayList<>();
    ArrayList<BankDetailsDTO> bankDetails = new ArrayList<>();
    EmploymentDetailsDTO employmentDetails =
        EmploymentDetailsDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .existingIncomeObligation("Existing Income Obligation")
            .monthlySalary(10.0d)
            .occupationType("Occupation Type")
            .totalWorkExperience(1)
            .build();

    M2pLeadRequestDTO actualM2pLeadRequest =
        LeadDataUtil.getM2pLeadRequest(
            new Lead(
                clientDetails,
                addressDetails,
                familyDetails,
                clientIdentifierDetails,
                bankDetails,
                employmentDetails,
                new ArrayList<>()),
            "Head Office");

    M2pClientDetailsDTO clientData = actualM2pLeadRequest.getClientData();
    assertEquals("2020-03-01", clientData.getDateOfBirth());
    assertEquals("42", clientData.getExternalId());
    assertEquals("Alternate Mobile No", clientData.getAlternateMobileNo());
    M2pEmploymentDetailsDTO employmentDetailData = actualM2pLeadRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    assertEquals("Doe", clientData.getLastName());
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("Existing Income Obligation", employmentDetailData.getExistingIncomeObligation());
    assertEquals("Head Office", clientData.getOfficeName());
    assertEquals("Jane", clientData.getFirstName());
    assertEquals("Male", clientData.getGender());
    assertEquals("Middle Name", clientData.getMiddleName());
    assertEquals("Mobile No", clientData.getMobileNo());
    assertEquals("Occupation Type", employmentDetailData.getOccupationType());
    assertEquals("dd-MM-yyyy", actualM2pLeadRequest.getDateFormat());
    assertEquals("en", actualM2pLeadRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadRequest.getAdditionalDetails().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(addressDetails, actualM2pLeadRequest.getAddressData());
    assertEquals(addressDetails, actualM2pLeadRequest.getBankDetailsData());
    assertEquals(addressDetails, actualM2pLeadRequest.getClientIdentifierData());
    assertEquals(addressDetails, actualM2pLeadRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadRequest(Lead,String)} */
  @Test
  void testGetM2pLeadRequest4() {
    Lead leadData = mock(Lead.class);
    ClientDetailsDTO buildResult =
        ClientDetailsDTO.builder()
            .alternateMobileNo("Alternate Mobile No")
            .dateOfBirth("2020-03-01")
            .education("Education")
            .email("jane.doe@example.org")
            .externalId("42")
            .firstName("Jane")
            .gender(Gender.MALE)
            .lastName("Doe")
            .middleName("Middle Name")
            .mobileNo("Mobile No")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult);
    EmploymentDetailsDTO buildResult2 =
        EmploymentDetailsDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .existingIncomeObligation("Existing Income Obligation")
            .monthlySalary(10.0d)
            .occupationType("Occupation Type")
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult2);
    ArrayList<AdditionalDetailsDTO> additionalDetailsDTOList = new ArrayList<>();
    when(leadData.getAdditionalDetails()).thenReturn(additionalDetailsDTOList);
    when(leadData.getAddressDetails()).thenReturn(new ArrayList<>());
    when(leadData.getBankDetails()).thenReturn(new ArrayList<>());
    when(leadData.getClientIdentifierDetails()).thenReturn(new ArrayList<>());
    when(leadData.getFamilyDetails()).thenReturn(new ArrayList<>());

    M2pLeadRequestDTO actualM2pLeadRequest =
        LeadDataUtil.getM2pLeadRequest(leadData, "Head Office");

    verify(leadData).getAdditionalDetails();
    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData, atLeast(1)).getClientIdentifierDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pClientDetailsDTO clientData = actualM2pLeadRequest.getClientData();
    assertEquals("2020-03-01", clientData.getDateOfBirth());
    assertEquals("42", clientData.getExternalId());
    assertEquals("Alternate Mobile No", clientData.getAlternateMobileNo());
    M2pEmploymentDetailsDTO employmentDetailData = actualM2pLeadRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    assertEquals("Doe", clientData.getLastName());
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("Existing Income Obligation", employmentDetailData.getExistingIncomeObligation());
    assertEquals("Head Office", clientData.getOfficeName());
    assertEquals("Jane", clientData.getFirstName());
    assertEquals("Male", clientData.getGender());
    assertEquals("Middle Name", clientData.getMiddleName());
    assertEquals("Mobile No", clientData.getMobileNo());
    assertEquals("Occupation Type", employmentDetailData.getOccupationType());
    assertEquals("dd-MM-yyyy", actualM2pLeadRequest.getDateFormat());
    assertEquals("en", actualM2pLeadRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadRequest.getAdditionalDetails().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getAddressData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getBankDetailsData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getClientIdentifierData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadRequest(Lead,String)} */
  @Test
  void testGetM2pLeadRequest5() {
    // Arrange
    ArrayList<AdditionalDetailsDTO> additionalDetailsDTOList = new ArrayList<>();
    AdditionalDetailsDTO buildResult =
        AdditionalDetailsDTO.builder()
            .appTable("App Table")
            .businessAddress("42 Main St")
            .businessAddressType("42 Main St")
            .businessType("Business Type")
            .city("Oxford")
            .country("GB")
            .dataTableName("Data Table Name")
            .industry("Industry")
            .mPin("M Pin")
            .postalCode(1)
            .state("MD")
            .subIndustry("Sub Industry")
            .build();
    additionalDetailsDTOList.add(buildResult);
    Lead leadData = mock(Lead.class);
    ClientDetailsDTO buildResult2 =
        ClientDetailsDTO.builder()
            .alternateMobileNo("Alternate Mobile No")
            .dateOfBirth("2020-03-01")
            .education("Education")
            .email("jane.doe@example.org")
            .externalId("42")
            .firstName("Jane")
            .gender(Gender.MALE)
            .lastName("Doe")
            .middleName("Middle Name")
            .mobileNo("Mobile No")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult2);
    EmploymentDetailsDTO buildResult3 =
        EmploymentDetailsDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .existingIncomeObligation("Existing Income Obligation")
            .monthlySalary(10.0d)
            .occupationType("Occupation Type")
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult3);
    when(leadData.getAdditionalDetails()).thenReturn(additionalDetailsDTOList);
    ArrayList<AddressDetailsDTO> addressDetailsDTOList = new ArrayList<>();
    when(leadData.getAddressDetails()).thenReturn(addressDetailsDTOList);
    when(leadData.getBankDetails()).thenReturn(new ArrayList<>());
    when(leadData.getClientIdentifierDetails()).thenReturn(new ArrayList<>());
    when(leadData.getFamilyDetails()).thenReturn(new ArrayList<>());

    // Act
    M2pLeadRequestDTO actualM2pLeadRequest =
        LeadDataUtil.getM2pLeadRequest(leadData, "Head Office");

    // Assert
    verify(leadData).getAdditionalDetails();
    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData, atLeast(1)).getClientIdentifierDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pClientDetailsDTO clientData = actualM2pLeadRequest.getClientData();
    assertEquals("2020-03-01", clientData.getDateOfBirth());
    assertEquals("42", clientData.getExternalId());
    assertEquals("Alternate Mobile No", clientData.getAlternateMobileNo());
    M2pEmploymentDetailsDTO employmentDetailData = actualM2pLeadRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    assertEquals("Doe", clientData.getLastName());
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("Existing Income Obligation", employmentDetailData.getExistingIncomeObligation());
    assertEquals("Head Office", clientData.getOfficeName());
    assertEquals("Jane", clientData.getFirstName());
    assertEquals("Male", clientData.getGender());
    assertEquals("Middle Name", clientData.getMiddleName());
    assertEquals("Mobile No", clientData.getMobileNo());
    assertEquals("Occupation Type", employmentDetailData.getOccupationType());
    assertEquals("dd-MM-yyyy", actualM2pLeadRequest.getDateFormat());
    assertEquals("en", actualM2pLeadRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(2, actualM2pLeadRequest.getAdditionalDetails().size());
    assertEquals(addressDetailsDTOList, actualM2pLeadRequest.getAddressData());
    assertEquals(addressDetailsDTOList, actualM2pLeadRequest.getBankDetailsData());
    assertEquals(addressDetailsDTOList, actualM2pLeadRequest.getClientIdentifierData());
    assertEquals(addressDetailsDTOList, actualM2pLeadRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadRequest(Lead,String)} */
  @Test
  void testGetM2pLeadRequest6() {
    // Arrange
    ArrayList<AddressDetailsDTO> addressDetailsDTOList = new ArrayList<>();
    AddressDetailsDTO buildResult =
        AddressDetailsDTO.builder()
            .landmark("Landmark")
            .ownershipType("Ownership Type")
            .postalCode("Postal Code")
            .build();
    addressDetailsDTOList.add(buildResult);
    Lead leadData = mock(Lead.class);
    ClientDetailsDTO buildResult2 =
        ClientDetailsDTO.builder()
            .alternateMobileNo("Alternate Mobile No")
            .dateOfBirth("2020-03-01")
            .education("Education")
            .email("jane.doe@example.org")
            .externalId("42")
            .firstName("Jane")
            .gender(Gender.MALE)
            .lastName("Doe")
            .middleName("Middle Name")
            .mobileNo("Mobile No")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult2);
    EmploymentDetailsDTO buildResult3 =
        EmploymentDetailsDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .existingIncomeObligation("Existing Income Obligation")
            .monthlySalary(10.0d)
            .occupationType("Occupation Type")
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult3);
    ArrayList<AdditionalDetailsDTO> additionalDetailsDTOList = new ArrayList<>();
    when(leadData.getAdditionalDetails()).thenReturn(additionalDetailsDTOList);
    when(leadData.getAddressDetails()).thenReturn(addressDetailsDTOList);
    when(leadData.getBankDetails()).thenReturn(new ArrayList<>());
    when(leadData.getClientIdentifierDetails()).thenReturn(new ArrayList<>());
    when(leadData.getFamilyDetails()).thenReturn(new ArrayList<>());

    M2pLeadRequestDTO actualM2pLeadRequest =
        LeadDataUtil.getM2pLeadRequest(leadData, "Head Office");

    verify(leadData).getAdditionalDetails();
    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData, atLeast(1)).getClientIdentifierDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pClientDetailsDTO clientData = actualM2pLeadRequest.getClientData();
    assertEquals("2020-03-01", clientData.getDateOfBirth());
    assertEquals("42", clientData.getExternalId());
    assertEquals("Alternate Mobile No", clientData.getAlternateMobileNo());
    M2pEmploymentDetailsDTO employmentDetailData = actualM2pLeadRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    assertEquals("Doe", clientData.getLastName());
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("Existing Income Obligation", employmentDetailData.getExistingIncomeObligation());
    assertEquals("Head Office", clientData.getOfficeName());
    assertEquals("Jane", clientData.getFirstName());
    assertEquals("Male", clientData.getGender());
    assertEquals("Middle Name", clientData.getMiddleName());
    assertEquals("Mobile No", clientData.getMobileNo());
    assertEquals("Occupation Type", employmentDetailData.getOccupationType());
    assertEquals("dd-MM-yyyy", actualM2pLeadRequest.getDateFormat());
    assertEquals("en", actualM2pLeadRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadRequest.getAdditionalDetails().size());
    assertEquals(1, actualM2pLeadRequest.getAddressData().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getBankDetailsData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getClientIdentifierData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadRequest(Lead,String)} */
  @Test
  void testGetM2pLeadRequest7() {
    // Arrange
    ArrayList<BankDetailsDTO> bankDetailsDTOList = new ArrayList<>();
    BankDetailsDTO buildResult =
        BankDetailsDTO.builder()
            .accountNumber("42")
            .accountType("3")
            .ifscCode("Ifsc Code")
            .name("Name")
            .supportedForDisbursement(true)
            .supportedForRepayment(true)
            .build();
    bankDetailsDTOList.add(buildResult);
    Lead leadData = mock(Lead.class);
    ClientDetailsDTO buildResult2 =
        ClientDetailsDTO.builder()
            .alternateMobileNo("Alternate Mobile No")
            .dateOfBirth("2020-03-01")
            .education("Education")
            .email("jane.doe@example.org")
            .externalId("42")
            .firstName("Jane")
            .gender(Gender.MALE)
            .lastName("Doe")
            .middleName("Middle Name")
            .mobileNo("Mobile No")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult2);
    EmploymentDetailsDTO buildResult3 =
        EmploymentDetailsDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .existingIncomeObligation("Existing Income Obligation")
            .monthlySalary(10.0d)
            .occupationType("Occupation Type")
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult3);
    ArrayList<AdditionalDetailsDTO> additionalDetailsDTOList = new ArrayList<>();
    when(leadData.getAdditionalDetails()).thenReturn(additionalDetailsDTOList);
    when(leadData.getAddressDetails()).thenReturn(new ArrayList<>());
    when(leadData.getBankDetails()).thenReturn(bankDetailsDTOList);
    when(leadData.getClientIdentifierDetails()).thenReturn(new ArrayList<>());
    when(leadData.getFamilyDetails()).thenReturn(new ArrayList<>());

    M2pLeadRequestDTO actualM2pLeadRequest =
        LeadDataUtil.getM2pLeadRequest(leadData, "Head Office");

    verify(leadData).getAdditionalDetails();
    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData, atLeast(1)).getClientIdentifierDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pClientDetailsDTO clientData = actualM2pLeadRequest.getClientData();
    assertEquals("2020-03-01", clientData.getDateOfBirth());
    assertEquals("42", clientData.getExternalId());
    assertEquals("Alternate Mobile No", clientData.getAlternateMobileNo());
    M2pEmploymentDetailsDTO employmentDetailData = actualM2pLeadRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    assertEquals("Doe", clientData.getLastName());
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("Existing Income Obligation", employmentDetailData.getExistingIncomeObligation());
    assertEquals("Head Office", clientData.getOfficeName());
    assertEquals("Jane", clientData.getFirstName());
    assertEquals("Male", clientData.getGender());
    assertEquals("Middle Name", clientData.getMiddleName());
    assertEquals("Mobile No", clientData.getMobileNo());
    assertEquals("Occupation Type", employmentDetailData.getOccupationType());
    assertEquals("dd-MM-yyyy", actualM2pLeadRequest.getDateFormat());
    assertEquals("en", actualM2pLeadRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadRequest.getAdditionalDetails().size());
    assertEquals(1, actualM2pLeadRequest.getBankDetailsData().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getAddressData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getClientIdentifierData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadRequest(Lead,String)} */
  @Test
  void testGetM2pLeadRequest8() {
    ArrayList<ClientIdentifierDetailsDTO> clientIdentifierDetailsDTOList = new ArrayList<>();
    ClientIdentifierDetailsDTO buildResult =
        ClientIdentifierDetailsDTO.builder()
            .documentKey("Document Key")
            .documentType("Document Type")
            .expiryDate("2020-03-01")
            .build();
    clientIdentifierDetailsDTOList.add(buildResult);
    Lead leadData = mock(Lead.class);
    ClientDetailsDTO buildResult2 =
        ClientDetailsDTO.builder()
            .alternateMobileNo("Alternate Mobile No")
            .dateOfBirth("2020-03-01")
            .education("Education")
            .email("jane.doe@example.org")
            .externalId("42")
            .firstName("Jane")
            .gender(Gender.MALE)
            .lastName("Doe")
            .middleName("Middle Name")
            .mobileNo("Mobile No")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult2);
    EmploymentDetailsDTO buildResult3 =
        EmploymentDetailsDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .existingIncomeObligation("Existing Income Obligation")
            .monthlySalary(10.0d)
            .occupationType("Occupation Type")
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult3);
    ArrayList<AdditionalDetailsDTO> additionalDetailsDTOList = new ArrayList<>();
    when(leadData.getAdditionalDetails()).thenReturn(additionalDetailsDTOList);
    when(leadData.getAddressDetails()).thenReturn(new ArrayList<>());
    when(leadData.getBankDetails()).thenReturn(new ArrayList<>());
    when(leadData.getClientIdentifierDetails()).thenReturn(clientIdentifierDetailsDTOList);
    when(leadData.getFamilyDetails()).thenReturn(new ArrayList<>());

    M2pLeadRequestDTO actualM2pLeadRequest =
        LeadDataUtil.getM2pLeadRequest(leadData, "Head Office");

    verify(leadData).getAdditionalDetails();
    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData, atLeast(1)).getClientIdentifierDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pClientDetailsDTO clientData = actualM2pLeadRequest.getClientData();
    assertEquals("2020-03-01", clientData.getDateOfBirth());
    List<M2pClientIdentifierDetailsDTO> clientIdentifierData =
        actualM2pLeadRequest.getClientIdentifierData();
    assertEquals(1, clientIdentifierData.size());
    M2pClientIdentifierDetailsDTO getResult = clientIdentifierData.get(0);
    assertEquals("2020-03-01", getResult.getExpiryDate());
    assertEquals("42", clientData.getExternalId());
    assertEquals("Alternate Mobile No", clientData.getAlternateMobileNo());
    M2pEmploymentDetailsDTO employmentDetailData = actualM2pLeadRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    assertEquals("Document Key", getResult.getDocumentKey());
    assertEquals("Document Type", getResult.getDocumentType());
    assertEquals("Doe", clientData.getLastName());
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("Existing Income Obligation", employmentDetailData.getExistingIncomeObligation());
    assertEquals("Head Office", clientData.getOfficeName());
    assertEquals("Jane", clientData.getFirstName());
    assertEquals("Male", clientData.getGender());
    assertEquals("Middle Name", clientData.getMiddleName());
    assertEquals("Mobile No", clientData.getMobileNo());
    assertEquals("Occupation Type", employmentDetailData.getOccupationType());
    assertEquals("dd-MM-yyyy", actualM2pLeadRequest.getDateFormat());
    assertEquals("en", actualM2pLeadRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertNull(getResult.getIssueDate());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadRequest.getAdditionalDetails().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getAddressData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getBankDetailsData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadRequest(Lead,String)} */
  @Test
  void testGetM2pLeadRequest9() {
    // Arrange
    ArrayList<FamilyDetailsDTO> familyDetailsDTOList = new ArrayList<>();
    FamilyDetailsDTO buildResult =
        FamilyDetailsDTO.builder()
            .dateOfBirth("2020-03-01")
            .documentKey("Document Key")
            .documentType("Document Type")
            .firstName("Jane")
            .gender(Gender.MALE)
            .lastName("Doe")
            .relationship(RelationshipType.FATHER)
            .build();
    familyDetailsDTOList.add(buildResult);
    Lead leadData = mock(Lead.class);
    ClientDetailsDTO buildResult2 =
        ClientDetailsDTO.builder()
            .alternateMobileNo("Alternate Mobile No")
            .dateOfBirth("2020-03-01")
            .education("Education")
            .email("jane.doe@example.org")
            .externalId("42")
            .firstName("Jane")
            .gender(Gender.MALE)
            .lastName("Doe")
            .middleName("Middle Name")
            .mobileNo("Mobile No")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult2);
    EmploymentDetailsDTO buildResult3 =
        EmploymentDetailsDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .existingIncomeObligation("Existing Income Obligation")
            .monthlySalary(10.0d)
            .occupationType("Occupation Type")
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult3);
    ArrayList<AdditionalDetailsDTO> additionalDetailsDTOList = new ArrayList<>();
    when(leadData.getAdditionalDetails()).thenReturn(additionalDetailsDTOList);
    when(leadData.getAddressDetails()).thenReturn(new ArrayList<>());
    when(leadData.getBankDetails()).thenReturn(new ArrayList<>());
    when(leadData.getClientIdentifierDetails()).thenReturn(new ArrayList<>());
    when(leadData.getFamilyDetails()).thenReturn(familyDetailsDTOList);

    M2pLeadRequestDTO actualM2pLeadRequest =
        LeadDataUtil.getM2pLeadRequest(leadData, "Head Office");

    verify(leadData).getAdditionalDetails();
    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData, atLeast(1)).getClientIdentifierDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pClientDetailsDTO clientData = actualM2pLeadRequest.getClientData();
    assertEquals("2020-03-01", clientData.getDateOfBirth());
    assertEquals("42", clientData.getExternalId());
    assertEquals("Alternate Mobile No", clientData.getAlternateMobileNo());
    M2pEmploymentDetailsDTO employmentDetailData = actualM2pLeadRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    assertEquals("Doe", clientData.getLastName());
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("Existing Income Obligation", employmentDetailData.getExistingIncomeObligation());
    assertEquals("Head Office", clientData.getOfficeName());
    assertEquals("Jane", clientData.getFirstName());
    assertEquals("Male", clientData.getGender());
    assertEquals("Middle Name", clientData.getMiddleName());
    assertEquals("Mobile No", clientData.getMobileNo());
    assertEquals("Occupation Type", employmentDetailData.getOccupationType());
    assertEquals("dd-MM-yyyy", actualM2pLeadRequest.getDateFormat());
    assertEquals("en", actualM2pLeadRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadRequest.getAdditionalDetails().size());
    assertEquals(1, actualM2pLeadRequest.getFamilyDetailsData().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getAddressData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getBankDetailsData());
    assertEquals(additionalDetailsDTOList, actualM2pLeadRequest.getClientIdentifierData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadUpdateRequest(LeadUpdate)} */
  @Test
  void testGetM2pLeadUpdateRequest() {
    M2pLeadUpdateDTO actualM2pLeadUpdateRequest =
        LeadDataUtil.getM2pLeadUpdateRequest(new LeadUpdate());

    assertEquals("dd-MM-yyyy", actualM2pLeadUpdateRequest.getDateFormat());
    assertEquals("en", actualM2pLeadUpdateRequest.getLocale());
    assertNull(actualM2pLeadUpdateRequest.getClientData());
    assertNull(actualM2pLeadUpdateRequest.getEmploymentDetailData());
    assertTrue(actualM2pLeadUpdateRequest.getAddressData().isEmpty());
    assertTrue(actualM2pLeadUpdateRequest.getBankDetailsData().isEmpty());
    assertTrue(actualM2pLeadUpdateRequest.getFamilyDetailsData().isEmpty());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadUpdateRequest(LeadUpdate)} */
  @Test
  void testGetM2pLeadUpdateRequest2() {
    // Arrange
    LeadUpdate.LeadUpdateBuilder builderResult = LeadUpdate.builder();
    ArrayList<BankDetailsDTO> bankDetails = new ArrayList<>();
    LeadUpdate.LeadUpdateBuilder bankDetailsResult = builderResult.bankDetails(bankDetails);
    ClientDetailsUpdateDTO clientDetails =
        ClientDetailsUpdateDTO.builder()
            .education("Education")
            .email("jane.doe@example.org")
            .build();
    LeadUpdate.LeadUpdateBuilder clientDetailsResult =
        bankDetailsResult.clientDetails(clientDetails);
    EmploymentDetailsUpdateDTO employmentDetails =
        EmploymentDetailsUpdateDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .monthlySalary(10.0d)
            .totalWorkExperience(1)
            .build();
    LeadUpdate.LeadUpdateBuilder employmentDetailsResult =
        clientDetailsResult.employmentDetails(employmentDetails);
    LeadUpdate leadData = employmentDetailsResult.familyDetails(new ArrayList<>()).build();

    M2pLeadUpdateDTO actualM2pLeadUpdateRequest = LeadDataUtil.getM2pLeadUpdateRequest(leadData);

    M2pEmploymentDetailsDTO employmentDetailData =
        actualM2pLeadUpdateRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    M2pClientDetailsUpdateDTO clientData = actualM2pLeadUpdateRequest.getClientData();
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("dd-MM-yyyy", actualM2pLeadUpdateRequest.getDateFormat());
    assertEquals("en", actualM2pLeadUpdateRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertNull(employmentDetailData.getExistingIncomeObligation());
    assertNull(employmentDetailData.getOccupationType());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(bankDetails, actualM2pLeadUpdateRequest.getAddressData());
    assertEquals(bankDetails, actualM2pLeadUpdateRequest.getBankDetailsData());
    assertEquals(bankDetails, actualM2pLeadUpdateRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadUpdateRequest(LeadUpdate)} */
  @Test
  void testGetM2pLeadUpdateRequest3() {
    ClientDetailsUpdateDTO clientDetails =
        ClientDetailsUpdateDTO.builder()
            .education("Education")
            .email("jane.doe@example.org")
            .build();
    ArrayList<AddressDetailsDTO> addressDetails = new ArrayList<>();
    ArrayList<FamilyDetailsDTO> familyDetails = new ArrayList<>();
    ArrayList<BankDetailsDTO> bankDetails = new ArrayList<>();
    EmploymentDetailsUpdateDTO employmentDetails =
        EmploymentDetailsUpdateDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .monthlySalary(10.0d)
            .totalWorkExperience(1)
            .build();

    M2pLeadUpdateDTO actualM2pLeadUpdateRequest =
        LeadDataUtil.getM2pLeadUpdateRequest(
            new LeadUpdate(
                clientDetails,
                addressDetails,
                familyDetails,
                bankDetails,
                employmentDetails,
                null));

    M2pEmploymentDetailsDTO employmentDetailData =
        actualM2pLeadUpdateRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    M2pClientDetailsUpdateDTO clientData = actualM2pLeadUpdateRequest.getClientData();
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("dd-MM-yyyy", actualM2pLeadUpdateRequest.getDateFormat());
    assertEquals("en", actualM2pLeadUpdateRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertNull(employmentDetailData.getExistingIncomeObligation());
    assertNull(employmentDetailData.getOccupationType());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(addressDetails, actualM2pLeadUpdateRequest.getAddressData());
    assertEquals(addressDetails, actualM2pLeadUpdateRequest.getBankDetailsData());
    assertEquals(addressDetails, actualM2pLeadUpdateRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadUpdateRequest(LeadUpdate)} */
  @Test
  void testGetM2pLeadUpdateRequest4() {
    LeadUpdate leadData = mock(LeadUpdate.class);
    ClientDetailsUpdateDTO buildResult =
        ClientDetailsUpdateDTO.builder()
            .education("Education")
            .email("jane.doe@example.org")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult);
    EmploymentDetailsUpdateDTO buildResult2 =
        EmploymentDetailsUpdateDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .monthlySalary(10.0d)
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult2);
    ArrayList<AddressDetailsDTO> addressDetailsDTOList = new ArrayList<>();
    when(leadData.getAddressDetails()).thenReturn(addressDetailsDTOList);
    when(leadData.getBankDetails()).thenReturn(new ArrayList<>());
    when(leadData.getFamilyDetails()).thenReturn(new ArrayList<>());

    M2pLeadUpdateDTO actualM2pLeadUpdateRequest = LeadDataUtil.getM2pLeadUpdateRequest(leadData);

    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pEmploymentDetailsDTO employmentDetailData =
        actualM2pLeadUpdateRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    M2pClientDetailsUpdateDTO clientData = actualM2pLeadUpdateRequest.getClientData();
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("dd-MM-yyyy", actualM2pLeadUpdateRequest.getDateFormat());
    assertEquals("en", actualM2pLeadUpdateRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertNull(employmentDetailData.getExistingIncomeObligation());
    assertNull(employmentDetailData.getOccupationType());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(addressDetailsDTOList, actualM2pLeadUpdateRequest.getAddressData());
    assertEquals(addressDetailsDTOList, actualM2pLeadUpdateRequest.getBankDetailsData());
    assertEquals(addressDetailsDTOList, actualM2pLeadUpdateRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadUpdateRequest(LeadUpdate)} */
  @Test
  void testGetM2pLeadUpdateRequest5() {
    ArrayList<AddressDetailsDTO> addressDetailsDTOList = new ArrayList<>();
    AddressDetailsDTO buildResult =
        AddressDetailsDTO.builder()
            .landmark("Landmark")
            .ownershipType("Ownership Type")
            .postalCode("Postal Code")
            .build();
    addressDetailsDTOList.add(buildResult);
    LeadUpdate leadData = mock(LeadUpdate.class);
    ClientDetailsUpdateDTO buildResult2 =
        ClientDetailsUpdateDTO.builder()
            .education("Education")
            .email("jane.doe@example.org")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult2);
    EmploymentDetailsUpdateDTO buildResult3 =
        EmploymentDetailsUpdateDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .monthlySalary(10.0d)
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult3);
    when(leadData.getAddressDetails()).thenReturn(addressDetailsDTOList);
    ArrayList<BankDetailsDTO> bankDetailsDTOList = new ArrayList<>();
    when(leadData.getBankDetails()).thenReturn(bankDetailsDTOList);
    when(leadData.getFamilyDetails()).thenReturn(new ArrayList<>());

    M2pLeadUpdateDTO actualM2pLeadUpdateRequest = LeadDataUtil.getM2pLeadUpdateRequest(leadData);

    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pEmploymentDetailsDTO employmentDetailData =
        actualM2pLeadUpdateRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    M2pClientDetailsUpdateDTO clientData = actualM2pLeadUpdateRequest.getClientData();
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("dd-MM-yyyy", actualM2pLeadUpdateRequest.getDateFormat());
    assertEquals("en", actualM2pLeadUpdateRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertNull(employmentDetailData.getExistingIncomeObligation());
    assertNull(employmentDetailData.getOccupationType());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadUpdateRequest.getAddressData().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(bankDetailsDTOList, actualM2pLeadUpdateRequest.getBankDetailsData());
    assertEquals(bankDetailsDTOList, actualM2pLeadUpdateRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadUpdateRequest(LeadUpdate)} */
  @Test
  void testGetM2pLeadUpdateRequest6() {
    ArrayList<BankDetailsDTO> bankDetailsDTOList = new ArrayList<>();
    BankDetailsDTO buildResult =
        BankDetailsDTO.builder()
            .accountNumber("42")
            .accountType("3")
            .ifscCode("Ifsc Code")
            .name("Name")
            .supportedForDisbursement(true)
            .supportedForRepayment(true)
            .build();
    bankDetailsDTOList.add(buildResult);
    LeadUpdate leadData = mock(LeadUpdate.class);
    ClientDetailsUpdateDTO buildResult2 =
        ClientDetailsUpdateDTO.builder()
            .education("Education")
            .email("jane.doe@example.org")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult2);
    EmploymentDetailsUpdateDTO buildResult3 =
        EmploymentDetailsUpdateDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .monthlySalary(10.0d)
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult3);
    ArrayList<AddressDetailsDTO> addressDetailsDTOList = new ArrayList<>();
    when(leadData.getAddressDetails()).thenReturn(addressDetailsDTOList);
    when(leadData.getBankDetails()).thenReturn(bankDetailsDTOList);
    when(leadData.getFamilyDetails()).thenReturn(new ArrayList<>());

    M2pLeadUpdateDTO actualM2pLeadUpdateRequest = LeadDataUtil.getM2pLeadUpdateRequest(leadData);

    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pEmploymentDetailsDTO employmentDetailData =
        actualM2pLeadUpdateRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    M2pClientDetailsUpdateDTO clientData = actualM2pLeadUpdateRequest.getClientData();
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("dd-MM-yyyy", actualM2pLeadUpdateRequest.getDateFormat());
    assertEquals("en", actualM2pLeadUpdateRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertNull(employmentDetailData.getExistingIncomeObligation());
    assertNull(employmentDetailData.getOccupationType());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadUpdateRequest.getBankDetailsData().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(addressDetailsDTOList, actualM2pLeadUpdateRequest.getAddressData());
    assertEquals(addressDetailsDTOList, actualM2pLeadUpdateRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadUpdateRequest(LeadUpdate)} */
  @Test
  void testGetM2pLeadUpdateRequest7() {
    ArrayList<FamilyDetailsDTO> familyDetailsDTOList = new ArrayList<>();
    FamilyDetailsDTO buildResult =
        FamilyDetailsDTO.builder()
            .dateOfBirth("2020-03-01")
            .documentKey("Document Key")
            .documentType("Document Type")
            .firstName("Jane")
            .gender(Gender.MALE)
            .lastName("Doe")
            .relationship(RelationshipType.FATHER)
            .build();
    familyDetailsDTOList.add(buildResult);
    LeadUpdate leadData = mock(LeadUpdate.class);
    ClientDetailsUpdateDTO buildResult2 =
        ClientDetailsUpdateDTO.builder()
            .education("Education")
            .email("jane.doe@example.org")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult2);
    EmploymentDetailsUpdateDTO buildResult3 =
        EmploymentDetailsUpdateDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .monthlySalary(10.0d)
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult3);
    ArrayList<AddressDetailsDTO> addressDetailsDTOList = new ArrayList<>();
    when(leadData.getAddressDetails()).thenReturn(addressDetailsDTOList);
    when(leadData.getBankDetails()).thenReturn(new ArrayList<>());
    when(leadData.getFamilyDetails()).thenReturn(familyDetailsDTOList);

    M2pLeadUpdateDTO actualM2pLeadUpdateRequest = LeadDataUtil.getM2pLeadUpdateRequest(leadData);

    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pEmploymentDetailsDTO employmentDetailData =
        actualM2pLeadUpdateRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    M2pClientDetailsUpdateDTO clientData = actualM2pLeadUpdateRequest.getClientData();
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("dd-MM-yyyy", actualM2pLeadUpdateRequest.getDateFormat());
    assertEquals("en", actualM2pLeadUpdateRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertNull(employmentDetailData.getExistingIncomeObligation());
    assertNull(employmentDetailData.getOccupationType());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadUpdateRequest.getFamilyDetailsData().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(addressDetailsDTOList, actualM2pLeadUpdateRequest.getAddressData());
    assertEquals(addressDetailsDTOList, actualM2pLeadUpdateRequest.getBankDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#getM2pLeadUpdateRequest(LeadUpdate)} */
  @Test
  void testGetM2pLeadUpdateRequest8() {
    AddressDetailsDTO.AddressDetailsDTOBuilder builderResult = AddressDetailsDTO.builder();
    ArrayList<AddressType> addressType = new ArrayList<>();
    builderResult.addressType(addressType);
    AddressDetailsDTO buildResult =
        builderResult
            .landmark("Landmark")
            .ownershipType("Ownership Type")
            .postalCode("Postal Code")
            .build();

    ArrayList<AddressDetailsDTO> addressDetailsDTOList = new ArrayList<>();
    addressDetailsDTOList.add(buildResult);
    LeadUpdate leadData = mock(LeadUpdate.class);
    ClientDetailsUpdateDTO buildResult2 =
        ClientDetailsUpdateDTO.builder()
            .education("Education")
            .email("jane.doe@example.org")
            .build();
    when(leadData.getClientDetails()).thenReturn(buildResult2);
    EmploymentDetailsUpdateDTO buildResult3 =
        EmploymentDetailsUpdateDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .monthlySalary(10.0d)
            .totalWorkExperience(1)
            .build();
    when(leadData.getEmploymentDetails()).thenReturn(buildResult3);
    when(leadData.getAddressDetails()).thenReturn(addressDetailsDTOList);
    when(leadData.getBankDetails()).thenReturn(new ArrayList<>());
    when(leadData.getFamilyDetails()).thenReturn(new ArrayList<>());

    M2pLeadUpdateDTO actualM2pLeadUpdateRequest = LeadDataUtil.getM2pLeadUpdateRequest(leadData);

    verify(leadData).getAddressDetails();
    verify(leadData).getBankDetails();
    verify(leadData).getClientDetails();
    verify(leadData).getEmploymentDetails();
    verify(leadData).getFamilyDetails();
    M2pEmploymentDetailsDTO employmentDetailData =
        actualM2pLeadUpdateRequest.getEmploymentDetailData();
    assertEquals("Company Type", employmentDetailData.getCompanyType());
    assertEquals("Current Employer Name", employmentDetailData.getCurrentEmployerName());
    M2pClientDetailsUpdateDTO clientData = actualM2pLeadUpdateRequest.getClientData();
    assertEquals("Education", clientData.getEducation());
    assertEquals("Employment Type", employmentDetailData.getEmploymentType());
    assertEquals("dd-MM-yyyy", actualM2pLeadUpdateRequest.getDateFormat());
    assertEquals("en", actualM2pLeadUpdateRequest.getLocale());
    assertEquals("jane.doe@example.org", clientData.getEmail());
    assertNull(employmentDetailData.getExistingIncomeObligation());
    assertNull(employmentDetailData.getOccupationType());
    assertEquals(1, employmentDetailData.getTotalWorkExperience().intValue());
    assertEquals(1, actualM2pLeadUpdateRequest.getAddressData().size());
    assertEquals(10.0d, employmentDetailData.getMonthlySalary());
    assertEquals(addressType, actualM2pLeadUpdateRequest.getBankDetailsData());
    assertEquals(addressType, actualM2pLeadUpdateRequest.getFamilyDetailsData());
  }

  /** Method under test: {@link LeadDataUtil#generateUcic(String)} */
  @Test
  void testGenerateUcic() {
    assertEquals("42", LeadDataUtil.generateUcic("42"));
    assertEquals("160114 142113020518", LeadDataUtil.generateUcic("Pan Number"));
  }

  /** Method under test: {@link LeadDataUtil#getM2pDedupeRequest(String)} */
  @Test
  void testGetM2pDedupeRequest() {
    M2pDedupeRequest actualM2pDedupeRequest = LeadDataUtil.getM2pDedupeRequest("42");

    assertEquals("", actualM2pDedupeRequest.getClientData().getMobileNo());
    List<M2pDedupeRequest.ClientIdentifierData> clientIdentifierData =
        actualM2pDedupeRequest.getClientIdentifierData();
    assertEquals(1, clientIdentifierData.size());
    M2pDedupeRequest.ClientIdentifierData getResult = clientIdentifierData.get(0);
    assertEquals("42", getResult.getDocumentKey());
    assertEquals(45, getResult.getDocumentTypeId().intValue());
  }
}
