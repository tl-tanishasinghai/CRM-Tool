package com.trillionloans.los.constant;

public class StringConstants {
  private StringConstants() {}

  // error strings, sent as response to clients & vendors
  public static final String SOMETHING_WENT_WRONG = "something went wrong, please try again";
  public static final String SOMETHING_WENT_WRONG_CONFIG =
      "something went wrong, please try again - config";
  public static final String SOMETHING_WENT_WRONG_CACHE =
      "something went wrong, please try again - cache";
  public static final String SERVER_ERROR = "server error";
  public static final String CLIENT_ERROR = "client error";
  public static final String FORBIDDEN_ERROR = "forbidden error";
  public static final String LOAN_CREATION_FAIL = "loan creation failed";
  public static final String KYC_FAIL = "kyc failed";
  public static final String OKYC_FAIL = "o-kyc failed";
  public static final String CKYC_FAIL = "c-kyc failed";
  public static final String CAFFEINE_REMOVE_MSG = "key removed from caffeine cache";
  public static final String CAFFEINE_ALL_REMOVE_MSG = "all keys removed from caffeine cache";
  public static final String REDIS_REMOVE_MSG = "key removed from redis cache";
  public static final String REDIS_ALL_REMOVE_MSG = "all keys removed from redis cache";
  public static final String NOT_FOUND = "not found";
  public static final String VEHICLE_DETAILS_FAIL = "vehicle details stamping failed";
  public static final String BRE_FAIL = "BRE Failed";
  public static final String BRE_OFFER_EXPIRED = "BRE Offer Expired";
  public static final String PENNY_DROP_NAME_MATCH_REJECTION = "Penny Drop Name Match Rejection";
  public static final String BRE_EXPIRY_IDENTIFIER = "BRE_OFFER_EXPIRY";
  public static final String RISK_PARAMETERS_NOT_FOUND =
      "Risk parameters not found for the loan application";
  public static final String TIMEOUT_ERROR = "did not observe any item or terminal signal";

  // logging literals used for unified logs in the application
  public static final String LOGGING_LITERAL = "[{}] request to {}: {}";
  public static final String LOGGING_LITERAL_URI = "[{}] request to {} (uri): {}";
  public static final String LOGGING_LITERAL_RETRY = "[{}] retrying request {}, retry count: {}";
  public static final String LOGGING_ERROR_RESPONSE =
      "[{}] [ERROR] status code from {}: {} response: {}";
  public static final String LOGGING_RESPONSE = "[{}] response from {}: {}";
  public static final String UPDATE_LOAN_LOG_HEADER = "UPDATE_LOAN";
  public static final String CAFFEINE_OPS = "CAFFEINE_OPS";
  public static final String REDIS_OPS = "REDIS_OPS";
  public static final String REQUEST_LOG = "REQUEST_LOG";
  public static final String TRACE_ID = "traceId";
  public static final String LOGGER_HEADER = "loggerHeader";
  public static final String UPLOAD_DOC_LOAN = "UPLOAD_DOC_LOAN";
  public static final String UPLOAD_NACH_LOAN = "UPLOAD_NACH_LOAN";
  public static final String PARTNER_MASTER_OPERATION = "PARTNER_MASTER_OPERATION";
  public static final String BRE = "BRE_";
  public static final String ERROR_PROCESSING_JSON = "Error processing JSON";
  public static final String BRE_RESPONSE = "BRE_RESPONSE";
  public static final String ATTACH_BANK_ACCOUNT_LOAN = "ATTACH_BANK_ACCOUNT_LOAN";
  public static final String NO_DISBURSAL_BANK_ACCOUNT =
      "Disbursal bank account not attached for given loan!";
  public static final String BANK_NOT_MAPPED = "Loan Disbursal failed, bank account not mapped!";
  public static final String HTML_RESPONSE =
      "[{}] [ERROR] error body is not json, treating as plain text/xml/html. original exception:"
          + " {}";

