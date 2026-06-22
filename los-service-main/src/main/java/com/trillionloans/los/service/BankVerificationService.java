package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.ATTACH_BANK_ACCOUNT_CTA_IDENTIFIER;
import static com.trillionloans.los.constant.StringConstants.ATTACH_BANK_ACCOUNT_LOAN;
import static com.trillionloans.los.constant.StringConstants.BANK_VERIFICATION_FAILED;
import static com.trillionloans.los.constant.StringConstants.FAIL;
import static com.trillionloans.los.constant.StringConstants.MESSAGE;
import static com.trillionloans.los.constant.StringConstants.PASS;
import static com.trillionloans.los.constant.StringConstants.SOMETHING_WENT_WRONG_CONFIG;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.api.partner.TransactionApi;
import com.trillionloans.los.constant.BankAccountType;
import com.trillionloans.los.constant.BeneficaryType;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.ServerErrorException;
import com.trillionloans.los.model.AttachBankDetailsDTO;
import com.trillionloans.los.model.dto.BankDetailsDTO;
import com.trillionloans.los.model.dto.BankVerificationDetailsDTO;
import com.trillionloans.los.model.dto.BankVerificationResponseDTO;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.partner.m2p.M2pBeneficiaryBankDetailsDTO;
import com.trillionloans.los.model.request.LoanBankAccountDataTableDTO;
import com.trillionloans.los.model.request.NachMandateRequest;
import com.trillionloans.los.model.request.m2p.M2pBankDetailsRequestDTO;
import com.trillionloans.los.model.response.BankVerificationStatusResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pAddBankDetailsResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pBankDetailsResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pGetBeneficiaryBankDetailsResponse;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import java.util.Map;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service class responsible for handling bank verification and attachment of bank accounts to loans
 * for both self and third-party beneficiaries.
 */
@Service
@Slf4j
@AllArgsConstructor
public class BankVerificationService {

  private final TransactionApi transactionApi;
  private final M2PWrapperApi m2PWrapperApi;
  private final ProductConfigMasterService productConfigMasterService;

  public Mono<BankVerificationStatusResponseDTO> getBankVerificationStatus(
      String clientId, String bankId) {
    return transactionApi.getBankVerificationStatus(clientId, bankId);
  }

  public Mono<M2pAddBankDetailsResponseDTO> attachBankAccountProductWise(
      String loanId, String leadId, AttachBankDetailsDTO attachBankDetailsDTO, String productCode) {
    return productConfigMasterService
        .getProductConfigMasterData(productCode)
        .flatMap(
            productControlConfigData -> {
              ProductControl.Flow flowData =
                  productConfigMasterService.getFlowFromProductConfig(
                      productControlConfigData.getT2(), ATTACH_BANK_ACCOUNT_CTA_IDENTIFIER);
              if (Objects.isNull(flowData)) {
                log.error("Failed to retrieve flow data for product code: {}", productCode);
                return Mono.error(
                    new BaseException(
                        SOMETHING_WENT_WRONG_CONFIG,
                        SOMETHING_WENT_WRONG_CONFIG,
                        HttpStatus.INTERNAL_SERVER_ERROR));
              }
              return attachBankAccount(loanId, leadId, attachBankDetailsDTO, productCode, flowData);
            });
  }

  /** Attaches a bank account to a loan based on the beneficiary type. */
  public Mono<M2pAddBankDetailsResponseDTO> attachBankAccount(
      String loanId,
      String leadId,
      AttachBankDetailsDTO attachBankDetailsDTO,
      String productCode,
      ProductControl.Flow flowData) {
    log.info(
        "Attaching bank account for loan ID: {}, lead ID: {}, product code: {}",
        loanId,
        leadId,
        productCode);
    boolean pennyDropFlag =
        flowData.getConditions() != null
            && (boolean) flowData.getConditions().getOrDefault("pennyDropFlag", false);
    if (attachBankDetailsDTO.getBeneficiaryType().equals(String.valueOf(BeneficaryType.SELF))) {
      return handleSelfBeneficiary(
          loanId, leadId, attachBankDetailsDTO, productCode, pennyDropFlag, flowData);
    } else if (attachBankDetailsDTO
        .getBeneficiaryType()
        .equals(String.valueOf(BeneficaryType.THIRD_PARTY))) {
      return handleThirdPartyBeneficiary(
          loanId, leadId, attachBankDetailsDTO, productCode, pennyDropFlag, flowData);
    } else {
      log.error("Invalid beneficiary type: {}", attachBankDetailsDTO.getBeneficiaryType());
      return Mono.error(
          new BaseException(
              "Invalid beneficiary type!", "Invalid beneficiary type!", HttpStatus.BAD_REQUEST));
    }
  }

