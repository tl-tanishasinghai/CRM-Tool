package com.trillionloans.customer_portal.constant;

public class StringConstants {
  private StringConstants() {}

  // error strings, sent as response to clients & vendors
  public static final String SERVER_ERROR = "server error";
  public static final String CLIENT_ERROR = "client error";
  public static final String DEFAULT_ERROR_MESSAGE = "Something went wrong";
  public static final String HTTP_STATUS_410 = "410 GONE";
  public static final String INVALID_RESPONSE_ERROR = "invalid_response_error";

  // logging literals used for unified logs in the application
  public static final String LOGGING_LITERAL = "[{}] request to {}: {}";
  public static final String LOGGING_LITERAL_URI = "[{}] request to {} (uri): {}";
  public static final String LOGGING_LITERAL_RETRY = "[{}] retrying request {}, retry count: {}";
  public static final String LOGGING_ERROR_RESPONSE =
      "[{}] [ERROR] status code from {}: {} response: {}";
  public static final String LOGGING_RESPONSE = "[{}] response from {}: {}";
  public static final String REQUEST_LOG = "REQUEST_LOG";
  public static final String TRACE_ID = "traceId";
  public static final String FORBIDDEN_ERROR = "forbidden error";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String APPLICATION_JSON = "application/json";

  // lead details
  public static final String LEAD_ID = "leadId";
  public static final String MOBILE_NUMBER = "mobileNumber";
  public static final String DOB = "dateOfBirth";
  public static final String PAN = "panLast4Digits";

  public static final String MMM_D_YYYY = "MMM d, yyyy";
  public static final String DD_MM_YYYY = "dd/MM/yyyy";
  public static final String YYYY_MM_DD = "yyyy-MM-dd";
  public static final String DD_MM_YYYY_DASH = "dd-MM-yyyy";

  public static final String INVALID_INPUT = "Invalid input";

  public static final String LOGGING_LITERAL_URI_AND_BODY =
      "[{}] request to {} (uri): {} , (body): {}";

  public static final String LOGGING_SUCCESS_RESPONSE = "[{}] status code from {}: {} response: {}";

  public static final String LOGIN = "LOGIN";
  public static final String FLOW_STATE = "flowState";
  public static final String VERIFY_PAN = "VERIFY_PAN";
  public static final String AWAITING_OTP = "AWAITING_OTP";
  public static final String AWAITING_PAN = "AWAITING_PAN";
  public static final String VERIFY_OTP = "VERIFY_OTP";
  public static final String SEND_OTP = "SEND_OTP";
  public static final String RESEND_OTP = "RESEND_OTP";
  public static final String LOGOUT_MOBILE_NUMBER = "LOGOUT_MOBILE_NUMBER";
  public static final String LOGGING_LITERAL_MULTIPART_URI =
      "[{}] calling multipart post to {} (uri): {}";

  public static final String UNREGISTERED_MOBILE_NUMBER =
      "Mobile Number not registered. Please retry with a registered number";
  public static final String UNREGISTERED_MOBILE_NUMBER_INCORRECT_DOB =
      "No account with this mobile number and date of birth. Please check your details and try"
          + " again.";
  public static final String PAN_MISMATCH_NOT_FOUND =
      "PAN mismatch or no match found. Please check your details and try again. Redirecting to"
          + " login page.";
  public static final String UNKNOWN_LOAN_STATUS = "UNKNOWN_STATUS";

  public static final String JSON_PROCESSING_EXCEPTION_MESSAGE =
      "An exception with message [%s] was thrown while processing request.";

  public static final String UNAUTHORIZED = "Unauthorized to access this resource";

  public static final String INVALID_SESSION =
      "Session expired or invalid. Please start the login process again.";

  public static final String INVALID_FLOW_STATE_OTP = "Invalid flow state for OTP verification";
  public static final String INVALID_FLOW_STATE_PAN = "Invalid flow state for PAN verification";

  public static final String NONE = "None";

  public static final String REDIS_REMOVE_MSG = "key removed from redis cache";
  public static final String REDIS_ALL_REMOVE_MSG = "all keys removed from redis cache";

  public static final String COLLECTION_SERVICE = "collection-service";

  public class MaskConstants {
    public static final String GENERIC_MASK = "XXXXX";
    public static final String EMAIL_MASK = "masked@example.com";
    public static final String NAME_MASK = "XXX";
    public static final String DOB_MASK = "XXXX-XX-XX";
    public static final String OTP_MASK = "****";
    public static final String TOKEN_MASK = "*****";
    public static final String UCIC_MASK_PREFIX = "XXXXXXXXXX";
  }

  public static class Freshdesk {
    public static final String STATUS_OPEN = "2"; // 2 = Open status in Freshdesk
  }
}