  // [DO NOT ALTER]
  // identifiers for callback configurations fetch & callback logs persist
  public static final String CKYC_CALLBACK_IDENTIFIER = "CKYC_CB";
  public static final String MANUAL_KYC_CALLBACK_IDENTIFIER = "MANUAL_KYC_CB";
  public static final String REJECTION_CALLBACK_IDENTIFIER = "REJECTION_CB";
  public static final String CLOSURE_CALLBACK_IDENTIFIER = "CLOSURE_CB";
  public static final String FI_STATUS_CALLBACK_IDENTIFIER = "FI_CB";
  public static final String VEHICLE_DETAILS_POST_CTA_IDENTIFIER = "VEHICLE_DETAILS_CTA";
  public static final String KYC_CALLBACK_IDENTIFIER = "KYC_CB";
  public static final String DISB_CALLBACK_IDENTIFIER = "DISB_CB";
  public static final String OKYC_CALLBACK_IDENTIFIER = "OKYC_CB";
  public static final String E_SIGN_CALLBACK_IDENTIFIER = "E_SIGN_CB";
  public static final String LOAN_CREATE_CTA_IDENTIFIER = "LOAN_CREATE_CTA";
  public static final String UPLOAD_DOC_CTA_IDENTIFIER = "UPLOAD_DOC_CTA";
  public static final String UPLOAD_NACH_IDENTIFIER = "UPLOAD_NACH_CTA";
  public static final String TRIGGER_DISB_CTA_IDENTIFIER = "TRIGGER_DISB_CTA";
  public static final String BRE_CTA_IDENTIFIER = "BRE_CTA";
  public static final String START_KYC_CTA_IDENTIFIER = "START_KYC_CTA";
  public static final String BRE_CALLBACK_IDENTIFIER = "BRE_CB";
  public static final String BRE_IDENTIFIER = "BRE";
  public static final String FI_CTA_IDENTIFIER = "FI_CTA";
  public static final String UPDATE_LOAN_IDENTIFIER = "UPDATE_LOAN_CONDITION";
  public static final String OFFER_DOWN_CTA_IDENTIFIER = "offer-downgrade";
  public static final String SCIENAPTIC_CTA_IDENTIFIER = "scienaptic-status";
  public static final String CALLBACK_RETRY = "CB_RETRY";
  public static final String ATTACH_BANK_ACCOUNT_CTA_IDENTIFIER = "LOAN_BANK_ACCOUNT_CTA";
  public static final String CKYCR_CTA_IDENTIFIER = "CKYCR_CTA";
  public static final String LEAD_ACKNOWLEDGEMENT = "LEAD_ACKNOWLEDGEMENT";
  public static final String GET_LEAD = "GET_LEAD";
  public static final String BUSINESS_LOAN_CONFIG_IDENTIFIER = "BUSINESS_LOAN_CONFIG";

  // [DO NOT ALTER]
  // string literals for usages at various places in code
  public static final String PRODUCT_KEY_CB = "productkey";
  public static final String PRODUCT_CODE = "productCode";
  public static final String PARTNER_ID = "partnerId";
  public static final String PARTNER_NAME = "partnerName";
  public static final String EXTERNAL_ID = "externalId";
  public static final String VERIFIED = "VERIFIED";
  public static final String ACTIVE = "A";
  public static final String M2P_CTA_LITERAL = "m2p.cta.";
  public static final String SUCCESS = "SUCCESS";
  public static final String FAIL = "FAIL";
  public static final String PAN_NOT_MATCHED = "PAN_NOT_MATCHED";
  public static final String FILE_KEY = "fileKey";
  public static final String DD_MMMM_YYYY = "dd MMMM yyyy";
  public static final String EN = "en";
  public static final String INDIVIDUAL = "individual";
  public static final String OFFICE_MAPPING_NOT_FOUND =
      "Office mapping not found for given product";
  public static final String PRODUCT_MAPPING_NOT_FOUND =
      "Product mapping not found for given product";
  public static final String DD_MM_YYYY = "dd-MM-yyyy";
  public static final String BRE_PF_CHECK = "brePfCheckEnabled";
  public static final String BRE_ROI_CHECK = "breRoiCheckEnabled";
  public static final String BRE_AMT_CHECK = "breAmountCheckEnabled";
  public static final String BRE_TENURE_CHECK = "breTenureCheckEnabled";
  public static final String PROCESSING_FEE = "processingFees";
  public static final String AMOUNT = "amount";
  public static final String CREDIT_LINE_PRODUCT_CODE = "bharatpe";

  // [DO NOT ALTER]
  // status codes for BRE
  public static final String BRE_INITIATED = "BRE01";
  public static final String BRE_COMPLETED = "BRE02";
  public static final String DATE_FORMAT = "dd-MM-yyyy";

