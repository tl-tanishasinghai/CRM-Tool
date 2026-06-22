package com.trillionloans.customer_portal.controller;

import com.trillionloans.customer_portal.model.dto.LoginRequest;
import com.trillionloans.customer_portal.model.dto.OTPRequestDTO;
import com.trillionloans.customer_portal.model.dto.OTPVerificationDTO;
import com.trillionloans.customer_portal.model.dto.VerifyOtpClientResponseDTO;
import com.trillionloans.customer_portal.model.dto.VerifyPANRequest;
import com.trillionloans.customer_portal.service.AuthOTPService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthOTPService authOTPService;

  @Autowired
  public AuthController(AuthOTPService authOTPService) {
    this.authOTPService = authOTPService;
  }

  @Operation(
      summary = "Send OTP",
      description = "This operation sends OTP to the mobileNumber of the user.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = OTPRequestDTO.class)),
      description = "OTP Request to be provided in the request body.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
        @ApiResponse(responseCode = "400", description = "Bad Request")
      })
  @PostMapping("/send")
  public Mono<ResponseEntity<?>> sendOTP(
      @RequestBody OTPRequestDTO otpRequestDTO, WebSession session) {
    return Mono.just(
        ResponseEntity.ok(authOTPService.sendOtp(otpRequestDTO.getMobileNumber(), session)));
  }

  @Operation(
      summary = "Resend OTP",
      description = "This operation resends OTP to the mobileNumber of the user.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = OTPRequestDTO.class)),
      description = "OTP Request to be provided in the request body.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "OTP resent successfully"),
        @ApiResponse(responseCode = "400", description = "Bad Request")
      })
  @PostMapping("/resend")
  public ResponseEntity<?> resendOTP(@RequestBody OTPRequestDTO otpRequestDTO, WebSession session) {
    return ResponseEntity.ok(authOTPService.reSendOtp(otpRequestDTO.getMobileNumber(), session));
  }

  @Operation(
      summary = "Verifies OTP",
      description = "This operation verifies OTP against a mobileNumber for the user.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = OTPVerificationDTO.class)),
      description = "OTPVerification Request to be provided in the request body.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Verification successful"),
        @ApiResponse(responseCode = "401", description = "Bad Request")
      })
  @PostMapping("/verify")
  public Mono<ResponseEntity<?>> verifyOTP(
      @RequestBody OTPVerificationDTO otpVerificationDTO, WebSession session) {
    return authOTPService
        .verifyOTP(otpVerificationDTO.getMobileNumber(), otpVerificationDTO.getOtp(), session)
        .map(
            verifyOTPResponse -> {
              ResponseCookie jwtCookie =
                  authOTPService.createJWTCookie(
                      verifyOTPResponse.getToken(), verifyOTPResponse.getExpiry());
              VerifyOtpClientResponseDTO verifyOtpClientResponseDTO =
                  new VerifyOtpClientResponseDTO();
              verifyOtpClientResponseDTO.setLeadId(verifyOTPResponse.getLeadId());
              return ResponseEntity.ok()
                  .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                  .body(verifyOtpClientResponseDTO);
            });
  }

  @Operation(
      summary = "Initiate login with mobile number and Date of Birth",
      description =
          "Starts a multi-step login flow by validating a user's mobile number and Date of Birth.\n"
              + "      The system checks for a client IDs and determines the next step:\n"
              + "      - If a unique client is found, an OTP is sent.\n"
              + "      - If multiple client are found, a PAN verification step is required.\n"
              + "      - If no client is found, the flow terminates.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = LoginRequest.class)),
      description = "Login Request to be provided in the request body.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Login flow successfully initiated. The response indicates the next required step."
                    + " A session cookie is set for tracking the state of the multi-step process.",
            content =
                @Content(
                    mediaType = "application/json",
                    examples = {
                      @ExampleObject(
                          name = "Awaiting OTP",
                          value = "{\"flowStatus\": \"awaiting_otp\"}"),
                      @ExampleObject(
                          name = "Awaiting PAN",
                          value = "{\"flowStatus\": \"awaiting_pan\"}")
                    }),
            headers = {
              @Header(
                  name = "Set-Cookie",
                  description = "Session cookie to maintain the state of the login flow.",
                  schema =
                      @Schema(type = "string", example = "JSESSIONID=abcde12345; Path=/; HttpOnly"))
            }),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request. Invalid input format for mobile number or date of birth.",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value = "{\"message\": \"Invalid mobile number format.\"}"))),
        @ApiResponse(
            responseCode = "404",
            description =
                "Not Found. The provided mobile number and date of birth combination does not match"
                    + " any customer.",
            content =
                @Content(
                    mediaType = "application/json",
                    examples =
                        @ExampleObject(
                            value =
                                "{\"message\": \"We couldn’t find an account with this mobile"
                                    + " number and date of birth.\"}")))
      })
  @PostMapping("/login")
  public Mono<ResponseEntity<?>> login(@RequestBody LoginRequest loginRequest, WebSession session) {
    return Mono.just(
        ResponseEntity.ok(
            authOTPService.login(
                loginRequest.getMobileNumber(), loginRequest.getDateOfBirth(), session)));
  }

  @Operation(
      summary = "Verify PAN and continue login flow",
      description =
          "This operation verifies the last 4 digits of the PAN to resolve multiple customer"
              + " matches and proceeds to the OTP step.")
  @io.swagger.v3.oas.annotations.parameters.RequestBody(
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = VerifyPANRequest.class)),
      description = "Request body containing the last 4 digits of the PAN.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description =
                "Unique customer found. Session state updated to awaiting_otp, and an OTP has been"
                    + " sent."),
        @ApiResponse(
            responseCode = "404",
            description = "Not found. The provided PAN does not lead to a unique customer match."),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized. Session expired or invalid.")
      })
  @PostMapping("/login/verify-pan")
  public Mono<ResponseEntity<?>> verifyPAN(
      @RequestBody VerifyPANRequest verifyPANRequest, WebSession session) {
    return Mono.just(
        ResponseEntity.ok(authOTPService.verifyPAN(verifyPANRequest.getPanLast4Digits(), session)));
  }
}
