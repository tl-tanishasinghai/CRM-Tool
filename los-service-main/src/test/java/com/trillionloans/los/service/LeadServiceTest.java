package com.trillionloans.los.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.Gender;
import com.trillionloans.los.constant.RelationshipType;
import com.trillionloans.los.model.dto.AddressDetailsDTO;
import com.trillionloans.los.model.dto.BankDetailsDTO;
import com.trillionloans.los.model.dto.ClientDetailsUpdateDTO;
import com.trillionloans.los.model.dto.EmploymentDetailsUpdateDTO;
import com.trillionloans.los.model.dto.FamilyDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadUpdateDTO;
import com.trillionloans.los.model.request.LeadUpdate;
import com.trillionloans.los.model.request.SelfieUpload;
import com.trillionloans.los.model.response.m2p.M2pSelfieUploadResponseDTO;
import com.trillionloans.los.service.db.PartnerMasterService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {LeadService.class})
@ExtendWith(SpringExtension.class)
class LeadServiceTest {
  @Autowired private LeadService leadService;

  @MockBean private M2PWrapperApi m2PWrapperApi;

  @MockBean private PartnerMasterService partnerMasterService;

  @MockBean private ProductConfigMasterService productConfigMasterService;

  @MockBean private KycService kycService;

  /** Method under test: {@link LeadService#getLeadData(String)} */
  @Test
  void testGetLeadData() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.getLeadData(Mockito.<String>any())).thenReturn(justResult);
    Mono<?> actualLeadData = leadService.getLeadData("42");
    verify(m2PWrapperApi).getLeadData(Mockito.<String>any());
    assertSame(justResult, actualLeadData);
  }

  /** Method under test: {@link LeadService#updateLead(LeadUpdate, String)} */
  @Test
  void testUpdateLead() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any()))
        .thenReturn(justResult);
    Mono<?> actualUpdateLeadResult = leadService.updateLead(new LeadUpdate(), "42");
    verify(m2PWrapperApi).updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any());
    assertSame(justResult, actualUpdateLeadResult);
  }

  /** Method under test: {@link LeadService#updateLead(LeadUpdate, String)} */
  @Test
  void testUpdateLeadWithParameters() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any()))
        .thenReturn(justResult);
    LeadUpdate.LeadUpdateBuilder builderResult = LeadUpdate.builder();
    builderResult.bankDetails(new ArrayList<>());
    ClientDetailsUpdateDTO clientDetails =
        ClientDetailsUpdateDTO.builder()
            .education("Education")
            .email("jane.doe@example.org")
            .build();
    builderResult.clientDetails(clientDetails);
    EmploymentDetailsUpdateDTO employmentDetails =
        EmploymentDetailsUpdateDTO.builder()
            .companyType("Company Type")
            .currentEmployerName("Current Employer Name")
            .employmentType("Employment Type")
            .monthlySalary(10.0d)
            .totalWorkExperience(1)
            .build();
    builderResult.employmentDetails(employmentDetails);
    LeadUpdate leadData = builderResult.familyDetails(new ArrayList<>()).build();
    Mono<?> actualUpdateLeadResult = leadService.updateLead(leadData, "42");
    verify(m2PWrapperApi).updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any());
    assertSame(justResult, actualUpdateLeadResult);
  }

  /** Method under test: {@link LeadService#updateLead(LeadUpdate, String)} */
  @Test
  void testUpdateLeadWithMultipleParameters() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any()))
        .thenReturn(justResult);
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
    Mono<?> actualUpdateLeadResult =
        leadService.updateLead(
            new LeadUpdate(
                clientDetails, addressDetails, familyDetails, bankDetails, employmentDetails, null),
            "42");
    verify(m2PWrapperApi).updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any());
    assertSame(justResult, actualUpdateLeadResult);
  }

  /** Method under test: {@link LeadService#updateLead(LeadUpdate, String)} */
  @Test
  void testUpdateLeadDetailedParameterScenario() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any()))
        .thenReturn(justResult);

    ArrayList<BankDetailsDTO> bankDetails = new ArrayList<>();
    BankDetailsDTO buildResult =
        BankDetailsDTO.builder()
            .accountNumber("42")
            .accountType("3")
            .ifscCode("Ifsc Code")
            .name("Name")
            .supportedForDisbursement(true)
            .supportedForRepayment(true)
            .build();
    bankDetails.add(buildResult);
    LeadUpdate.LeadUpdateBuilder bankDetailsResult = LeadUpdate.builder().bankDetails(bankDetails);
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
    Mono<?> actualUpdateLeadResult = leadService.updateLead(leadData, "42");
    verify(m2PWrapperApi).updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any());
    assertSame(justResult, actualUpdateLeadResult);
  }

  /** Method under test: {@link LeadService#updateLead(LeadUpdate, String)} */
  @Test
  void testUpdateLeadWithFamilyClientEmployment() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any()))
        .thenReturn(justResult);

    ArrayList<FamilyDetailsDTO> familyDetails = new ArrayList<>();
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
    familyDetails.add(buildResult);
    LeadUpdate.LeadUpdateBuilder builderResult = LeadUpdate.builder();
    LeadUpdate.LeadUpdateBuilder bankDetailsResult = builderResult.bankDetails(new ArrayList<>());
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
    LeadUpdate leadData =
        clientDetailsResult
            .employmentDetails(employmentDetails)
            .familyDetails(familyDetails)
            .build();
    Mono<?> actualUpdateLeadResult = leadService.updateLead(leadData, "42");
    verify(m2PWrapperApi).updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any());
    assertSame(justResult, actualUpdateLeadResult);
  }

  /** Method under test: {@link LeadService#updateLead(LeadUpdate, String)} */
  @Test
  void testUpdateLeadDetailedParameterAndEmptyIdentifier() {
    Mono<?> justResult = Mono.just("Data");
    Mockito.<Mono<?>>when(
            m2PWrapperApi.updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any()))
        .thenReturn(justResult);

    ArrayList<AddressDetailsDTO> addressDetails = new ArrayList<>();
    AddressDetailsDTO buildResult =
        AddressDetailsDTO.builder()
            .landmark("Landmark")
            .ownershipType("Ownership Type")
            .postalCode("Postal Code")
            .build();
    addressDetails.add(buildResult);
    ClientDetailsUpdateDTO clientDetails =
        ClientDetailsUpdateDTO.builder()
            .education("Education")
            .email("jane.doe@example.org")
            .build();
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
    Mono<?> actualUpdateLeadResult =
        leadService.updateLead(
            new LeadUpdate(
                clientDetails, addressDetails, familyDetails, bankDetails, employmentDetails, null),
            "");
    verify(m2PWrapperApi).updateLead(Mockito.<M2pLeadUpdateDTO>any(), Mockito.<String>any());
    assertSame(justResult, actualUpdateLeadResult);
  }

  /** Method under test: {@link LeadService#uploadSelfieAgainstLead(SelfieUpload, String)} */
  @Test
  void testUploadSelfieAgainstLead() {
    M2pSelfieUploadResponseDTO m2pSelfieUploadResponseDTO = new M2pSelfieUploadResponseDTO(123);

    Mono<?> justResult = Mono.just(m2pSelfieUploadResponseDTO);

    Mockito.<Mono<?>>when(
            m2PWrapperApi.uploadSelfieAgainstLead(
                Mockito.<SelfieUpload>any(), Mockito.<String>any()))
        .thenReturn(justResult);

    Mono<?> actualUploadSelfieAgainstLeadResult =
        leadService.uploadSelfieAgainstLead(new SelfieUpload(), "42");

    StepVerifier.create(actualUploadSelfieAgainstLeadResult)
        .consumeNextWith(
            result -> {
              assertTrue(result instanceof M2pSelfieUploadResponseDTO);
              M2pSelfieUploadResponseDTO actual = (M2pSelfieUploadResponseDTO) result;
              assertEquals(123, actual.imageId());
            })
        .verifyComplete();

    Mockito.verify(m2PWrapperApi).uploadSelfieAgainstLead(Mockito.any(), Mockito.any());
  }

  @Test
  void testGetLeadInfo() {
    Mono<?> justResult = Mono.just("Lead Data");
    Mockito.<Mono<?>>when(m2PWrapperApi.getLeadInfo(Mockito.any())).thenReturn(justResult);
    Mono<?> actualLeadInfo = leadService.getLeadInfo("1234567890");
    verify(m2PWrapperApi).getLeadInfo(Mockito.any());
    assertSame(justResult, actualLeadInfo);
  }
}
