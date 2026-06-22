package com.trillionloans.customer_portal.service;

import static com.trillionloans.customer_portal.constant.StringConstants.AWAITING_OTP;
import static com.trillionloans.customer_portal.constant.StringConstants.AWAITING_PAN;
import static com.trillionloans.customer_portal.constant.StringConstants.DOB;
import static com.trillionloans.customer_portal.constant.StringConstants.FLOW_STATE;
import static com.trillionloans.customer_portal.constant.StringConstants.INVALID_FLOW_STATE_OTP;
import static com.trillionloans.customer_portal.constant.StringConstants.INVALID_FLOW_STATE_PAN;
import static com.trillionloans.customer_portal.constant.StringConstants.INVALID_SESSION;
import static com.trillionloans.customer_portal.constant.StringConstants.LEAD_ID;
import static com.trillionloans.customer_portal.constant.StringConstants.LOGIN;
import static com.trillionloans.customer_portal.constant.StringConstants.MOBILE_NUMBER;
import static com.trillionloans.customer_portal.constant.StringConstants.PAN;
import static com.trillionloans.customer_portal.constant.StringConstants.PAN_MISMATCH_NOT_FOUND;
import static com.trillionloans.customer_portal.constant.StringConstants.RESEND_OTP;
import static com.trillionloans.customer_portal.constant.StringConstants.SEND_OTP;
import static com.trillionloans.customer_portal.constant.StringConstants.SERVER_ERROR;
import static com.trillionloans.customer_portal.constant.StringConstants.UNREGISTERED_MOBILE_NUMBER_INCORRECT_DOB;
import static com.trillionloans.customer_portal.constant.StringConstants.VERIFY_OTP;
import static com.trillionloans.customer_portal.constant.StringConstants.VERIFY_PAN;