  /** Handles the attachment of a bank account for a self beneficiary. */
  @SuppressWarnings("unchecked")
  private Mono<M2pAddBankDetailsResponseDTO> handleSelfBeneficiary(
      String loanId,
      String leadId,
      AttachBankDetailsDTO attachBankDetailsDTO,
      String productCode,
      boolean pennyDropFlag,
      ProductControl.Flow flowData) {

    return m2PWrapperApi
        .getLeadData(leadId)
        .flatMap(
            leadData -> {
              String fullName = (leadData.getFirstName() + " " + leadData.getLastName()).trim();
              attachBankDetailsDTO.setAccountHolderName(fullName);
              return m2PWrapperApi
                  .fetchBankAccountDetails(leadId)
                  .collectMap(M2pBankDetailsResponseDTO::getAccountNumber)
                  .flatMap(
                      bankAccountDetails -> {
                        if (bankAccountDetails.containsKey(
                            attachBankDetailsDTO.getAccountNumber())) {
                          log.info("Processing existing bank account for loan ID: {}", loanId);
                          bankAccountDetails
                              .get(attachBankDetailsDTO.getAccountNumber())
                              .setAccountHolderName(fullName);
                          return processExistingBankAccount(
                              bankAccountDetails,
                              attachBankDetailsDTO,
                              loanId,
                              leadId,
                              productCode,
                              pennyDropFlag,
                              flowData);
                        } else {
                          log.info("Adding new bank account for loan ID: {}", loanId);
                          return addNewBankAccount(
                              leadId,
                              attachBankDetailsDTO,
                              loanId,
                              productCode,
                              pennyDropFlag,
                              flowData);
                        }
                      })
                  .doOnNext(bankId -> log.info("Bank ID: {}", bankId));
            });
  }

  /** Processes an existing bank account for a self beneficiary. * */
  private Mono<M2pAddBankDetailsResponseDTO> processExistingBankAccount(
      Map<String, M2pBankDetailsResponseDTO> bankAccountDetails,
      AttachBankDetailsDTO attachBankDetailsDTO,
      String loanId,
      String leadId,
      String productCode,
      boolean pennyDropFlag,
      ProductControl.Flow flowData) {

    M2pBankDetailsResponseDTO bankAccDetails =
        bankAccountDetails.get(attachBankDetailsDTO.getAccountNumber());
    LoanBankAccountDataTableDTO bankAccountDataTableRequest =
        buildLoanBankAccountDataTableDTO(bankAccDetails, attachBankDetailsDTO);
    String bankId = String.valueOf(bankAccDetails.getBankId());
    if (pennyDropFlag) {
      return verifyAndAddBankDetails(
          bankAccountDataTableRequest, loanId, leadId, productCode, bankId, flowData);
    } else {
      return addBankDetailsDataTable(bankAccountDataTableRequest, loanId, flowData)
          .flatMap(
              response ->
                  response != null
                      ? Mono.just(new M2pAddBankDetailsResponseDTO(bankId, null, null))
                      : Mono.error(
                          new RuntimeException("Failed to add bank details to the data table")));
    }
  }

  /** Adds a new bank account for a self beneficiary. * */
  private Mono<M2pAddBankDetailsResponseDTO> addNewBankAccount(
      String leadId,
      AttachBankDetailsDTO attachBankDetailsDTO,
      String loanId,
      String productCode,
      boolean pennyDropFlag,
      ProductControl.Flow flowData) {

    M2pBankDetailsRequestDTO m2pRequestDTO = buildM2pBankDetailsRequestDTO(attachBankDetailsDTO);
    return m2PWrapperApi
        .addBankAccountDetails(leadId, m2pRequestDTO)
        .flatMap(
            response -> {
              if (response != null && response.bankAccountDetailsId() != null) {
                LoanBankAccountDataTableDTO bankAccountDataTableRequest =
                    buildLoanBankAccountDataTableDTO(
                        response.bankAccountDetailsId(), attachBankDetailsDTO);
                if (pennyDropFlag) {
                  return verifyAndAddBankDetails(
                      bankAccountDataTableRequest,
                      loanId,
                      leadId,
                      productCode,
                      response.bankAccountDetailsId(),
                      flowData);
                } else {
                  return addBankDetailsDataTable(bankAccountDataTableRequest, loanId, flowData)
                      .flatMap(
                          res ->
                              res != null
                                  ? Mono.just(
                                      new M2pAddBankDetailsResponseDTO(
                                          response.bankAccountDetailsId(), null, null))
                                  : Mono.error(
                                      new RuntimeException(
                                          "Failed to add new bank details to the data"
                                              + " table")));
                }

              } else {
                log.error("Failed to add bank account for lead ID: {}", leadId);
                return Mono.error(
                    new RuntimeException(
                        "Failed to add bank account, response did not contain bank ID"));
              }
            });
  }