  // Bre Stages
  public static final String BUREAU_HARD_PULL = "BUREAU_HARD_PULL";
  public static final String SCIENAPTIC = "SCIENAPTIC";
  public static final String M2P_UPDATE = "M2P_UPDATE";
  public static final String INITIATED = "INITIATED";
  public static final String IN_PROGRESS = "IN PROGRESS";
  public static final String COMPLETED = "COMPLETED";
  public static final String BRE_STATUS = "BRE_STATUS";
  public static final String ACTION = "action";
  public static final String LOANS = "loans";
  public static final String LIMIT = "limit";
  public static final String FINAL_LIMIT = "final_limit";
  public static final String TENURE = "tenure";
  public static final String ROI = "roi";
  public static final String REASONS = "reasons";
  public static final String TRUE = "true";
  public static final String ELIGIBLE = "Eligible";
  public static final String INELIGIBLE = "Ineligible";
  public static final String APPROVED = "Approved";
  public static final String APPROVED_FI = "Approved subject to FI";
  public static final String REJECTED = "Rejected";
  public static final String DECLINED = "Declined";
  public static final String LOANID = "loanId";
  public static final String LOAN_AMOUNT = "loan_amount";
  public static final String LOAN_TENURE = "loan_tenure";
  public static final String SINGLE_BRE = "SINGLE";
  public static final String MULTIPLE_BRE = "MULTIPLE";
  public static final String LIMIT_BRE = "LIMIT";

  // BRE Reasons
  public static final String TENURE_NOT_FOUNT = "Tenure not found in approved loans";
  public static final String REQUESTED_AMT_GREATER_THAN_APPROVED_AMT =
      "Requested loan amount is greater than approved limit";
  public static final String NO_BRE_RESPONSE =
      "no approved bre response found for this loan application";
  public static final String AMOUNT_EXCEEDS_APPROVED =
      "the amount in request body exceeds amount in approved bre response";
  public static final String INVALID_VALIDATION_PARAMETER = "invalid validation parameter";
  public static final String NO_AMOUNT_IN_BRE = "no amount found in approved bre response";
  public static final String AMOUNT_MISMATCH_BRE =
      "the amount in request body does not match with amount in approved bre response";
  public static final String RISK_DEDUPE_ERROR =
      "Loan application rejected basis Risk dedupe framework";
  public static final String FUNNELS_OFF_ERROR = "Request not serviceable";
  public static final String DRAWDOWN_RISK_DEDUPE_ERROR =
      "Drawdown rejected basis Risk dedupe framework";

  public static final String LOAN_REJECTION = "Loan Application Rejection";

  // constants required for kafka logging
  public static final String SERVICE_NAME = "los-service";
  public static final String APPLICATION_NAME = "los";
  public static final String ERROR = "error";

  // risk constants
  public static final String HIGH_RISK = "HIGH_RISK";
  public static final String MEDIUM_RISK = "MEDIUM_RISK";
  public static final String LOW_RISK = "LOW_RISK";
  public static final String RED = "red";

  // constants required for disbursal
  public static final String PASS = "PASS";
  public static final String AUTO = "AUTO";
  public static final String ASIA_KOLKATA = "Asia/Kolkata";
  public static final String INITIATING_SAVE_UPDATE =
      "[TRANSACTION_STATUS] initiating save or update for loan application id: {}";
  public static final String EXISTING_TRANSACTION_FOUND =
      "[TRANSACTION_STATUS] existing transaction found for loan application id: {}, updating"
          + " fields";
  public static final String SUCCESSFULLY_UPDATED_TRANSACTION =
      "[TRANSACTION_STATUS] successfully updated transaction for loan application id: {}";
  public static final String ERROR_UPDATING_TRANSACTION =
      "[TRANSACTION_STATUS] error occurred while updating transaction for loan application id: {}";
  public static final String NO_EXISTING_TRANSACTION_FOUND =
      "[TRANSACTION_STATUS] no existing transaction found for loan application id: {}, creating new"
          + " transaction";
  public static final String SUCCESSFULLY_CREATED_TRANSACTION =
      "[TRANSACTION_STATUS] successfully created new transaction for loan application id: {}";
  public static final String ERROR_CREATING_TRANSACTION =
      "[TRANSACTION_STATUS] error occurred while creating transaction for loan application id: {}";
  public static final String BANK_VERIFICATION_FAILED =
      "[BANK_VERIFICATION] bank verification failed for loan application id: {}";
  public static final String MESSAGE = "message";
  public static final String PRE_DISBURSAL_CHECK_FAILURE = "pre-disbursal validations failure";

