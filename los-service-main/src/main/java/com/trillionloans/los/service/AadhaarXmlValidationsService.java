package com.trillionloans.los.service;

import com.trillionloans.los.constant.KycValidationVendors;
import com.trillionloans.los.model.dto.MatchingScoreDTO;
import reactor.core.publisher.Mono;

public interface AadhaarXmlValidationsService {

  Mono<MatchingScoreDTO> parallelNameMatchExecution(
      String nameOne, String nameTwo, String clientId, String loanId);

  Mono<MatchingScoreDTO> sequenceNameMatchExecution(
      KycValidationVendors validationPriority,
      String nameOne,
      String nameTwo,
      boolean fallbackEnabled,
      String clientId,
      String loanId);

  Mono<MatchingScoreDTO> parallelFaceMatchExecution(
      String face1Base64, String face2Base64, String clientId, String loanId);

  Mono<MatchingScoreDTO> sequenceFaceMatchExecution(
      KycValidationVendors validationPriority,
      String face1Base64,
      String face2Base64,
      boolean fallbackEnabled,
      String clientId,
      String loanId);
}
