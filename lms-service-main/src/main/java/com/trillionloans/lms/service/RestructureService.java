package com.trillionloans.lms.service;

import com.trillionloans.lms.model.dto.restructure.ApproveRestructureResponseDTO;
import reactor.core.publisher.Mono;

/**
 * Service interface for handling loan restructure operations.
 *
 * @author Pawan Kumar
 */
public interface RestructureService {
  Mono<?> getRestructureDetails(String lan, String type, String requestId);

  /**
   * Approves a restructured loan request.
   *
   * @param lan The loan account number.
   * @param requestId The reschedule request ID from eligibility API.
   * @return A Mono containing the approval response.
   */
  Mono<ApproveRestructureResponseDTO> approveRestructure(String lan, String requestId);
}