  // constants required for risk categorization
  public static final String RISK_CATEGORIZATION_RETRY = "[RISK CATEGORIZATION RETRY]";
  public static final String RISK_LOAN_APPLICATION_NOT_FOUND = "Loan Application Not Found";
  public static final String RISK_CATEGORIZATION_RETRY_FAILED = "Risk Categorization Failed";
  public static final String RISK_RETRY_PROCESSED_SUCCESSFULLY = "Request Processed Successfully";
  public static final String NO_FAILED_CASE_FOR_RISK_CATEGORIZATION_FOUND =
      "No failed case for Risk Categorization found";

  public static final String M2P_PRODUCT_ID_NOT_FOUND = "loan product id not found";

  public static final String FAILED_UPDATING_LEAD_VIA_XML =
      "[UPDATE_LEAD_VIA_XML] failed to update lead from XML for leadId {}: {}";
  public static final String UPDATE_LEAD_VIA_XML =
      "[UPDATE_LEAD_VIA_XML] successfully updated aadhaar address for lead {}";
  public static final String CKYC_UPDATE = "[CKYC_UPDATE]";

  // aml/pep check
  public static final String YES = "Yes";
  public static final String PEP_REJECTION_DESCRIPTION =
      "Application not acceptable as per PEP Policy";
  public static final String REJECT = "Reject";
  public static final String CANNOT_BE_DONE = "CAN_NOT_BE_DONE";
  public static final String FEATURE_NOT_ENABLED = "Feature not enabled";
  public static final String PEP_CHECK_CANNOT_BE_DONE = "PEP check can not be done";
  public static final String AML_CHECK_CANNOT_BE_DONE = "AML check can not be done";
  public static final String AML_REJECTION_DESCRIPTION =
      "Application not acceptable as per AML Policy";
  public static final String MANUAL_REVIEW_DESCRIPTION =
      "Manual Review required due to AML score threshold";
  public static final String AML_VERIFIED_DESCRIPTION =
      "AML score below rejection and manual review threshold";
  public static final String MANUAL_REVIEW = "Manual Review";
  public static final String DEFAULT_AML_PEP_DISABLE_DESCRIPTION =
      "PEP check passed or disabled, and AML check either disabled or no valid score available";
  public static final Double AML_CHECK_DEFAULT_ZERO = 0.0;
  public static final String AML_PEP_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  public static final String ENGLISH_PREFIX = "en";

  public static final String ERROR_CODE = "errorCode";
  public static final String USER_MESSAGE_CODE = "userMessageGlobalisationCode";

  public static final String M2P_ERROR_CODE = "G001";
  public static final String DUPLICATE_EXTERNAL_ID_MESSAGE_CODE =
      "error.msg.loan.application.reference.external.id.one.duplicated";
  public static final String ERROR_NO_LOAN_FOUND = "No loan found for external ID: ";

  // PAN AADHAAR LINKAGE - DISBURSAL CHECK
  public static final String PAN_AADHAAR_LINKAGE_HEADER = "PAN_AADHAAR_CHECK";
  public static final String PAN_AADHAAR_LINKAGE_AUTO_DISB_STATUS =
      "[AUTO_DISB_VALIDATION] [PAN_AADHAAR_CHECK]";
  public static final String PAN_AADHAAR_NOT_LINKED = "Aadhaar is not linked with PAN";

  public static final String STATUS = "status";

  // constant required for disbursal
  public static final String RULE_NOT_FOUND = "rule not found";
  public static final String RULE_DELETE_SUCCESS = "rule deleted successfully";
  public static final String AUTO_DISB_CALLBACK_IDENTIFIER = "AUTO_DISB_CB";
  public static final String DISBURSEMENT_CONFIG = "DISBURSEMENT_CONFIG";
  public static final String MANUAL = "MANUAL";
  public static final String MANUAL_DISB = "MANUAL_DISB";
  public static final String DISBURSAL_IN_PROGRESS =
      "loan application disbursal already in-progress";
  public static final String AUTO_DISBURSAL_ERROR = "error in auto-disbursal process";
  public static final String IS_INSURANCE_CHARGE_ADDED = "isInsuranceChargeAdded";
  public static final String OFFER_DOWNGRADE = "offerDowngrade";

  // kyc reusability
  public static final String NO_AADHAAR_XML_FOUND = "No AADHAAR DATA FOUND";

