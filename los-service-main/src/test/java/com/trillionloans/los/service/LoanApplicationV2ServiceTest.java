package com.trillionloans.los.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.AadhaarXMLType;
import com.trillionloans.los.model.request.AadharXmlRequestV2;
import com.trillionloans.los.model.request.SelfieUpload;
import com.trillionloans.los.model.response.m2p.DisbursementStatusV2ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pSelfieUploadResponseDTO;
import com.trillionloans.los.model.response.okyc.M2pAadhaarXmlResponseDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class LoanApplicationV2ServiceTest {

  @Mock private M2PWrapperApi m2PWrapperApi;

  @Mock private M2pFacadeService m2pFacadeService;

  @InjectMocks private LoanApplicationV2Service loanApplicationV2Service;

  @Test
  void testUploadAadhaarXml() {
    AadharXmlRequestV2 requestV2 = new AadharXmlRequestV2();
    requestV2.setRequestString("req-string");
    requestV2.setAadhaarXMLType(AadhaarXMLType.OKYC);

    String loanId = "loan123";
    String clientId = "client123";

    M2pAadhaarXmlResponseDTO responseDTO = new M2pAadhaarXmlResponseDTO();

    when(m2pFacadeService.findClientIdFromLoanId(loanId, null)).thenReturn(Mono.just(clientId));

    when(m2PWrapperApi.uploadAadhaarXml(any(), eq(clientId), any(), null))
        .thenReturn(Mono.just(responseDTO));

    Mono<M2pAadhaarXmlResponseDTO> result =
        loanApplicationV2Service.uploadAadhaarXml(requestV2, loanId, "1005");

    StepVerifier.create(result).expectNext(responseDTO).verifyComplete();
  }

  @Test
  void testUploadSelfieAgainstLoan() {
    SelfieUpload selfieUpload = new SelfieUpload();
    selfieUpload.setScore("0.8");
    selfieUpload.setIp("127.0.0.1");

    String loanId = "loan456";
    String clientId = "client456";
    M2pSelfieUploadResponseDTO mockResponse = new M2pSelfieUploadResponseDTO(99);

    when(m2pFacadeService.findClientIdFromLoanId(loanId, null)).thenReturn(Mono.just(clientId));

    when(m2PWrapperApi.uploadSelfieAgainstLead(any(), eq(clientId), null))
        .thenReturn((Mono) Mono.just(mockResponse));

    when(m2PWrapperApi.uploadScoreAgainstLead(any(), any())).thenReturn((Mono) Mono.just("ok"));

    Mono<?> result = loanApplicationV2Service.uploadSelfieAgainstLoan(selfieUpload, loanId);

    StepVerifier.create(result)
        .expectNextMatches(
            r ->
                r instanceof M2pSelfieUploadResponseDTO
                    && ((M2pSelfieUploadResponseDTO) r).imageId() == 99)
        .verifyComplete();
  }

  @Test
  void testGetLoanDisbursementStatus() {
    String loanId = "loan789";

    DisbursementStatusV2ResponseDTO dto = new DisbursementStatusV2ResponseDTO();
    dto.setStatus("DISBURSED");
    List<DisbursementStatusV2ResponseDTO> mockResponseList = List.of(dto);

    when(m2PWrapperApi.getLoanDisbursementStatusV2(loanId)).thenReturn(Mono.just(mockResponseList));

    Mono<?> result = loanApplicationV2Service.getLoanDisbursementStatus(loanId);

    StepVerifier.create(result)
        .expectNextMatches(
            r ->
                r instanceof List
                    && ((List<?>) r).get(0) instanceof DisbursementStatusV2ResponseDTO
                    && "DISBURSED"
                        .equals(
                            ((DisbursementStatusV2ResponseDTO) ((List<?>) r).get(0)).getStatus()))
        .verifyComplete();

    verify(m2PWrapperApi).getLoanDisbursementStatusV2(loanId);
  }
}