  /** Verifies the bank details using the Transaction API. */
  public Mono<BankVerificationResponseDTO> verifyBank(
      LoanBankAccountDataTableDTO bankAccountDataTableRequest,
      String leadId,
      String productCode,
      String bankId,
      String loanId) {
    log.info("Verifying bank details for lead ID: {}", leadId);
    BankVerificationDetailsDTO requestBody = new BankVerificationDetailsDTO();
    requestBody.setAccountNumber(bankAccountDataTableRequest.getBankAccountNumber());
    requestBody.setIfscCode(bankAccountDataTableRequest.getIfscCode());
    requestBody.setName(bankAccountDataTableRequest.getAccountHolderName());
    requestBody.setAccountType(bankAccountDataTableRequest.getAccountType());
    requestBody.setBankAccountId(bankId);
    return transactionApi.verifyBank(requestBody, leadId, productCode, loanId);
  }

  /**
   * Adds a client bank account. First checks if the bank account already exists for the client. If
   * it exists, returns the existing bankId. If not, adds a new bank account and returns the new
   * bankId.
   *
   * @param leadId The client/lead ID
   * @param nachMandateRequest The NACH mandate request containing bank details
   * @return Mono containing the bankId (existing or newly created)
   */
  public Mono<String> addClientBankAccount(String leadId, NachMandateRequest nachMandateRequest) {
    log.info(
        "[ADD_CLIENT_BANK_ACCOUNT] Checking if bank account exists for leadId: {}, accountNumber:"
            + " {}",
        leadId,
        nachMandateRequest.getBankAccountNumber());

    return m2PWrapperApi
        .fetchBankAccountDetails(leadId)
        .collectMap(M2pBankDetailsResponseDTO::getAccountNumber)
        .flatMap(
            bankAccountDetails -> {
              if (bankAccountDetails.containsKey(nachMandateRequest.getBankAccountNumber())) {
                M2pBankDetailsResponseDTO existingBank =
                    bankAccountDetails.get(nachMandateRequest.getBankAccountNumber());
                String bankId = String.valueOf(existingBank.getBankId());
                log.info(
                    "[ADD_CLIENT_BANK_ACCOUNT] Bank account already exists for leadId: {},"
                        + " returning existing bankId: {}",
                    leadId,
                    bankId);
                return Mono.just(bankId);
              } else {
                log.info(
                    "[ADD_CLIENT_BANK_ACCOUNT] Bank account not found, adding new bank account for"
                        + " leadId: {}",
                    leadId);
                M2pBankDetailsRequestDTO m2pRequestDTO =
                    buildM2pBankDetailsRequestDTO(nachMandateRequest);
                return m2PWrapperApi
                    .addBankAccountDetails(leadId, m2pRequestDTO)
                    .flatMap(
                        response -> {
                          if (response != null && response.bankAccountDetailsId() != null) {
                            log.info(
                                "[ADD_CLIENT_BANK_ACCOUNT] Successfully added bank account for"
                                    + " leadId: {}, new bankId: {}",
                                leadId,
                                response.bankAccountDetailsId());
                            return Mono.just(response.bankAccountDetailsId());
                          } else {
                            log.error(
                                "[ADD_CLIENT_BANK_ACCOUNT] Failed to add bank account for leadId:"
                                    + " {}",
                                leadId);
                            return Mono.error(
                                new ServerErrorException(
                                    "Failed to add bank account, response did not contain bank ID",
                                    null,
                                    HttpStatus.INTERNAL_SERVER_ERROR));
                          }
                        });
              }
            });
  }

