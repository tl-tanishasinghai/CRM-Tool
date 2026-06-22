package com.trillionloans.lms.service;

import com.trillionloans.lms.api.m2p.M2PApi;
import com.trillionloans.lms.exception.BaseException;
import com.trillionloans.lms.model.request.DocumentUploadRequest;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Service class responsible for handling loan-related operations. */
@Service
@Slf4j
public class LoanService {

  private final M2PApi m2PApi;

  /**
   * Constructs a new instance of {@link LoanService} with the required dependencies.
   *
   * @param m2PApi the API client used for interacting with the external M2P system.
   */
  @Autowired
  public LoanService(M2PApi m2PApi) {
    this.m2PApi = m2PApi;
  }

  /**
   * Uploads a document against a specific loan.
   *
   * <p>This method validates the file type before uploading it to the M2P API. If the file type is
   * invalid, it logs an error and returns a {@link Mono} with a {@link BaseException}.
   *
   * @param documentUploadRequest the document upload request containing the file and metadata.
   * @param loanId the identifier of the loan to which the document is being uploaded.
   * @return a {@link Mono} that completes when the upload process is finished. Emits an error if
   *     the file type is invalid or the upload fails.
   */
  public Mono<?> uploadDocumentAgainstLoan(
      DocumentUploadRequest documentUploadRequest, String loanId) {
    return m2PApi.uploadDocumentAgainstLoan(documentUploadRequest, loanId);
  }

  /**
   * Retrieves loan details for a given loan ID using the M2P API.
   *
   * @param loanId The unique identifier of the loan to be retrieved.
   * @param staffInSelectedOfficeOnly A flag indicating whether to filter loans based on staff in
   *     the selected office.
   * @param associations A set of loan associations to include in the response (e.g.,
   *     repaymentSchedule, transactions, charges).
   * @param exclude A set of loan associations to exclude from the response.
   * @param fields A set of specific fields to be included in the response.
   * @return A {@code Mono<?>} containing the loan details retrieved from the M2P API.
   */
  public Mono<Object> retrieveLoan(
      Long loanId,
      boolean staffInSelectedOfficeOnly,
      Set<String> associations,
      Set<String> exclude,
      Set<String> fields) {
    return m2PApi.retrieveLoan(loanId, staffInSelectedOfficeOnly, associations, exclude, fields);
  }
}
