package com.trillionloans.los.util;

import static com.trillionloans.los.constant.StringConstants.DATE_FORMAT;
import static com.trillionloans.los.validation.RequestValidation.validateM2pTopUpLoanApplicationRequest;

import com.trillionloans.los.model.dto.LoanChargesDTO;
import com.trillionloans.los.model.partner.m2p.M2PLoanApplicationRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pLoanApplicationAssociationsDTO;
import com.trillionloans.los.model.partner.m2p.M2pLoanApplicationTermsDTO;
import com.trillionloans.los.model.partner.m2p.M2pNachMandateRequestDTO;
import com.trillionloans.los.model.partner.m2p.M2pVehicleDetailsDTO;
import com.trillionloans.los.model.request.LoanApplication;
import com.trillionloans.los.model.request.NachMandateRequest;
import com.trillionloans.los.model.request.VehicleDetailsRequest;
import com.trillionloans.los.model.response.m2p.M2PTopUpLoanApplicationRequestDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanDataUtil {
  static String dateFormat = "dd-MM-yyyy";
  static String locale = "en";

  /** Set of product codes that are classified as Credit Line products. */
  private static final Set<String> CREDIT_LINE_PRODUCT_CODES = Set.of("FUND", "KCL");

  public static final String CREDIT_LINE_KCL = "KCL";

  /**
   * Checks if the given product code is a Credit Line product.
   *
   * @param productCode the product code to check
   * @return true if the product is a Credit Line product, false otherwise
   */
  public static boolean isCreditLineProduct(String productCode) {
    if (productCode == null) {
      return false;
    }
    return CREDIT_LINE_PRODUCT_CODES.contains(productCode.toUpperCase());
  }

  /**
   * Determines the type of loan request to be created.
   *
   * @param loanData the loan application data
   * @param productCode the product code
   * @return the loan request type as a string
   */
  public static LoanRequestType determineLoanRequestType(
      LoanApplication loanData, String productCode) {
    if (isCreditLineProduct(productCode)) {
      return LoanRequestType.CREDIT_LINE;
    }
    if (Boolean.TRUE.equals(loanData.getIsTopup())) {
      return LoanRequestType.TOP_UP;
    }
    return LoanRequestType.NORMAL;
  }

  /** Enum representing the different types of loan requests. */
  public enum LoanRequestType {
    CREDIT_LINE,
    TOP_UP,
    NORMAL
  }

  public static M2pNachMandateRequestDTO getM2pNachMandate(NachMandateRequest nachMandateRequest) {
    return M2pNachMandateRequestDTO.builder()
        .status(nachMandateRequest.getStatus())
        .umrn(nachMandateRequest.getUmrn())
        .bankName(nachMandateRequest.getBankName())
        .bankAccountType(nachMandateRequest.getBankAccountType())
        .bankAccountHolderName(nachMandateRequest.getBankAccountHolderName())
        .branchName(nachMandateRequest.getBranchName())
        .bankAccountNumber(nachMandateRequest.getBankAccountNumber())
        .micr(nachMandateRequest.getMicr())
        .ifsc(nachMandateRequest.getIfsc())
        .mandateRegistrationRequestedDate(nachMandateRequest.getMandateRegistrationRequestedDate())
        .dateFormat(DATE_FORMAT)
        .periodStartDate(nachMandateRequest.getPeriodStartDate())
        .periodEndDate(nachMandateRequest.getPeriodEndDate())
        .periodUntilCancelled(nachMandateRequest.getPeriodUntilCancelled())
        .debitTypeEnum(nachMandateRequest.getDebitTypeEnum())
        .debitFrequencyEnum(nachMandateRequest.getDebitFrequencyEnum())
        .amount(nachMandateRequest.getAmount())
        .externalReferenceNumber(nachMandateRequest.getExternalReferenceNumber())
        .mode(nachMandateRequest.getMode())
        .build();
  }

  public static M2PLoanApplicationRequestDTO getM2pLoanApplicationRequestDTO(
      LoanApplication loanApplication) {
    return M2PLoanApplicationRequestDTO.builder()
        .loanOfficerId(loanApplication.getLoanPurposeId())
        .amount(loanApplication.getAmount())
        .losProductKey(loanApplication.getLosProductKey())
        .sourcingChannelId(loanApplication.getSourcingChannelId())
        .associations(
            M2pLoanApplicationAssociationsDTO.builder()
                .anchor(
                    Objects.isNull(loanApplication.getAssociations())
                        ? null
                        : loanApplication.getAssociations().getAnchor())
                .build())
        .leadApplicationTerms(
            M2pLoanApplicationTermsDTO.builder()
                .maxEligibleAmount(loanApplication.getLeadApplicationTerms().getMaxEligibleAmount())
                .repaymentsStartingFromDate(
                    loanApplication.getLeadApplicationTerms().getRepaymentsStartingFromDate())
                .numberOfRepayments(
                    loanApplication.getLeadApplicationTerms().getNumberOfRepayments())
                .repayEvery(loanApplication.getLeadApplicationTerms().getRepayEvery())
                .repaymentPeriodFrequencyEnum(
                    loanApplication.getLeadApplicationTerms().getRepaymentPeriodFrequencyEnum())
                .termPeriodFrequencyEnum(
                    loanApplication.getLeadApplicationTerms().getTermPeriodFrequencyEnum())
                .termFrequency(loanApplication.getLeadApplicationTerms().getTermFrequency())
                .interestRatePerPeriod(
                    loanApplication.getLeadApplicationTerms().getInterestRatePerPeriod())
                .dateFormat(DATE_FORMAT)
                .graceOnPrincipalPayment(
                    loanApplication.getLeadApplicationTerms().getGraceOnPrincipalPayment())
                .graceOnInterestCharged(
                    loanApplication.getLeadApplicationTerms().getGraceOnInterestCharged())
                .amountForUpfrontCollection(
                    loanApplication.getLeadApplicationTerms().getAmountForUpfrontCollection())
                .build())
        .charges(loanApplication.getCharges())
        .externalIdOne(loanApplication.getExternalId())
        .build();
  }

  public static M2PLoanApplicationRequestDTO buildCreditLineM2pLoanRequest(
      LoanApplication loanApplication) {

    return M2PLoanApplicationRequestDTO.builder()
        .externalIdOne(loanApplication.getExternalId())
        .losProductKey(loanApplication.getLosProductKey())
        .amount(null)
        .leadApplicationTerms(null)
        .charges(null)
        .associations(null)
        .loanOfficerId(null)
        .sourcingChannelId(null)
        .build();
  }

  public static M2PTopUpLoanApplicationRequestDTO getM2pTopUpLoanApplicationRequestDTO(
      LoanApplication loanApplication, String leadId) {

    validateM2pTopUpLoanApplicationRequest(loanApplication);

    List<LoanChargesDTO> totalCharges = new ArrayList<>();

    if (loanApplication.getCharges() != null) {
      totalCharges.addAll(loanApplication.getCharges());
    }
    // adding default charge
    LoanChargesDTO defaultLoanCharge = LoanChargesDTO.builder().chargeId(7).amount(0.00).build();
    totalCharges.add(defaultLoanCharge);
    return M2PTopUpLoanApplicationRequestDTO.builder()
        .clientId(Integer.valueOf(leadId))
        .locale(locale)
        .dateFormat(dateFormat)
        .isTopup(loanApplication.getIsTopup())
        .loanIdToClose(loanApplication.getLoanIdToClose())
        .loanSubType("RENEWAL")
        .amount(loanApplication.getAmount())
        .charges(totalCharges)
        .losProductKey(loanApplication.getLosProductKey())
        .leadApplicationTerms(loanApplication.getLeadApplicationTerms())
        .build();
  }

  public static M2pVehicleDetailsDTO getM2pVehicleDetailsRequestDTO(
      VehicleDetailsRequest vehicleDetailsRequest) {
    return M2pVehicleDetailsDTO.builder()
        .additionalDetailsFormKey("new_vehicle_basic_details")
        .miscellaneousFee(vehicleDetailsRequest.getRtoFee())
        .insuranceFee(vehicleDetailsRequest.getInsuranceFee())
        .vehicleRegistrationCost(vehicleDetailsRequest.getVehicleRegistrationCost())
        .standardFitmentAccessoriesChagres(
            vehicleDetailsRequest.getStandardFitmentAccessoriesChagres())
        .vehicleAdditionalDetails(
            M2pVehicleDetailsDTO.VehicleAdditionalDetails.builder()
                .merchantName(vehicleDetailsRequest.getMerchantName())
                .downPaymentPercentage(vehicleDetailsRequest.getDownPaymentPercentage())
                .brand(vehicleDetailsRequest.getBrand())
                .model(vehicleDetailsRequest.getModel())
                .insuranceFee(vehicleDetailsRequest.getInsuranceFee())
                .rtoFee(vehicleDetailsRequest.getRtoFee())
                .vehicleRegistrationCost(vehicleDetailsRequest.getVehicleRegistrationCost())
                .standardFitmentAccessoriesChagres(
                    vehicleDetailsRequest.getStandardFitmentAccessoriesChagres())
                .tenure(vehicleDetailsRequest.getTenure())
                .interestRate(vehicleDetailsRequest.getInterestRate())
                .repaymentStartDate(vehicleDetailsRequest.getRepaymentStartDate())
                .expectedDisbursementDate(vehicleDetailsRequest.getExpectedDisbursementDate())
                .isRoyalEnfield(vehicleDetailsRequest.getIsRoyalEnfield())
                .onRoadPrice(vehicleDetailsRequest.getOnRoadPrice())
                .isFieldInvestigationDone(vehicleDetailsRequest.getIsFieldInvestigationDone())
                .isEv(vehicleDetailsRequest.getIsEv())
                .ltv(vehicleDetailsRequest.getLtv())
                .dateOfManufacturing(vehicleDetailsRequest.getDateOfManufacturing())
                .build())
        .showroomPrice(vehicleDetailsRequest.getShowroomPrice())
        .isNew(vehicleDetailsRequest.getIsNew())
        .build();
  }
}
