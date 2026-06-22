package com.trillionloans.customer_portal.api.internal;

import com.trillionloans.customer_portal.api.WebClientFactory;
import com.trillionloans.customer_portal.api.WebClientFactoryImpl;
import com.trillionloans.customer_portal.constant.StringConstants;
import com.trillionloans.customer_portal.model.dto.LogoutRequestDTO;
import com.trillionloans.customer_portal.model.dto.OTPRequestDTO;
import com.trillionloans.customer_portal.model.dto.OTPVerificationDTO;
import com.trillionloans.customer_portal.model.dto.VerifyOTPResponse;
import com.trillionloans.customer_portal.model.internal.WebClientParameters;
import com.trillionloans.customer_portal.model.response.ResponseDTO;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
public class AuthApi {
  private final Environment environment;
  private final WebClientFactory webClientFactory;

  private static final String AUTH_PARTNER_NAME = "authorization";

  private static final String SEND_OTP_ENDPOINT = "authorization-service.api.send-otp.endpoint";

  private static final String RE_SEND_OTP_ENDPOINT =
      "authorization-service.api.resend-otp.endpoint";

  private static final String VERIFY_OTP_ENDPOINT = "authorization-service.api.verify-otp.endpoint";

  private static final String LOGOUT_ENDPOINT = "authorization-service.api.logout-user.endpoint";

  public AuthApi(
      @Value("${authorization-service.api.base-url}") String baseUrl, Environment environment) {
    this.webClientFactory =
        new WebClientFactoryImpl(baseUrl, AUTH_PARTNER_NAME, environment, ResponseDTO.class);
    this.environment = environment;
  }

  public Mono<?> sendOTP(OTPRequestDTO otpRequestDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty(SEND_OTP_ENDPOINT)))
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters(StringConstants.SEND_OTP, AUTH_PARTNER_NAME, 0, true, true, false);
    return webClientFactory.post(
        uri, otpRequestDTO, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<?> reSendOTP(OTPRequestDTO otpRequestDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty(RE_SEND_OTP_ENDPOINT)))
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters(
            StringConstants.RESEND_OTP, AUTH_PARTNER_NAME, 0, true, true, false);
    return webClientFactory.post(
        uri, otpRequestDTO, getHeaders(), Object.class, webClientParameters);
  }

  public Mono<VerifyOTPResponse> verifyOTP(OTPVerificationDTO otpVerificationDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty(VERIFY_OTP_ENDPOINT)))
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters(
            StringConstants.VERIFY_OTP, AUTH_PARTNER_NAME, 0, true, true, false);
    return webClientFactory.post(
        uri, otpVerificationDTO, getHeaders(), VerifyOTPResponse.class, webClientParameters);
  }

  public Mono<?> logout(LogoutRequestDTO logoutRequestDTO) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty(LOGOUT_ENDPOINT)))
            .toUriString();
    WebClientParameters webClientParameters =
        new WebClientParameters(
            StringConstants.LOGOUT_MOBILE_NUMBER, AUTH_PARTNER_NAME, 0, true, true, false);
    return webClientFactory.post(
        uri, logoutRequestDTO, getHeaders(), Object.class, webClientParameters);
  }

  private HttpHeaders getHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }
}