  /** Builds a LoanBankAccountDataTableDTO object from bank ID and attach bank details. */
  private LoanBankAccountDataTableDTO buildLoanBankAccountDataTableDTO(
      String bankId, AttachBankDetailsDTO attachBankDetailsDTO) {

    return LoanBankAccountDataTableDTO.builder()
        .bankId(bankId)
        .bankAccountNumber(attachBankDetailsDTO.getAccountNumber())
        .bankName(attachBankDetailsDTO.getBankName())
        .ifscCode(attachBankDetailsDTO.getIfscCode())
        .accountHolderName(attachBankDetailsDTO.getAccountHolderName())
        .accountType(attachBankDetailsDTO.getBankAccountType())
        .beneficiaryType(attachBankDetailsDTO.getBeneficiaryType())
        .build();
  }

  /**
   * Builds a LoanBankAccountDataTableDTO object from bank details response and attach bank details.
   */
  private LoanBankAccountDataTableDTO buildLoanBankAccountDataTableDTO(
      M2pBankDetailsResponseDTO bankAccDetails, AttachBankDetailsDTO attachBankDetailsDTO) {

    return LoanBankAccountDataTableDTO.builder()
        .bankId(String.valueOf(bankAccDetails.getBankId()))
        .bankAccountNumber(bankAccDetails.getAccountNumber())
        .bankName(bankAccDetails.getBankName())
        .ifscCode(bankAccDetails.getIfscCode())
        .accountHolderName(bankAccDetails.getAccountHolderName())
        .accountType(
            bankAccDetails.getAccountType() != null
                ? bankAccDetails.getAccountType().getValue()
                : null)
        .beneficiaryType(attachBankDetailsDTO.getBeneficiaryType())
        .build();
  }

  /** Builds a M2pBankDetailsRequestDTO object from attach bank details. */
  private M2pBankDetailsRequestDTO buildM2pBankDetailsRequestDTO(
      AttachBankDetailsDTO attachBankDetailsDTO) {
    return BankDetailsDTO.builder()
        .accountNumber(attachBankDetailsDTO.getAccountNumber())
        .accountType(
            String.valueOf(
                BankAccountType.valueOf(attachBankDetailsDTO.getBankAccountType()).getId()))
        .name(attachBankDetailsDTO.getAccountHolderName())
        .ifscCode(attachBankDetailsDTO.getIfscCode())
        .supportedForRepayment(false)
        .supportedForDisbursement(true)
        .build()
        .getM2pRequestDTO();
  }

  private M2pBankDetailsRequestDTO buildM2pBankDetailsRequestDTO(
      NachMandateRequest nachMandateRequest) {
    return BankDetailsDTO.builder()
        .accountNumber(nachMandateRequest.getBankAccountNumber())
        .accountType(
            String.valueOf(
                BankAccountType.valueOf(nachMandateRequest.getBankAccountType()).getId()))
        .name(nachMandateRequest.getBankAccountHolderName())
        .ifscCode(nachMandateRequest.getIfsc())
        .supportedForRepayment(true)
        .supportedForDisbursement(false)
        .build()
        .getM2pRequestDTO();
  }

  /** Handles the attachment of a bank account for a third-party beneficiary. */
  private Mono<M2pAddBankDetailsResponseDTO> handleThirdPartyBeneficiary(
      String loanId,
      String leadId,
      AttachBankDetailsDTO attachBankDetailsDTO,
      String productCode,
      boolean pennyDropFlag,
      ProductControl.Flow flowData) {

    M2pBeneficiaryBankDetailsDTO m2pBeneficiaryBankDetailsDTO =
        buildM2pBeneficiaryBankDetailsDTO(attachBankDetailsDTO);

    return m2PWrapperApi
        .getMappedBeneficiaryBankId(leadId)
        .collectMap(
            response -> response.getBankAccountDetails().getAccountNumber(), response -> response)
        .flatMap(
            beneficiaryAccountDetails -> {
              if (beneficiaryAccountDetails.isEmpty()
                  || !beneficiaryAccountDetails.containsKey(
                      attachBankDetailsDTO.getAccountNumber())) {
                log.info("Adding new beneficiary bank account for loan ID: {}", loanId);
                return addNewBeneficiaryBankAccount(
                    leadId,
                    m2pBeneficiaryBankDetailsDTO,
                    attachBankDetailsDTO,
                    loanId,
                    productCode,
                    flowData);
              } else {
                log.info("Processing existing beneficiary bank account for loan ID: {}", loanId);
                return processExistingBeneficiaryBankAccount(
                    beneficiaryAccountDetails,
                    attachBankDetailsDTO,
                    loanId,
                    leadId,
                    productCode,
                    pennyDropFlag,
                    flowData);
              }
            });
  }

