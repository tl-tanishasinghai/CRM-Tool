package com.trillionloans.customer_portal.configuration.filter;

import static com.trillionloans.customer_portal.constant.ResponseStatus.ERROR;
import static com.trillionloans.customer_portal.constant.StringConstants.DEFAULT_ERROR_MESSAGE;
import static com.trillionloans.customer_portal.constant.StringConstants.TRACE_ID;
import static com.trillionloans.customer_portal.constant.StringConstants.UNAUTHORIZED;
import static com.trillionloans.customer_portal.service.AuthOTPService.JWT_COOKIE_NAME;
import static com.trillionloans.customer_portal.util.EncryptionUtil.getKeyFromString;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.customer_portal.configuration.ProtectedPathRegistry;
import com.trillionloans.customer_portal.constant.ValidationKey;
import com.trillionloans.customer_portal.model.response.ResponseDTO;
import com.trillionloans.customer_portal.service.PreValidationService;
import com.trillionloans.customer_portal.util.EncryptionUtil;
import com.trillionloans.customer_portal.util.JwtTokenUtil;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.annotation.NonNull;

/**
 * WebFilter that intercepts incoming requests and performs authorization on dynamic path variables
 * (e.g., leadId, loanAccountNumber, etc.) based on the mobile number extracted from the user's JWT
 * token.
 *
 * <p>This filter is specifically designed to enforce access control rules ensuring that the
 * authenticated user can only access resources (like documents, loan details, etc.) that are linked
 * to their identity.
 *
 * <p>Workflow:
 *
 * <ul>
 *   <li>Extracts JWT token from HTTP cookies
 *   <li>Decrypts and parses the token to retrieve the mobile number
 *   <li>Matches the request URI against known protected patterns
 *   <li>Extracts relevant path variables and validates them using PreValidationService
 *   <li>If validation fails, responds with a standardized error response (403 or 400)
 * </ul>
 *
 * <p>Pattern-based validation is driven by {@link
 * com.trillionloans.customer_portal.constant.ValidationKey} and dynamic caching via Redis through
 * {@link com.trillionloans.customer_portal.service.PreValidationService}.
 *
 * @see org.springframework.web.server.WebFilter
 * @see com.trillionloans.customer_portal.service.PreValidationService
 * @see com.trillionloans.customer_portal.util.JwtTokenUtil
 */
