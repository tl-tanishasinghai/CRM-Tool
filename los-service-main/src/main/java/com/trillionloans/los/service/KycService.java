package com.trillionloans.los.service;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.response.m2p.M2PCkycInfoResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service class to handle KYC operations. This class interacts with the M2PWrapperApi to manage
 * KYC-related processes, such as fetching CKYC status, initiating CKYC, and retrieving CKYC
 * information.
 */
@Service
@Slf4j
@AllArgsConstructor
public class KycService {
  private final M2PWrapperApi m2PWrapperApi;

  /**
   * Retrieves the CKYC status for a given loan ID.
   *
   * @param loanId The ID of the loan for which CKYC status is to be fetched.
   * @return A {@code Mono<?>} that emits the CKYC status response.
   */
  public Mono<?> getCkycStatus(String loanId) {
    return m2PWrapperApi.getCkycStatus(loanId);
  }

  /**
   * Initiates the CKYC process for a given lead ID.
   *
   * @param leadId The ID of the lead for which CKYC process is to be initiated.
   * @return A {@code Mono<?>} that emits the response of the initiation process.
   */
  public Mono<?> initiateCkyc(String leadId) {
    return m2PWrapperApi.initiateCkyc(leadId);
  }

  /**
   * Fetches detailed CKYC information for a given lead ID.
   *
   * @param leadId The ID of the lead for which CKYC information is to be fetched.
   * @return A {@code Mono<?>} that emits the CKYC information response.
   */
  public Mono<M2PCkycInfoResponse> getCkycInfo(String leadId) {
    return m2PWrapperApi.getCkycInfo(leadId);
  }

  /**
   * Retrieves the VKYC status for a given loan ID.
   *
   * @param loanId The ID of the loan for which VKYC status is to be fetched.
   * @return A {@code Mono<?>} that emits the VKYC status response.
   */
  public Mono<Object> getVkycStatus(String loanId) {
    return m2PWrapperApi.getVkycStatus(loanId);
  }

  /**
   * Initiates the VKYC process for a given lead ID.
   *
   * @param leadId The ID of the lead for which VKYC process is to be initiated.
   * @return A {@code Mono<?>} that emits the response of the initiation process.
   */
  public Mono<Object> initiateVkyc(String leadId) {
    return m2PWrapperApi.initiateVkyc(leadId);
  }
}