  /** Builds a M2pBeneficiaryBankDetailsDTO object from attach bank details. */
  private M2pBeneficiaryBankDetailsDTO buildM2pBeneficiaryBankDetailsDTO(
      AttachBankDetailsDTO attachBankDetailsDTO) {
    return M2pBeneficiaryBankDetailsDTO.builder()
        .name(attachBankDetailsDTO.getAccountHolderName())
        .accountNumber(attachBankDetailsDTO.getAccountNumber())
        .ifscCode(attachBankDetailsDTO.getIfscCode())
        .accountTypeId(BankAccountType.valueOf(attachBankDetailsDTO.getBankAccountType()).getId())
        .locale("en")
        .bankName(attachBankDetailsDTO.getBankName())
        .branchName(attachBankDetailsDTO.getBankName())
        .bankCity("")
        .mobileNumber("")
        .build();
  }

  /** Adds a new bank account for a third-party beneficiary. */
  private Mono<M2pAddBankDetailsResponseDTO> addNewBeneficiaryBankAccount(
      String leadId,
      M2pBeneficiaryBankDetailsDTO m2pBeneficiaryBankDetailsDTO,
      AttachBankDetailsDTO attachBankDetailsDTO,
      String loanId,
      String productCode,
      ProductControl.Flow flowData) {

    return m2PWrapperApi
        .addBeneficiaryBankAccountDetails(leadId, m2pBeneficiaryBankDetailsDTO)
        .flatMap(
            addBeneficiaryResponse -> {
              if (addBeneficiaryResponse != null
                  && addBeneficiaryResponse.getClientThirdPartyBankAccountDetailId() != null
                  && !addBeneficiaryResponse.isAccountAlreadyActive()) {
                log.info("Activating new beneficiary bank account for loan ID: {}", loanId);
                return activateBeneficiaryBankAccount(
                    addBeneficiaryResponse.getClientThirdPartyBankAccountDetailId(),
                    addBeneficiaryResponse.getClientThirdPartyBankAccountDetailAssociationId(),
                    m2pBeneficiaryBankDetailsDTO,
                    attachBankDetailsDTO,
                    loanId,
                    leadId,
                    productCode,
                    flowData);
              }
              log.error("Failed to add beneficiary bank account for lead ID: {}", leadId);
              return Mono.error(
                  new RuntimeException(
                      "Failed to add bank account, response did not contain bank ID"));
            });
  }

  /** Activates a beneficiary bank account and verifies it. * */
  private Mono<M2pAddBankDetailsResponseDTO> activateBeneficiaryBankAccount(
      String bankAccountDetailId,
      String thirdPartyBankAccountAssociationId,
      M2pBeneficiaryBankDetailsDTO m2pBeneficiaryBankDetailsDTO,
      AttachBankDetailsDTO attachBankDetailsDTO,
      String loanId,
      String leadId,
      String productCode,
      ProductControl.Flow flowData) {
    boolean pennyDropFlag =
        flowData.getConditions() != null
            && (boolean) flowData.getConditions().getOrDefault("pennyDropFlag", false);
    return m2PWrapperApi
        .activateBeneficiaryBankAccount(thirdPartyBankAccountAssociationId, loanId, leadId)
        .flatMap(
            activateBeneficiaryResponse -> {
              LoanBankAccountDataTableDTO bankAccountDataTableRequest =
                  buildLoanBankAccountDataTableDTO(
                      bankAccountDetailId, m2pBeneficiaryBankDetailsDTO, attachBankDetailsDTO);
              if (pennyDropFlag) {
                return verifyAndAddBankDetails(
                    bankAccountDataTableRequest,
                    loanId,
                    leadId,
                    productCode,
                    bankAccountDetailId,
                    flowData);
              } else {
                return addBankDetailsDataTable(bankAccountDataTableRequest, loanId, flowData)
                    .flatMap(
                        res ->
                            Mono.just(
                                new M2pAddBankDetailsResponseDTO(bankAccountDetailId, null, null)));
              }
            });
  }