import com.trillionloans.customer_portal.api.internal.AuthApi;
import com.trillionloans.customer_portal.constant.StringConstants;
import com.trillionloans.customer_portal.exception.CustomisedException;
import com.trillionloans.customer_portal.exception.NotFoundException;
import com.trillionloans.customer_portal.model.dto.LeadIdResponse;
import com.trillionloans.customer_portal.model.dto.LoginResponse;
import com.trillionloans.customer_portal.model.dto.LogoutRequestDTO;
import com.trillionloans.customer_portal.model.dto.OTPRequestDTO;
import com.trillionloans.customer_portal.model.dto.OTPVerificationDTO;
import com.trillionloans.customer_portal.model.dto.VerifyOTPResponse;
import com.trillionloans.customer_portal.util.OtpUtils;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class AuthOTPService {
  private final AuthApi authApi;
  private final CustomerService customerService;
  public static final String PATH = "/";
  public static final String JWT_COOKIE_NAME = "AUTH-TOKEN";

  @Value("${cookie.secure:true}")
  private boolean secure;

  @Value("${cookie.sameSite:None}")
  private String sameSite;

  public AuthOTPService(AuthApi authApi, CustomerService customerService) {
    this.customerService = customerService;
    this.authApi = authApi;
  }

  public Mono<LoginResponse> login(String mobileNumber, String dateOfBirth, WebSession session) {
    OtpUtils.validateFieldsDOB(mobileNumber, dateOfBirth, LOGIN);
    Flux<LeadIdResponse> leadIds =
        customerService.fetchLeadIdAgainstMobileNumberAndDOB(mobileNumber, dateOfBirth);
    if (leadIds == null) {
      log.error("[ERROR] null response for mobileNumber={}.", mobileNumber);
      return Mono.error(new IllegalStateException(SERVER_ERROR));
    }
    return leadIds
        .collectList()
        .flatMap(
            list -> {
              log.info("[LOGIN] fetched {} lead(s) for mobileNumber={}", list.size(), mobileNumber);
              if (list.size() == 1) {
                LeadIdResponse leadIdResponse = list.get(0);
                return handleUniqueCustomer(
                    mobileNumber, dateOfBirth, session, leadIdResponse.getEntityId());
              } else if (list.size() > 1) {
                return handleMultipleCustomers(mobileNumber, dateOfBirth, session);
              } else {
                session.invalidate();
                return Mono.error(new NotFoundException(UNREGISTERED_MOBILE_NUMBER_INCORRECT_DOB));
              }
            });
  }

  public Mono<LoginResponse> handleUniqueCustomer(
      String mobileNumber, String dateOfBirth, WebSession session, Long leadId) {
    session.getAttributes().put(FLOW_STATE, AWAITING_OTP);
    session.getAttributes().put(MOBILE_NUMBER, mobileNumber);
    session.getAttributes().put(DOB, dateOfBirth);
    session.getAttributes().put(LEAD_ID, leadId);

    session.setMaxIdleTime(Duration.ofMinutes(10));
    String mobileNumberWithCountryCode = "91" + mobileNumber;
    sendOtp(mobileNumberWithCountryCode, session)
        .subscribe(
            result -> {
              log.info("[LOGIN] otp sent to mobileNumber={}", mobileNumberWithCountryCode);
            },
            error -> {
              log.info(
                  "[LOGIN] otp failed to sent to mobileNumber={}. error={}",
                  mobileNumberWithCountryCode,
                  error.getMessage());
            });
    log.info("[LOGIN] otp sent to mobileNumber={}", mobileNumberWithCountryCode);
    return Mono.just(new LoginResponse(AWAITING_OTP));
  }

  public Mono<LoginResponse> handleMultipleCustomers(
      String mobileNumber, String dateOfBirth, WebSession session) {
    session.getAttributes().put(FLOW_STATE, AWAITING_PAN);
    session.getAttributes().put(MOBILE_NUMBER, mobileNumber);
    session.getAttributes().put(DOB, dateOfBirth);
    session.setMaxIdleTime(Duration.ofMinutes(10));
    return Mono.just(new LoginResponse(AWAITING_PAN));
  }

  public Mono<?> sendOtp(String mobileNumber, WebSession session) {
    OtpUtils.validateFieldsMobileWithCountryCode(mobileNumber, SEND_OTP);

    String flowState = session.getAttribute(FLOW_STATE);
    if (flowState == null) {
      return Mono.error(
          new CustomisedException(INVALID_SESSION, HttpStatus.UNAUTHORIZED, SEND_OTP));
    }
    if (!AWAITING_OTP.equals(flowState)) {
      return Mono.error(
          new CustomisedException(INVALID_FLOW_STATE_OTP, HttpStatus.FORBIDDEN, SEND_OTP));
    }

    OTPRequestDTO sendOTPRequest = new OTPRequestDTO();
    sendOTPRequest.setMobileNumber(mobileNumber);

    return authApi.sendOTP(sendOTPRequest);
  }

  public Mono<?> reSendOtp(String mobileNumber, WebSession session) {
    OtpUtils.validateFields(mobileNumber, RESEND_OTP);
    String mobileNumberWithCountryCode = "91" + mobileNumber;

    String flowState = session.getAttribute(FLOW_STATE);
    if (flowState == null) {
      return Mono.error(
          new CustomisedException(INVALID_SESSION, HttpStatus.UNAUTHORIZED, RESEND_OTP));
    }
    if (!AWAITING_OTP.equals(flowState)) {
      return Mono.error(
          new CustomisedException(INVALID_FLOW_STATE_OTP, HttpStatus.FORBIDDEN, RESEND_OTP));
    }

    OTPRequestDTO sendOTPRequest = new OTPRequestDTO();
    sendOTPRequest.setMobileNumber(mobileNumberWithCountryCode);
    log.info("[LOGIN] otp re-sent to mobileNumber={}", mobileNumberWithCountryCode);
    return authApi.reSendOTP(sendOTPRequest);
  }

  public Mono<VerifyOTPResponse> verifyOTP(String mobileNumber, String otp, WebSession session) {
    OtpUtils.validateFields(mobileNumber, otp, VERIFY_OTP);

    String flowState = session.getAttribute(FLOW_STATE);
    if (flowState == null) {
      return Mono.error(
          new CustomisedException(INVALID_SESSION, HttpStatus.UNAUTHORIZED, VERIFY_OTP));
    }
    if (!AWAITING_OTP.equals(flowState)) {
      return Mono.error(
          new CustomisedException(INVALID_FLOW_STATE_OTP, HttpStatus.FORBIDDEN, VERIFY_OTP));
    }

    Long leadId = (Long) session.getAttribute(LEAD_ID);
    String dateOfBirth = session.getAttribute(DOB);
    String panLast4Digits = session.getAttribute(PAN);
    String mobileNumberWithCountryCode = "91" + mobileNumber;

    OTPVerificationDTO otpVerificationDTO = new OTPVerificationDTO();
    otpVerificationDTO.setOtp(otp);
    otpVerificationDTO.setMobileNumber(mobileNumberWithCountryCode);
    otpVerificationDTO.setDateOfBirth(dateOfBirth);
    if (panLast4Digits != null) {
      otpVerificationDTO.setPanLast4Digits(panLast4Digits);
    }
    return authApi
        .verifyOTP(otpVerificationDTO)
        .flatMap(
            verifyOtpResponse -> {
              verifyOtpResponse.setLeadId(leadId);
              return session.invalidate().thenReturn(verifyOtpResponse);
            });
  }

  public Mono<?> logoutUser(LogoutRequestDTO logoutRequestDTO) {
    OtpUtils.validateFields(
        logoutRequestDTO.getMobileNumber(), StringConstants.LOGOUT_MOBILE_NUMBER);
    String mobileNumberWithCountryCode = "91" + logoutRequestDTO.getMobileNumber();
    logoutRequestDTO.setMobileNumber(mobileNumberWithCountryCode);
    return authApi.logout(logoutRequestDTO);
  }

  public ResponseCookie createJWTCookie(String token, String expiry) {
    long nowEpoch = Instant.now().getEpochSecond();
    long maxAgeSeconds = Math.max(0, Long.parseLong(expiry) - nowEpoch);

    return ResponseCookie.from(JWT_COOKIE_NAME, token)
        .httpOnly(true)
        .secure(secure)
        .sameSite(sameSite)
        .path(PATH)
        .maxAge(Duration.ofSeconds(maxAgeSeconds))
        .build();
  }

  public ResponseCookie clearJWTCookie() {
    return ResponseCookie.from(JWT_COOKIE_NAME, StringUtils.EMPTY)
        .httpOnly(true)
        .secure(secure)
        .maxAge(Duration.ZERO)
        .sameSite(sameSite)
        .path(PATH)
        .build();
  }

  public Mono<Object> verifyPAN(String panLast4Digits, WebSession session) {
    OtpUtils.validateFieldsPAN(panLast4Digits, VERIFY_PAN);

    String flowState = session.getAttribute(FLOW_STATE);
    if (flowState == null) {
      return Mono.error(
          new CustomisedException(INVALID_SESSION, HttpStatus.UNAUTHORIZED, VERIFY_PAN));
    }
    if (!AWAITING_PAN.equals(flowState)) {
      return Mono.error(
          new CustomisedException(
              INVALID_FLOW_STATE_PAN, HttpStatus.FORBIDDEN, StringConstants.VERIFY_PAN));
    }

    String mobileNumber = session.getAttribute(MOBILE_NUMBER);
    String mobileNumberWithCountryCode = "91" + mobileNumber;
    String dateOfBirth = session.getAttribute(DOB);

    Flux<LeadIdResponse> leadIds =
        customerService.fetchLeadIdAgainstMobileNumberDOBAndPAN(
            mobileNumber, dateOfBirth, panLast4Digits);
    if (leadIds == null) {
      log.error("[ERROR] null response for mobileNumber={}.", mobileNumber);
      return Mono.error(new IllegalStateException(SERVER_ERROR));
    }
    return leadIds
        .collectList()
        .flatMap(
            list -> {
              log.info(
                  "[VERIFY_PAN] fetched {} lead(s) for mobileNumber={}", list.size(), mobileNumber);
              if (list.size() == 1) {
                session.getAttributes().put(FLOW_STATE, AWAITING_OTP);
                LeadIdResponse leadIdResponse = list.get(0);
                session.getAttributes().put(LEAD_ID, leadIdResponse.getEntityId());
                session.getAttributes().put(PAN, panLast4Digits);
                sendOtp(mobileNumberWithCountryCode, session)
                    .subscribe(
                        result -> {
                          log.info(
                              "[VERIFY_PAN] otp sent to mobileNumber={}",
                              mobileNumberWithCountryCode);
                        },
                        error -> {
                          log.info(
                              "[VERIFY_PAN] otp failed to sent to mobileNumber={}. error={}",
                              mobileNumberWithCountryCode,
                              error.getMessage());
                        });
                return Mono.just(new LoginResponse(AWAITING_OTP));
              } else {
                session.invalidate();
                return Mono.error(new NotFoundException(PAN_MISMATCH_NOT_FOUND));
              }
            });
  }
}
