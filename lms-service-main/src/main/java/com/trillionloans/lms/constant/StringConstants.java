package com.trillionloans.lms.constant;

public class StringConstants {
  private StringConstants() {}

  // error strings, sent as response to clients & vendors
  public static final String SOMETHING_WENT_WRONG_CACHE =
      "something went wrong, please try again - cache";
  public static final String SOMETHING_WENT_WRONG_CONFIG =
      "something went wrong, please try again - config";
  public static final String CAFFEINE_REMOVE_MSG = "key removed from caffeine cache";
  public static final String CAFFEINE_ALL_REMOVE_MSG = "all keys removed from caffeine cache";
  public static final String NOT_FOUND = "not found";
  public static final String CAFFEINE_OPS = "CAFFEINE_OPS";
  public static final String REDIS_OPS = "REDIS_OPS";
  public static final String STRAPI_OPS = "STRAPI_OPS";
  public static final String SOMETHING_WENT_WRONG = "something went wrong, please try again";
  public static final String LOAN_NOTIFICATIONS = "LOAN_NOTIFICATIONS";
  public static final String WELCOME_NOTIFICATIONS = "WELCOME_NOTIFICATIONS";
  public static final String LOAN_AGREEMENT_NOTIFICATIONS = "LOAN_AGREEMENT_NOTIFICATIONS";
  public static final String CKYC_NOTIFICATION = "CKYC_NOTIFICATION";
  public static final String HISTORICAL_CKYC_NOTIFICATION = "HISTORICAL_CKYC_NOTIFICATION";

  // [DO NOT ALTER]
  // string literals for usages at various places in code
  public static final String PRODUCT_CODE = "productCode";
  public static final String PARTNER_ID = "partnerId";
  public static final String ACTIVE = "A";
  public static final String REDIS_REMOVE_MSG = "key removed from redis cache";
  public static final String REDIS_ALL_REMOVE_MSG = "all keys removed from redis cache";
  public static final String ERROR_PROCESSING_JSON = "Error processing JSON";
  public static final String LOGGING_ERROR_RESPONSE =
      "[{}] [ERROR] status code from {}: {} response: {}";
  public static final String LOGGING_RESPONSE = "[{}] response from {}: {}";
  public static final String REQUEST_LOG = "REQUEST_LOG";
  public static final String TRACE_ID = "traceId";
  public static final String LOGGER_HEADER = "loggerHeader";
  public static final String CLIENT_CONSENT = "CLIENT_CONSENT";

  // Constants required for kafka logging
  public static final String SERVICE_NAME = "lms-service";
  public static final String APPLICATION_NAME = "lms";
  public static final String ERROR = "error";
  public static final String RESPONSE = "response";
  public static final String CHARGES_LOGGER = "CHARGES_RUN";

  /** Adapter logger header for getRestructureDetails flow (logging, dashboarding, metrics). */
  public static final String RESTRUCTURE_GET_DETAILS =
      "[RESTRUCTURE_GET_DETAILS],type: {},lan: {},step: {},result: {},{}";

  public static final String RESTRUCTURE_RISK_VALIDATION = "[RESTRUCTURE_RISK_VALIDATION],{}";

  /** Logger headers for eligibility flow: use with log.info (success) or log.warn (fail). */
  public static final String CHECK_ELIGIBILITY_LOG =
      "[CHECK_ELIGIBILITY],lan: {},step: {},result: {},{}";

  public static final String CHECK_ELIGIBILITY_WITH_DB_VALIDATION_LOG =
      "[CHECK_ELIGIBILITY_WITH_DB_VALIDATION],lan: {},step: {},result: {},{}";
  public static final String GET_ELIGIBILITY_LOG =
      "[GET_ELIGIBILITY],lan: {},step: {},result: {},{}";

  // M2P date format constants
  public static final String M2P_DATE_FORMAT = "dd-MM-yyyy";
  public static final String M2P_LOCALE = "en";

  public static final String CREDIT_LINE_REPAYMENT_LOG_HEADER = "CREDIT_LINE_MARK_REPAYMENT";
  public static final String CREDIT_LINE_TRANSACTION_DETAILS = "CREDIT_LINE_TRANSACTION_DETAILS";
  public static final String LOAN_ACCOUNT_NO = "loanAccountNo";
  public static final String LOAN_ID = "loanId";
  public static final String LOAN_APPLICATION_NO = "loanApplicationNo";
  public static final String CLIENT_ID = "clientId";
}