  // mandate registration constants
  public static final String MANDATE_REGISTRATION_LOG_HEADER = "MANDATE_REGISTRATION";
  public static final String MANDATE_REGISTRATION_IDENTIFIER = "MANDATE_REGISTRATION_IDENTIFIER";
  public static final String CLIENT_DETAILS_ERROR = "Error in fetching client details";
  public static final String PARSING_ERROR = "Error in parsing";
  public static final String MANDATE_REGISTRATION_DETAILS_LOG_HEADER =
      "MANDATE_REGISTRATION_DETAILS";
  public static final String UNSUPPORTED_VENDOR = "[{}] Unsupported vendor: {} for loan: {}";
  public static final String FAILED_TO_SAVE_MANDATE_DETAILS =
      "Failed to save mandate registration details";
  public static final String MANDATE_DATA_MISMATCH =
      "Mandate registration details fetched from Digio does not match with Trillion's Information";
  public static final String MANDATE_CALLBACK_IDENTIFIER = "MANDATE_CALLBACK";
  public static final String MANDATE_DIGIO_CALLBACK = "MANDATE_DIGIO_CALLBACK";
  public static final String MANDATE_PARTNER_CALLBACK = "MANDATE_PARTNER_CALLBACK";
  public static final String CREDIT_LINE_STATUS_CALLBACK_IDENTIFIER = "CREDIT_LINE_STATUS_CB";
  public static final String DRAWDOWN_CALLBACK_IDENTIFIER = "DRAWDOWN_CB";
  public static final String UNAPPROVED_LOAN = "Loan is not in approved state";
  public static final String LOAN_CLIENT_MISMATCH =
      "Requested loan details does not matches with client details";
  public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
  public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  public static final String SAVINGS = "SAVINGS";
  public static final String ERROR_FETCHING_LIVE_BANK_LOG =
      "[{}] Error fetching live banks for productCode: {}. Error: {}";
  public static final String ERROR_FETCHING_LIVE_BANK = "Error fetching live banks";
  public static final String PARTNER_NOT_FOUND = "Partner not found for product code: ";
  public static final String EMPTY_MANDATE_REGISTRATION_CONFIG =
      "Mandate Registration Config is Null";

  // client fields
  public static final String FIRST_NAME = "firstName";
  public static final String MIDDLE_NAME = "middleName";
  public static final String LAST_NAME = "lastName";
  public static final String DOB = "dateOfBirth";
  public static final String MOBILE_NUMBER = "mobileNumber";
  public static final String ALTERNATE_MOBILE_NUMBER = "alternateMobileNo";
  public static final String EMAIL = "email";
  public static final String ADDRESS_TYPE = "addressType";
  public static final String ADDRESS_LINE_ONE = "addressLineOne";
  public static final String ADDRESS_LINE_TWO = "addressLineTwo";
  public static final String POSTAL_CODE = "postalCode";
  public static final String LANDMARK = "landmark";
  public static final String OWNERSHIP_TYPE = "ownershipType";
  public static final String GENDER = "gender";
  public static final String BANK_ACCOUNT_TYPE = "bankAccountType";
  public static final String NAME = "name";
  public static final String ACCOUNT_NUMBER = "accountNumber";
  public static final String IFSC_CODE = "ifscCode";
  public static final String ACCOUNT_TYPE = "accountType";
  public static final String PRE_DISBURSAL_VALIDATION = "PRE_DISBURSAL_VALIDATION";
  public static final String BRE_QC = "bre-qc";
  public static final String PRE_DISB_QC = "pre-disb-qc";
  public static final String PRE_ACTV_QC = "pre-actv-qc";

  public static final String UPDATE_GST = "[UPDATE_GST_ON_DEDUPE]";

  public static final String CREDIT_LINE_APPROVAL_CTA_IDENTIFIER = "credit-line-approval";
  public static final String CREDIT_LINE_CREATION_CTA_IDENTIFIER = "credit-line-creation";
  public static final String DISBURSEMENT_TRIGGER_CTA_IDENTIFIER = "disbursement-trigger";
  public static final String LOAN_CREATION_CTA_IDENTIFIER = "loan-creation";
  public static final String VCIP_LINK_NOTIFICATION = "VCIP_LINK_NOTIFICATION";
  public static final String KYC_STATUS_VERIFIED = "VERIFIED";

  public static final String EMPTY_FILE_CONTENT = "file content cannot be null or empty";
  public static final String BASE64 = "base64";
  public static final String SHORT_FILE_CONTENT = "file content is too short to be a valid pdf";
  public static final String INVALID_PDF = "file content is not a valid pdf";

