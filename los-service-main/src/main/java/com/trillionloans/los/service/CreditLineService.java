package com.trillionloans.los.service;

import com.trillionloans.los.model.partner.m2p.M2PGenerateCreditLineRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2PGenerateCreditLineResponseDTO;
import com.trillionloans.los.model.request.CreditLineLoanApplication;
import com.trillionloans.los.model.request.InitiateCreditLineRequestDTO;
import com.trillionloans.los.model.request.m2p.CreditLineStatusCallbackRequest;
import com.trillionloans.los.model.response.CreditLineCallbackToPartnerDTO;
import com.trillionloans.los.model.response.InitiateCreditLineResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pLoanCreationResponseDTO;
import reactor.core.publisher.Mono;

/**
 * Service interface for Credit Line operations.
 *
 * <p>This interface defines the contract for all credit line related operations including creation,
 * generation, approval, activation, and status management.
 */
public interface CreditLineService {

  /**
   * Creates a credit line lead with product validation.
   *
   * <p>This method validates that the product code is a valid credit line product before proceeding
   * with loan application creation.
   *
   * @param creditLineRequest the credit line loan application request
   * @param leadId the lead/client ID
   * @param productCode the product code from request header
   * @return Mono containing the loan creation response
   */
  Mono<M2pLoanCreationResponseDTO> createCreditLineLead(
      CreditLineLoanApplication creditLineRequest, String leadId, String productCode);

  /**
   * Generates a credit line for the given lead.
   *
   * <p>This method calls the M2P API to generate the credit line, registers the CTA, and persists
   * the credit line ID.
   *
   * @param request the generate credit line request
   * @param leadId the lead ID
   * @param productCode the product code
   * @return Mono containing the generate credit line response
   */
  Mono<M2PGenerateCreditLineResponseDTO> generateCreditLine(
      M2PGenerateCreditLineRequestDTO request, String leadId, String productCode);

  /**
   * Approves a credit line for the given lead.
   *
   * @param leadId the lead ID
   * @param productCode the product code
   * @return Mono containing the approval response
   */
  Mono<Object> approveCreditLine(String leadId, String productCode);

  /**
   * Activates a credit line for the given lead.
   *
   * @param leadId the lead ID
   * @param productCode the product code
   * @return Mono containing the activation response
   */
  Mono<Object> activateCreditLine(String leadId, String productCode);

  /**
   * Fetches credit line details for the given lead.
   *
   * @param leadId the lead ID
   * @param productCode the product code
   * @return Mono containing the credit line details
   */
  Mono<Object> fetchCreditLine(String leadId, String productCode);

  Mono<Object> fetchCreditLineDetailsByLineId(String lineId, String productCode);

  /**
   * Initiates a credit line by saving the credit line details with PENDING status.
   *
   * @param request the initiate credit line request containing limit and tenure details
   * @param leadId the lead ID
   * @param productCode the product code
   * @return Mono containing the response with status
   */
  Mono<InitiateCreditLineResponseDTO> initiateCreditLine(
      InitiateCreditLineRequestDTO request, String leadId, String productCode);

  /**
   * Gets the current status of a credit line for the given lead.
   *
   * <p>This method retrieves the credit line status from the database and returns a DTO with the
   * current state. Can be used as an alternative to callback-based status updates.
   *
   * @param leadId the lead ID
   * @param productCode the product code
   * @return Mono containing the credit line status DTO
   */
  Mono<CreditLineCallbackToPartnerDTO> getCreditLineStatusByLeadId(
      String leadId, String productCode);

  Mono<CreditLineCallbackToPartnerDTO> getCreditLineStatusByLineId(
      String leadId, String productCode);

  /**
   * Processes the credit line status callback from M2P.
   *
   * <p>On SUCCESS status:
   *
   * <ul>
   *   <li>Calls generate limit API
   *   <li>Calls approve limit API
   *   <li>Calls activate limit API
   *   <li>Updates timestamps in credit_line table
   *   <li>Sends callback to partner
   * </ul>
   *
   * @param callbackRequest the callback request from M2P
   * @param productCode the product code
   * @return Mono containing the result
   */
  Mono<Object> processCreditLineStatusCallback(
      CreditLineStatusCallbackRequest callbackRequest, String productCode);
}
