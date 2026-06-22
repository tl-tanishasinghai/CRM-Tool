package com.trillionloans.los.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.response.m2p.M2PCkycInfoResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

  @Mock private M2PWrapperApi m2PWrapperApi;

  @InjectMocks private KycService kycService;

  private static final String LEAD_ID = "someLeadId";

  @Test
  void initiateCkyc_Success() {
    // Arrange
    Map<String, Object> successResponse = createInitiateCkycResponse();
    when(m2PWrapperApi.initiateCkyc(LEAD_ID)).thenAnswer(invocation -> Mono.just(successResponse));

    // Act & Assert
    StepVerifier.create(kycService.initiateCkyc(LEAD_ID))
        .assertNext(
            response -> {
              assertNotNull(response);
              @SuppressWarnings("unchecked")
              Map<String, Object> responseMap = (Map<String, Object>) response;
              assertEquals("ckyc", responseMap.get("source"));
              assertNotNull(responseMap.get("additionalInfo"));
              @SuppressWarnings("unchecked")
              Map<String, Object> additionalInfo =
                  (Map<String, Object>) responseMap.get("additionalInfo");
              assertEquals("7148", additionalInfo.get("clientId"));
            })
        .verifyComplete();
  }

  @Test
  void initiateCkyc_AuthenticationError() {
    // Arrange
    String errorMessage =
        "Authentication Failed: Date of birth entered does not match with the date of birth stated"
            + " in the CKYC number 40014836546357";
    when(m2PWrapperApi.initiateCkyc(LEAD_ID))
        .thenReturn(Mono.error(new RuntimeException(errorMessage)));

    // Act & Assert
    StepVerifier.create(kycService.initiateCkyc(LEAD_ID))
        .expectErrorMatches(
            throwable ->
                throwable instanceof RuntimeException
                    && throwable.getMessage().equals(errorMessage))
        .verify();
  }

  @Test
  void getCkycStatus_Success() {
    // Arrange
    List<Map<String, String>> statusResponse = createStatusResponse("VERIFIED", "96.11", "100.0");
    when(m2PWrapperApi.getCkycStatus(LEAD_ID)).thenAnswer(invocation -> Mono.just(statusResponse));

    // Act & Assert
    StepVerifier.create(kycService.getCkycStatus(LEAD_ID))
        .assertNext(
            response -> {
              assertNotNull(response);
              @SuppressWarnings("unchecked")
              List<Map<String, String>> responseList = (List<Map<String, String>>) response;
              assertEquals(1, responseList.size());
              assertEquals("VERIFIED", responseList.get(0).get("ckycNamematchstatus"));
              assertEquals("96.11", responseList.get(0).get("ckycNamematchscore"));
              assertEquals("VERIFIED", responseList.get(0).get("ckycfacematchstatus"));
            })
        .verifyComplete();
  }

  @Test
  void getCkycStatus_Rejected() {
    // Arrange
    List<Map<String, String>> statusResponse = createStatusResponse("REJECTED", "80.0", "60.0");
    when(m2PWrapperApi.getCkycStatus(LEAD_ID)).thenAnswer(invocation -> Mono.just(statusResponse));

    // Act & Assert
    StepVerifier.create(kycService.getCkycStatus(LEAD_ID))
        .assertNext(
            response -> {
              assertNotNull(response);
              @SuppressWarnings("unchecked")
              List<Map<String, String>> responseList = (List<Map<String, String>>) response;
              assertEquals(1, responseList.size());
              assertEquals("REJECTED", responseList.get(0).get("ckycNamematchstatus"));
              assertEquals("REJECTED", responseList.get(0).get("ckycfacematchstatus"));
            })
        .verifyComplete();
  }

  @Test
  void getCkycInfo_Success() {

    M2PCkycInfoResponse ckycInfo = createCkycInfoResponse();
    when(m2PWrapperApi.getCkycInfo(LEAD_ID)).thenReturn(Mono.just(ckycInfo));

    // Act & Assert
    StepVerifier.create(kycService.getCkycInfo(LEAD_ID))
        .assertNext(
            response -> {
              assertNotNull(response);

              // Verify personal information
              @SuppressWarnings("unchecked")
              M2PCkycInfoResponse.PersonalInfo personalInfo = response.getPersonalInfo();
              assertEquals("Mr KARAN MEHTA", personalInfo.getName());
              assertEquals("07-08-1994", personalInfo.getDob());
              assertEquals("M", personalInfo.getGender());
              assertEquals("9876584385", personalInfo.getMobile());
              assertEquals("Mr RAJESH MEHTA", personalInfo.getFatherName());

              // Verify address information
              @SuppressWarnings("unchecked")
              List<M2PCkycInfoResponse.AddressInfo> addressInfo = response.getAddressInfo();
              assertEquals(2, addressInfo.size());
              assertEquals("PERMANENT", addressInfo.get(0).getAddressType());
              assertEquals("CORRESPONDENCE", addressInfo.get(1).getAddressType());

              // Verify identifier information
              @SuppressWarnings("unchecked")
              List<M2PCkycInfoResponse.IdentifierInfo> identifierInfo =
                  response.getIdentifierInfo();
              assertEquals(2, identifierInfo.size());
              assertEquals("PAN", identifierInfo.get(0).getDocumentType());

              // Verify document information
              @SuppressWarnings("unchecked")
              List<M2PCkycInfoResponse.DocumentInfo> documentInfo = response.getDocumentInfo();
              assertEquals(3, documentInfo.size());
              assertEquals("PAN", documentInfo.get(0).getDocumentType());
            })
        .verifyComplete();
  }

  @Test
  void getCkycInfo_NotFound() {
    // Arrange
    when(m2PWrapperApi.getCkycInfo(LEAD_ID))
        .thenReturn(
            Mono.error(
                new WebClientResponseException(404, "Not Found", HttpHeaders.EMPTY, null, null)));

    // Act & Assert
    StepVerifier.create(kycService.getCkycInfo(LEAD_ID))
        .expectErrorMatches(
            throwable ->
                throwable instanceof WebClientResponseException
                    && ((WebClientResponseException) throwable).getStatusCode()
                        == HttpStatus.NOT_FOUND)
        .verify();
  }

  // Helper methods to create test data
  private Map<String, Object> createInitiateCkycResponse() {
    Map<String, Object> additionalInfo = new HashMap<>();
    additionalInfo.put("clientId", "7148");
    additionalInfo.put("source", "ckyc");
    additionalInfo.put(
        "personalInfo",
        "{\"name\":\"Mr KARAN "
            + " MEHTA\",\"dob\":\"07-08-1994\",\"gender\":\"M\",\"mobileNumber\":\"9876584385\",\"fatherName\":\"Mr"
            + " RAJESH  MEHTA\"}");
    additionalInfo.put(
        "identifierInfo",
        "[{\"ckycIDType\":\"PAN\",\"ckycIDNumber\":\"CTLPM4576M\"},{\"ckycIDType\":\"CKYC"
            + " No\",\"ckycIDNumber\":\"20029021612243\"}]");
    additionalInfo.put(
        "addressInfo",
        "[{\"fullAddress\":\"S O RAJESH MEHTA HOUSE NUMBER 242 WARD NUMBER 2 MIANI HOSHIARPUR"
            + " PUNJAB"
            + " 144202\",\"addressType\":\"PERMANENT\",\"city\":\"Hoshiarpur\",\"state\":\"PB\",\"pincode\":\"144202\",\"country\":\"IN\"}]");
    additionalInfo.put(
        "documentInfo",
        "[{\"documentType\":\"AADHAAR\",\"documentId\":\"11024\",\"documentExtension\":\"image/jpg\",\"isVideo\":false}]");

    Map<String, Object> response = new HashMap<>();
    response.put("source", "ckyc");
    response.put("additionalInfo", additionalInfo);

    return response;
  }

  private List<Map<String, String>> createStatusResponse(
      String status, String nameScore, String faceScore) {
    Map<String, String> statusMap = new HashMap<>();
    statusMap.put("ckycNamematchstatus", status);
    statusMap.put("ckycNamematchscore", nameScore);
    statusMap.put("ckycfacematchscore", faceScore);
    statusMap.put("ckycfacematchstatus", status);

    List<Map<String, String>> response = new ArrayList<>();
    response.add(statusMap);
    return response;
  }

  private M2PCkycInfoResponse createCkycInfoResponse() {
    M2PCkycInfoResponse response = new M2PCkycInfoResponse();

    M2PCkycInfoResponse.PersonalInfo personalInfo = new M2PCkycInfoResponse.PersonalInfo();
    personalInfo.setName("Mr KARAN MEHTA");
    personalInfo.setDob("07-08-1994");
    personalInfo.setGender("M");
    personalInfo.setMobile("9876584385");
    personalInfo.setFatherName("Mr RAJESH MEHTA");
    response.setPersonalInfo(personalInfo);

    List<M2PCkycInfoResponse.AddressInfo> addressList = new ArrayList<>();
    M2PCkycInfoResponse.AddressInfo permanent = new M2PCkycInfoResponse.AddressInfo();
    permanent.setAddressType("PERMANENT");
    permanent.setCity("Hoshiarpur");
    permanent.setPincode("144202");

    M2PCkycInfoResponse.AddressInfo correspondence = new M2PCkycInfoResponse.AddressInfo();
    correspondence.setAddressType("CORRESPONDENCE");
    correspondence.setCity("Hoshiarpur");
    correspondence.setPincode("144202");

    addressList.add(permanent);
    addressList.add(correspondence);
    response.setAddressInfo(addressList);

    List<M2PCkycInfoResponse.IdentifierInfo> identifierList = new ArrayList<>();
    M2PCkycInfoResponse.IdentifierInfo pan = new M2PCkycInfoResponse.IdentifierInfo();
    pan.setDocumentType("PAN");

    M2PCkycInfoResponse.IdentifierInfo ckyc = new M2PCkycInfoResponse.IdentifierInfo();
    ckyc.setDocumentType("CKYC No");

    identifierList.add(pan);
    identifierList.add(ckyc);
    response.setIdentifierInfo(identifierList);

    List<M2PCkycInfoResponse.DocumentInfo> documentList = new ArrayList<>();
    M2PCkycInfoResponse.DocumentInfo doc1 = new M2PCkycInfoResponse.DocumentInfo();
    doc1.setDocumentType("PAN");

    M2PCkycInfoResponse.DocumentInfo doc2 = new M2PCkycInfoResponse.DocumentInfo();
    doc2.setDocumentType("AADHAAR");

    M2PCkycInfoResponse.DocumentInfo doc3 = new M2PCkycInfoResponse.DocumentInfo();
    doc3.setDocumentType("PHOTO");

    documentList.add(doc1);
    documentList.add(doc2);
    documentList.add(doc3);
    response.setDocumentInfo(documentList);

    return response;
  }
}