@Slf4j
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class DynamicPathVariableFilter implements WebFilter {
  private final ObjectMapper objectMapper;
  private final JwtTokenUtil jwtTokenUtil;
  private final String JWT_BASE64_SECRET_KEY;
  private final ProtectedPathRegistry pathRegistry;
  private final PreValidationService preValidationService;

  public DynamicPathVariableFilter(
      @Value("${jwt-base64-secret-key}") String base64SecretKey,
      JwtTokenUtil jwtTokenUtil,
      PreValidationService preValidationService,
      ProtectedPathRegistry pathRegistry) {
    this.JWT_BASE64_SECRET_KEY = base64SecretKey;
    this.jwtTokenUtil = jwtTokenUtil;
    this.pathRegistry = pathRegistry;
    this.preValidationService = preValidationService;
    this.objectMapper = new ObjectMapper();
  }

  /**
   * WebFilter entry point. Intercepts every incoming request and applies path variable-based
   * validation logic for protected endpoints.
   */
  @Override
  @NonNull
  public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getURI().getPath();

    String token = extractTokenFromCookie(request);

    if (token == null) {
      return chain.filter(exchange); // Token not found, continue the filter chain
    }

    Optional<String> mobileOpt = extractMobileNumberFromToken(token);
    Optional<String> dateOfBirthOpt = extractDOBFromToken(token);
    String panLast4Digits = extractPanLast4DigitsFromToken(token);

    if (mobileOpt.isEmpty() || mobileOpt.get().isBlank()) {
      log.error("Mobile Number is missing or blank");
      return safeBuildError(exchange, HttpStatus.FORBIDDEN, DEFAULT_ERROR_MESSAGE);
    }
    if (dateOfBirthOpt.isEmpty() || dateOfBirthOpt.get().isBlank()) {
      log.error("Date of birth is missing or blank");
      return safeBuildError(exchange, HttpStatus.FORBIDDEN, DEFAULT_ERROR_MESSAGE);
    }
    String dateOfBirth = dateOfBirthOpt.get();
    String mobileNumber = mobileOpt.get();

    return extractValidPathVariables(path)
        .map(
            vars ->
                validatePathVariablesSequentially(
                    exchange, chain, mobileNumber, dateOfBirth, panLast4Digits, vars, 0))
        .orElseGet(() -> chain.filter(exchange));
  }

  /**
   * This method checks if the given request path matches any pattern in the {@code pathRegistry}.
   * If a match is found, it extracts URI variables from the path and filters out only those that
   * are considered valid for downstream validation (based on {@link ValidationKey}).
   */
  public Optional<List<Map.Entry<String, String>>> extractValidPathVariables(String requestPath) {
    return pathRegistry.getProtectedPatterns().stream()
        .filter(pattern -> pattern.matches(PathContainer.parsePath(requestPath)))
        .findFirst()
        .map(
            pattern ->
                Objects.requireNonNull(
                        pattern.matchAndExtract(PathContainer.parsePath(requestPath)))
                    .getUriVariables())
        .map(this::filterValidKeys);
  }

  /***
   * <p>This method processes a map of path variables and returns a filtered list where: - Only keys
   * that map to a known {@link ValidationKey} are retained - Leading zeros are trimmed from the
   * corresponding values
   */
  private List<Map.Entry<String, String>> filterValidKeys(Map<String, String> pathVars) {
    return pathVars.entrySet().stream()
        .filter(e -> ValidationKey.fromOptionalKey(e.getKey()).isPresent())
        .map(e -> Map.entry(e.getKey(), trimLeadingZeros(e.getValue())))
        .toList();
  }

  /**
   * Recursively validates each path variable (e.g., leadId, loanAccountNumber) against the
   * authenticated user's mobile number.
   */
  private Mono<Void> validatePathVariablesSequentially(
      ServerWebExchange exchange,
      WebFilterChain chain,
      String mobileNumber,
      String dateOfBirth,
      String panLast4Digits,
      List<Map.Entry<String, String>> entries,
      int index) {

    if (index >= entries.size()) {
      return chain.filter(exchange); // All validations passed
    }

    Map.Entry<String, String> current = entries.get(index);

    ValidationKey validationKey;
    try {
      validationKey = ValidationKey.fromKey(current.getKey());
    } catch (IllegalArgumentException e) {
      return safeBuildError(exchange, HttpStatus.BAD_REQUEST, e.getMessage());
    }

    String value = current.getValue();

    return preValidationService
        .validatePathVariables(
            validationKey.getKey(), value, mobileNumber, dateOfBirth, panLast4Digits)
        .flatMap(
            isValid -> {
              if (Boolean.TRUE.equals(isValid)) {
                // Proceed to next path variable validation
                return validatePathVariablesSequentially(
                    exchange, chain, mobileNumber, dateOfBirth, panLast4Digits, entries, index + 1);
              }

              return safeBuildError(exchange, HttpStatus.FORBIDDEN, UNAUTHORIZED);
            })
        .onErrorResume(
            ex -> {
              log.error("Internal error during authorization: " + ex.getMessage());

              if (ex instanceof IllegalArgumentException) {
                return safeBuildError(exchange, HttpStatus.BAD_REQUEST, ex.getMessage());
              }

              return safeBuildError(
                  exchange, HttpStatus.FORBIDDEN, "Internal error during authorization");
            });
  }

  /** Extracts the JWT token from the request's cookie header. */
  private String extractTokenFromCookie(ServerHttpRequest request) {
    String cookieHeader = request.getHeaders().getFirst(HttpHeaders.COOKIE);

    if (cookieHeader == null) return null;

    return Arrays.stream(cookieHeader.split(";"))
        .map(String::trim)
        .filter(cookie -> cookie.startsWith(JWT_COOKIE_NAME + "="))
        .map(cookie -> cookie.substring(JWT_COOKIE_NAME.length() + 1))
        .findFirst()
        .orElse(null);
  }

  /** Extracts the Mobile Number from the token. */
  private Optional<String> extractMobileNumberFromToken(String token) {
    try {
      SecretKey secretKey = getKeyFromString(JWT_BASE64_SECRET_KEY);
      String decryptedToken = EncryptionUtil.decryptToken(token, secretKey);
      String mobile = jwtTokenUtil.extractMobileNumber(decryptedToken);
      return Optional.ofNullable(normalizeMobile(mobile));

    } catch (NoSuchPaddingException
        | NoSuchAlgorithmException
        | InvalidKeyException
        | InvalidAlgorithmParameterException
        | javax.crypto.BadPaddingException
        | javax.crypto.IllegalBlockSizeException e) {
      log.warn("[TOKEN DECRYPTION FAILED - EXPECTED CRYPTO ISSUE] {}", e.getMessage());
      return Optional.empty();

    } catch (IllegalArgumentException e) {
      log.warn("[TOKEN DECRYPTION FAILED - INVALID ARGUMENT] {}", e.getMessage());
      return Optional.empty();

    } catch (RuntimeException e) {
      log.error("[TOKEN DECRYPTION FAILED - UNEXPECTED RUNTIME ERROR]", e);
      return Optional.empty();
    }
  }

  private Optional<String> extractDOBFromToken(String token) {
    try {
      SecretKey secretKey = getKeyFromString(JWT_BASE64_SECRET_KEY);
      String decryptedToken = EncryptionUtil.decryptToken(token, secretKey);
      String dateOfBirth = jwtTokenUtil.extractDOB(decryptedToken);
      return Optional.ofNullable(dateOfBirth);
    } catch (Exception e) {
      log.error("[TOKEN DECRYPTION FAILED] {}", e.getMessage());
      return Optional.empty();
    }
  }

  private String extractPanLast4DigitsFromToken(String token) {
    try {
      SecretKey secretKey = getKeyFromString(JWT_BASE64_SECRET_KEY);
      String decryptedToken = EncryptionUtil.decryptToken(token, secretKey);
      String panLast4Digits = jwtTokenUtil.extractPanLast4Digits(decryptedToken);
      return panLast4Digits;
    } catch (Exception e) {
      log.error("[TOKEN DECRYPTION FAILED] {}", e.getMessage());
      return null;
    }
  }

  /** Normalizes mobile number by removing country code prefix (e.g., "91"). */
  private String normalizeMobile(String mobileNumber) {
    if (mobileNumber != null && mobileNumber.startsWith("91") && mobileNumber.length() > 10) {
      return mobileNumber.substring(2);
    }
    return mobileNumber;
  }

  private Mono<Void> safeBuildError(ServerWebExchange exchange, HttpStatus status, String msg) {
    return exchange.getResponse().isCommitted()
        ? Mono.fromRunnable(() -> log.warn("[COMMITTED] Can't write error response: {}", msg))
        : buildErrorResponse(exchange, status, msg);
  }

  /**
   * Builds a structured JSON error response using ResponseDTO and writes it to the response body.
   */
  private Mono<Void> buildErrorResponse(
      ServerWebExchange exchange, HttpStatus status, String message) {
    ServerHttpResponse response = exchange.getResponse();

    // Prevent writing if response already committed
    if (response.isCommitted()) {
      log.warn("[JWT] Skipping error response: response already committed");
      return Mono.empty();
    }

    return Mono.deferContextual(
        context -> {
          String traceId = context.getOrDefault(TRACE_ID, "N/A");

          ResponseDTO errorResponse =
              ResponseDTO.builder().status(ERROR).message(message).traceId(traceId).build();

          byte[] responseBytes;
          try {
            responseBytes = objectMapper.writeValueAsBytes(errorResponse);
          } catch (JsonProcessingException e) {
            log.error("Failed to serialize error response", e);
            responseBytes =
                "{\"status\":\"error\",\"message\":\"Internal serialization error\"}"
                    .getBytes(StandardCharsets.UTF_8);
          }

          response.setStatusCode(status);
          response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

          DataBuffer buffer = response.bufferFactory().wrap(responseBytes);
          return response.writeWith(Mono.just(buffer));
        });
  }

  private String trimLeadingZeros(String input) {
    return input.replaceFirst("^0+(?!$)", "");
  }
}
