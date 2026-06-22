package com.trillionloans.los.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.entity.LoanDetails;
import com.trillionloans.los.model.entity.MasterDocument;
import com.trillionloans.los.model.response.GetLoanV2ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pDocumentTagDTO;
import com.trillionloans.los.repository.LoanTagRepository;
import com.trillionloans.los.repository.MasterTagConfigRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PslServiceTest {

  @InjectMocks private LoanTaggingService pslService;

  @Mock private MasterTagConfigRepository configRepository;

  @Mock private LoanTagRepository loanTagRepository;

  @Mock private M2PWrapperApi m2PWrapperApi;

  private static final String LOAN_ID = "loan-123";
  private static final String CLIENT_ID = "client-456";
  private static final String PSL_TAG = "PSL_DOC";
  private static final String TAG_VALUE_KEY = "tagValue";

  // ------------------------------------------------------------
  // isPslEligible() TESTS
  // ------------------------------------------------------------

  @Test
  void isPslEligible_clientDocumentMatches_shouldReturnTrue() {

    MasterDocument doc = new MasterDocument();
    doc.setDocumentTag(PSL_TAG);

    when(configRepository.findAllByIsPslTrueAndIsActiveTrue()).thenReturn(Flux.just(doc));

    when(m2PWrapperApi.getLoanApplicationByLoanIdV2(eq(LOAN_ID), any()))
        .thenReturn(Mono.just(new GetLoanV2ResponseDTO()));

    when(m2PWrapperApi.getLatestBusinessProofDocumentByClientId(CLIENT_ID))
        .thenReturn(Mono.just(new M2pDocumentTagDTO(PSL_TAG, CLIENT_ID)));

    StepVerifier.create(pslService.isPslEligible(LOAN_ID)).expectNext(true).verifyComplete();
  }

  @Test
  void isPslEligible_loanDocumentMatches_whenClientDoesNotMatch_shouldReturnTrue() {

    MasterDocument doc = new MasterDocument();
    doc.setDocumentTag(PSL_TAG);

    when(configRepository.findAllByIsPslTrueAndIsActiveTrue()).thenReturn(Flux.just(doc));

    when(m2PWrapperApi.getLoanApplicationByLoanIdV2(eq(LOAN_ID), any()))
        .thenReturn(Mono.just(new GetLoanV2ResponseDTO()));

    when(m2PWrapperApi.getLatestBusinessProofDocumentByClientId(CLIENT_ID))
        .thenReturn(Mono.just(new M2pDocumentTagDTO("OTHER", CLIENT_ID)));

    Map<String, Object> loanDoc = Map.of(TAG_VALUE_KEY, PSL_TAG);

    when(m2PWrapperApi.getDocumentList(LOAN_ID)).thenReturn(Mono.just(List.of(loanDoc)));

    StepVerifier.create(pslService.isPslEligible(LOAN_ID)).expectNext(true).verifyComplete();
  }

  @Test
  void isPslEligible_noClientId_shouldFallbackToLoanDocs() {

    MasterDocument doc = new MasterDocument();
    doc.setDocumentTag(PSL_TAG);

    when(configRepository.findAllByIsPslTrueAndIsActiveTrue()).thenReturn(Flux.just(doc));

    when(m2PWrapperApi.getLoanApplicationByLoanIdV2(eq(LOAN_ID), any()))
        .thenReturn(Mono.just(new GetLoanV2ResponseDTO()));

    Map<String, Object> loanDoc = Map.of(TAG_VALUE_KEY, PSL_TAG);

    when(m2PWrapperApi.getDocumentList(LOAN_ID)).thenReturn(Mono.just(List.of(loanDoc)));

    StepVerifier.create(pslService.isPslEligible(LOAN_ID)).expectNext(true).verifyComplete();
  }

  @Test
  void isPslEligible_noMatchAnywhere_shouldReturnFalse() {

    when(configRepository.findAllByIsPslTrueAndIsActiveTrue()).thenReturn(Flux.empty());

    StepVerifier.create(pslService.isPslEligible(LOAN_ID)).expectNext(false).verifyComplete();
  }

  // ------------------------------------------------------------
  // tagLoanForPsl() TESTS
  // ------------------------------------------------------------

  @Test
  void tagLoanForPsl_eligibleAndNoExistingTag_shouldSave() {

    LoanTaggingService spyService = Mockito.spy(pslService);

    doReturn(Mono.just(true)).when(spyService).isPslEligible(LOAN_ID);

    when(loanTagRepository.countByLoanApplicationId(LOAN_ID)).thenReturn(Mono.just(0L));

    when(loanTagRepository.save(any())).thenReturn(Mono.just(new LoanDetails()));

    StepVerifier.create(spyService.tagLoanForPsl(LOAN_ID)).verifyComplete();

    verify(loanTagRepository, times(1)).save(any());
  }

  @Test
  void tagLoanForPsl_eligibleButTagAlreadyExists_shouldNotSave() {

    LoanTaggingService spyService = Mockito.spy(pslService);

    doReturn(Mono.just(true)).when(spyService).isPslEligible(LOAN_ID);

    when(loanTagRepository.countByLoanApplicationId(LOAN_ID)).thenReturn(Mono.just(1L));

    StepVerifier.create(spyService.tagLoanForPsl(LOAN_ID)).verifyComplete();

    verify(loanTagRepository, never()).save(any());
  }

  @Test
  void tagLoanForPsl_notEligible_shouldNotSave() {

    LoanTaggingService spyService = Mockito.spy(pslService);

    doReturn(Mono.just(false)).when(spyService).isPslEligible(LOAN_ID);

    StepVerifier.create(spyService.tagLoanForPsl(LOAN_ID)).verifyComplete();

    verifyNoInteractions(loanTagRepository);
  }

  @Test
  void tagLoanForPsl_error_shouldCompleteWithoutFailure() {

    LoanTaggingService spyService = Mockito.spy(pslService);

    doReturn(Mono.error(new RuntimeException("boom"))).when(spyService).isPslEligible(LOAN_ID);

    StepVerifier.create(spyService.tagLoanForPsl(LOAN_ID)).verifyComplete();
  }
}
