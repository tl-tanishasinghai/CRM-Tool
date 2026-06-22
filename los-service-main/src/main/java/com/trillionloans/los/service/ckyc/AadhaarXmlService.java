package com.trillionloans.los.service.ckyc;

import com.trillionloans.los.constant.AadhaarXMLType;
import com.trillionloans.los.model.request.AadhaarXmlRequest;
import com.trillionloans.los.model.response.okyc.M2pAadhaarXmlResponseDTO;
import reactor.core.publisher.Mono;

public interface AadhaarXmlService {

  Mono<M2pAadhaarXmlResponseDTO> uploadAadhaarXml(
      AadhaarXmlRequest aadhaarXmlRequest,
      String leadId,
      AadhaarXMLType aadhaarXMLType,
      String loanAppId,
      String productCode);
}