  /**
   * Builds a LoanBankAccountDataTableDTO object from bank account detail ID and attach bank
   * details.
   */
  private LoanBankAccountDataTableDTO buildLoanBankAccountDataTableDTO(
      String bankAccountDetailId,
      M2pBeneficiaryBankDetailsDTO m2pBeneficiaryBankDetailsDTO,
      AttachBankDetailsDTO attachBankDetailsDTO) {

    return LoanBankAccountDataTableDTO.builder()
        .bankId(bankAccountDetailId)
        .bankAccountNumber(m2pBeneficiaryBankDetailsDTO.getAccountNumber())
        .bankName(m2pBeneficiaryBankDetailsDTO.getBankName())
        .ifscCode(m2pBeneficiaryBankDetailsDTO.getIfscCode())
        .accountHolderName(m2pBeneficiaryBankDetailsDTO.getName())
        .accountType(attachBankDetailsDTO.getBankAccountType())
        .beneficiaryType(attachBankDetailsDTO.getBeneficiaryType())
        .build();
  }

  /** Processes an existing beneficiary bank account. * */
  private Mono<M2pAddBankDetailsResponseDTO> processExistingBeneficiaryBankAccount(
      Map<String, M2pGetBeneficiaryBankDetailsResponse> beneficiaryAccountDetails,
      AttachBankDetailsDTO attachBankDetailsDTO,
      String loanId,
      String leadId,
      String productCode,
      boolean pennyDropFlag,
      ProductControl.Flow flowData) {

    M2pGetBeneficiaryBankDetailsResponse m2pGetBeneficiaryBankDetailsResponse =
        beneficiaryAccountDetails.get(attachBankDetailsDTO.getAccountNumber());
    LoanBankAccountDataTableDTO bankAccountDataTableRequest =
        buildLoanBankAccountDataTableDTO(
            m2pGetBeneficiaryBankDetailsResponse, attachBankDetailsDTO);
    String bankId = m2pGetBeneficiaryBankDetailsResponse.getBankAccountDetails().getId();
    if (pennyDropFlag) {
      return verifyAndAddBankDetails(
          bankAccountDataTableRequest, loanId, leadId, productCode, bankId, flowData);
    } else {
      return addBankDetailsDataTable(bankAccountDataTableRequest, loanId, flowData)
          .flatMap(res -> Mono.just(new M2pAddBankDetailsResponseDTO(bankId, null, null)));
    }
  }

  /**
   * Builds a LoanBankAccountDataTableDTO object from beneficiary bank details response and attach
   * bank details.
   */
  private LoanBankAccountDataTableDTO buildLoanBankAccountDataTableDTO(
      M2pGetBeneficiaryBankDetailsResponse m2pGetBeneficiaryBankDetailsResponse,
      AttachBankDetailsDTO attachBankDetailsDTO) {

    return LoanBankAccountDataTableDTO.builder()
        .bankId(m2pGetBeneficiaryBankDetailsResponse.getBankAccountDetails().getId())
        .bankAccountNumber(
            m2pGetBeneficiaryBankDetailsResponse.getBankAccountDetails().getAccountNumber())
        .bankName(m2pGetBeneficiaryBankDetailsResponse.getBankAccountDetails().getBankName())
        .ifscCode(m2pGetBeneficiaryBankDetailsResponse.getBankAccountDetails().getIfscCode())
        .accountHolderName(m2pGetBeneficiaryBankDetailsResponse.getBankAccountDetails().getName())
        .accountType(attachBankDetailsDTO.getBankAccountType())
        .beneficiaryType(attachBankDetailsDTO.getBeneficiaryType())
        .build();
  }

