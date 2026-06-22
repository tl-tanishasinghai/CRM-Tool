package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.DATE_FORMAT;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.ResponseStatus;
import com.trillionloans.los.model.partner.m2p.M2pMerchantDetailsRequest;
import com.trillionloans.los.model.request.MerchantBankDetails;
import com.trillionloans.los.model.request.MerchantChangeRequest;
import com.trillionloans.los.model.request.MerchantDetailsRequest;
import com.trillionloans.los.model.response.ResponseDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class MerchantService {
  private M2PWrapperApi m2PWrapperApi;

  /**
   * Stamps the merchant details using the provided request.
   *
   * @param merchantDetailsRequest the request containing the merchant details to be stamped
   * @return a Mono wrapping the response object from the stamp operation
   */
  public Mono<Object> stampMerchantDetails(MerchantDetailsRequest merchantDetailsRequest) {
    M2pMerchantDetailsRequest m2pMerchantDetailsRequest =
        M2pMerchantDetailsRequest.builder()
            .name(merchantDetailsRequest.getName())
            .shortName(merchantDetailsRequest.getShortName())
            .locale("en")
            .dateFormat(DATE_FORMAT)
            .limitAmount(merchantDetailsRequest.getLimitAmount())
            .relationshipEndDate(merchantDetailsRequest.getRelationshipEndDate())
            .category(merchantDetailsRequest.getCategory())
            .officeId(merchantDetailsRequest.getOfficeId())
            .contactPersondetails(merchantDetailsRequest.getContactPersonDetails())
            .build();
    return m2PWrapperApi.stampMerchantDetails(m2pMerchantDetailsRequest);
  }

  /**
   * Stamps the merchant's bank account details using the provided bank details and identifier.
   *
   * @param merchantBankDetails the bank account details of the merchant
   * @param identifier a unique identifier for the merchant
   * @return a Mono wrapping the response object from the stamp operation
   */
  public Mono<Object> stampMerchantBankAccountDetails(
      MerchantBankDetails merchantBankDetails, String identifier) {
    return m2PWrapperApi.stampMerchantBankAccountDetails(merchantBankDetails, identifier);
  }

  /**
   * Updates the merchant details against a loan application.
   *
   * <p>This method attempts to update the merchant information related to a specified loan
   * application. If the update operation results in an empty response, a default success message is
   * returned.
   *
   * @param merchantChangeRequest the request containing the changes to be applied to the merchant
   * @param loanId the ID of the loan application to update against
   * @return a Mono wrapping the response object, which may contain the updated details or a success
   *     message
   */
  public Mono<Object> updateMerchantAgainstLoanApplication(
      MerchantChangeRequest merchantChangeRequest, String loanId) {
    return Mono.defer(
        () ->
            m2PWrapperApi
                .updateMerchantAgainstLoanApplication(merchantChangeRequest, loanId)
                .switchIfEmpty(
                    Mono.defer(
                        () ->
                            Mono.just(
                                ResponseDTO.builder()
                                    .message("successfully updated loan application")
                                    .status(ResponseStatus.SUCCESS)
                                    .build())))
                .onErrorResume(Mono::error));
  }

  /**
   * Retrieves the merchant details associated with a specified loan application.
   *
   * @param loanId the ID of the loan application for which to retrieve merchant details
   * @return a Mono wrapping the response object containing the merchant details
   */
  public Mono<Object> getMerchantAgainstLoanApplication(String loanId) {
    return m2PWrapperApi.getMerchantAgainstLoanApplication(loanId);
  }
}