  public static final String NIRA_PRODUCT = "NPLO1";
  public static final String SAVEIN_PRODUCT = "SAVE1";

  // portfolio metrics
  public static final String PORTFOLIO_BALANCING = "PORTFOLIO_BALANCING";
  public static final String SAVE_IN_NTC_COUNT = "SAVE_IN_NTC_COUNT";
  public static final String SAVE_IN_NON_NTC_COUNT = "SAVE_IN_NON_NTC_COUNT";
  public static final String NIRA_COHORT_A_AMOUNT = "NIRA_COHORT_A_AMOUNT";
  public static final String NIRA_COHORT_B_AMOUNT = "NIRA_COHORT_B_AMOUNT";
  public static final String NIRA_COHORT_C_AMOUNT = "NIRA_COHORT_C_AMOUNT";
  public static final String NIRA_COHORT_D_AMOUNT = "NIRA_COHORT_D_AMOUNT";
  public static final String NIRA_COHORT_E_AMOUNT = "NIRA_COHORT_E_AMOUNT";
  public static final String NIRA_COHORT_F_AMOUNT = "NIRA_COHORT_F_AMOUNT";
  public static final String NIRA_COHORT_G_AMOUNT = "NIRA_COHORT_G_AMOUNT";
  public static final String NIRA_COHORT_H_AMOUNT = "NIRA_COHORT_H_AMOUNT";
  public static final String NIRA_UNKNOWN_METRIC = "UNKNOWN";
  public static final String SAVE_IN_UNKNOWN_METRIC = "SAVE_IN_UNKNOWN_METRIC";

  public static final String LOW = "LOW";
  public static final String MEDIUM = "MEDIUM";
  public static final String HIGH = "HIGH";

  public static final String SMALL = "SMALL";
  public static final String LARGE = "LARGE";

  // KYC VALIDATION CONSTANTS
  public static final String KYC_VALIDATION = "kyc-validation";
  public static final String NAME_MATCH_EXECUTION = "nameMatchExecution";
  public static final String FACE_MATCH_EXECUTION = "faceMatchExecution";
  public static final String NAME_MATCH_PRIORITY = "nameMatchPriority";
  public static final String FACE_MATCH_PRIORITY = "faceMatchPriority";
  public static final String NAME_MATCH_THRESHOLD = "nameMatchThreshold";

  /**
   * BUSINESS_LOAN_CONFIG flow: minimum address match score (e.g. percent) for business loan doc
   * eval
   */
  public static final String ADDRESS_MATCH_THRESHOLD = "addressMatchThreshold";

  public static final String FACE_MATCH_THRESHOLD = "faceMatchThreshold";
  public static final String XML_VALIDITY = "xmlValidity";
  public static final String NAME_FALLBACK = "nameFallback";
  public static final String FACE_FALLBACK = "faceFallback";
  public static final String DRAWDOWN_BRE = "DRAWDOWN_BRE";

  public static final String DRAWDOWN_ORCHESTRATOR_LOGGER = "DRAWDOWN_ORCHESTRATOR";
  public static final String PERSIST_DRAWDOWN_LOGGER = "PERSIST_DRAWDOWN";
  public static final String AGREEMENT_UPLOAD_LOGGER = "AGREEMENT_UPLOAD";
  public static final String FETCH_LEAD_ID_LOGGER = "FETCH_LEAD_ID";

  // AML PEP CONSTANTS
  public static final String PEP = "PEP";
  public static final String PAN = "PAN";
  public static final Double DIGIO_PEP_NAME_MATCH_THRESHOLD = 100.0;
  public static final Double DIGIO_AML_NAME_MATCH_DEFAULT_VALUE = 0.0;
  public static final Double DIGIO_PEP_NAME_MATCH_DEFAULT_VALUE = 0.0;
  public static final String FAILED = "FAILED";
  public static final String AML_PEP_REDIS_KEY = "AML_PEP:";
  public static final String PAN_VERIFY = "PAN_VERIFY";
  public static final String AML_PEP_VERIFY = "AML_PEP_VERIFY";
  public static final String NO = "No";
  public static final String DIGIO_AML_PEP_CHECK =
      "[AML_PEP_VERIFY][SCREENING] Digio AML PEP Check";

  public static final String LOAN_LEVEL_CLIENT_DETAILS_REDIS_KEY_PREFIX =
      "loan_level_client_details";
  public static final String LOAN_APPLICATION_NO = "loanApplicationNo";
}