  /** Performs penny drop. */
  private Mono<M2pAddBankDetailsResponseDTO> verifyAndAddBankDetails(
      LoanBankAccountDataTableDTO bankAccountDataTableRequest,
      String loanId,
      String leadId,
      String productCode,
      String bankId,
      ProductControl.Flow flowData) {
    boolean verificationFlag =
        (boolean) flowData.getConditions().getOrDefault("verificationFlag", false);
    return verifyBank(bankAccountDataTableRequest, leadId, productCode, bankId, loanId)
        .flatMap(
            verificationResponse -> {
              if (verificationFlag) {
                return addBankDetailsDataTable(bankAccountDataTableRequest, loanId, flowData)
                    .map(response -> new M2pAddBankDetailsResponseDTO(bankId, null, null));
              }
              boolean isVerified =
                  PASS.equalsIgnoreCase(verificationResponse.getBankVerificationStatus());
              bankAccountDataTableRequest.setBankVerified(isVerified ? "Yes" : "No");
              String errorMessage = getErrorMessage(verificationResponse);
              if (isVerified) {
                return addBankDetailsDataTable(bankAccountDataTableRequest, loanId, flowData)
                    .map(response -> new M2pAddBankDetailsResponseDTO(bankId, "Yes", errorMessage));
              } else {
                log.error(BANK_VERIFICATION_FAILED, loanId);
                return Mono.just(new M2pAddBankDetailsResponseDTO(bankId, "No", errorMessage));
              }
            })
        .onErrorResume(
            error -> {
              log.error("Bank verification failed: {}", error.getMessage());
              if (verificationFlag) {
                return addBankDetailsDataTable(bankAccountDataTableRequest, loanId, flowData)
                    .map(response -> new M2pAddBankDetailsResponseDTO(bankId, null, null));
              }
              return Mono.just(
                  new M2pAddBankDetailsResponseDTO(bankId, "Fail", extractErrorMessage(error)));
            });
  }

  /** Adds bank details to the data table and triggers CTA if necessary. */
  private Mono<?> addBankDetailsDataTable(
      LoanBankAccountDataTableDTO loanBankAccountDataTableDTO,
      String loanId,
      ProductControl.Flow flowData) {

    return m2PWrapperApi
        .addBankDetailsDataTable(loanBankAccountDataTableDTO, loanId)
        .flatMap(
            data -> {
              if (flowData.isCtaCallFlag()) {
                log.info(
                    "[{}] triggering attach loan bank account CTA: {}",
                    ATTACH_BANK_ACCOUNT_LOAN,
                    loanId);
                return m2PWrapperApi.registerCta(loanId, flowData.getCtaName()).thenReturn(data);
              } else {
                log.info(
                    "[{}] attach loan bank account CTA cancelled: {}",
                    ATTACH_BANK_ACCOUNT_LOAN,
                    loanId);
                return Mono.just(data);
              }
            });
  }

  /** Retrieves the error message based on the verification response. */
  public String getErrorMessage(BankVerificationResponseDTO verificationResponse) {
    if (FAIL.equalsIgnoreCase(verificationResponse.getBankVerificationStatus())) {
      BankVerificationResponseDTO.PennyDropDTO pennyDrop =
          verificationResponse.getData().getPennyDrop();
      if (pennyDrop != null && Boolean.FALSE.equals(pennyDrop.getIsValid())) {
        log.error("Penny drop verification failed: {}", pennyDrop.getBankResponse());
        return pennyDrop.getBankResponse();
      } else {
        BankVerificationResponseDTO.NameMatchPercentageDTO nameMatch =
            verificationResponse.getData().getNameMatchPercentage();
        if (nameMatch != null && Boolean.FALSE.equals(nameMatch.getIsValid())) {
          log.error("Name match percentage verification failed.");
          return "NAME MATCH PERCENTAGE IS BELOW THRESHOLD.";
        }
      }
      return "PENNY DROP VERIFICATION FAILED.";
    }
    return "";
  }

  /** Retrieves the error message based on the error. */
  private String extractErrorMessage(Throwable error) {
    String errorMessage = "Unknown error";

    if (error instanceof ClientSideException clientException) {
      try {
        String responseBody = new Gson().toJson(clientException.getResponseBody());
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        errorMessage =
            jsonObject.has(MESSAGE) ? jsonObject.get(MESSAGE).getAsString() : errorMessage;
      } catch (JsonSyntaxException | IllegalStateException | ClassCastException e) {
        log.warn("Failed to parse error response body");
      }
    } else if (error instanceof ServerErrorException serverException) {
      try {
        String responseBody = new Gson().toJson(serverException.getClientResponse());
        JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
        errorMessage =
            jsonObject.has(MESSAGE) ? jsonObject.get(MESSAGE).getAsString() : errorMessage;
      } catch (JsonSyntaxException | IllegalStateException | ClassCastException e) {
        log.warn("Failed to parse server-side error response body");
      }
    } else {
      errorMessage = error.getMessage();
    }

    return errorMessage;
  }
}
