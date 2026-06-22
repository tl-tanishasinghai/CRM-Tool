package com.trillionloans.los.service.impl;

import com.trillionloans.los.api.partner.AnalyticApi;
import com.trillionloans.los.api.partner.KarzaApi;
import com.trillionloans.los.constant.KycValidationVendors;
import com.trillionloans.los.model.KarzaNameSimilarityRequest;
import com.trillionloans.los.model.dto.MatchingScoreDTO;
import com.trillionloans.los.model.request.AnalyticFaceSimilarityRequest;
import com.trillionloans.los.model.request.AnalyticNameSimilarityRequest;
import com.trillionloans.los.model.request.KarzaFaceSimilarityRequest;
import com.trillionloans.los.model.request.KarzaNameSimilarityResponse;
import com.trillionloans.los.model.response.AnalyticFaceSimilarityResponse;
import com.trillionloans.los.model.response.AnalyticNameSimilarityResponse;
import com.trillionloans.los.model.response.KarzaFaceSimilarityResponse;
import com.trillionloans.los.service.AadhaarXmlValidationsService;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service implementation for Aadhaar XML validations including name and face matching.
 *
 * <p>This service provides both parallel and sequential execution strategies for matching
 * operations:
 *
 * <ul>
 *   <li><b>Parallel Execution:</b> Calls both Trillion Analytic and Karza APIs simultaneously,
 *       returns scores from both vendors (with null values for failed calls)
 *   <li><b>Sequential Execution:</b> Calls APIs based on priority with fallback mechanism, returns
 *       single score from the successful vendor
 * </ul>
 *
 * <p>Supports two types of matching:
 *
 * <ul>
 *   <li>Name Similarity - Compares two names and returns a matching score
 *   <li>Face Similarity - Compares two face images (base64) and returns a matching score
 * </ul>
 *
 * @author @sofiyan
 * @version 1.0
 * @since 2025-12-02
 */
@AllArgsConstructor
@Service
@Slf4j
public class AadhaarXmlValidationsServiceImpl implements AadhaarXmlValidationsService {

  /** Karza API success status code. Any other status code indicates a failure. */
  private static final int KARZA_SUCCESS_STATUS_CODE = 101;

  private final AnalyticApi analyticApi;

  private final KarzaApi karzaApi;

  // ==================== Public API Methods ====================

  /**
   * Executes name similarity check on both Trillion Analytic and Karza APIs in parallel.
   *
   * <p><b>Execution Strategy:</b> Both APIs are called simultaneously without blocking each other.
   * If one API fails, it returns a null score for that vendor while the other continues execution.
   *
   * @param nameOne the first name to compare
   * @param nameTwo the second name to compare
   * @return Mono of MatchingScoreDTO containing two MatchingScore objects (one for each vendor)
   *     with null scores for any failed API calls
   * @see #sequenceNameMatchExecution(KycValidationVendors, String, String, boolean, String, String)
   *     for sequential execution
   */
  public Mono<MatchingScoreDTO> parallelNameMatchExecution(
      String nameOne, String nameTwo, String clientId, String loanId) {
    log.info(
        "[KYC_QC] Starting parallel name match execution for names: '{}' and '{}'",
        nameOne,
        nameTwo);

    // Build vendor-specific request objects
    AnalyticNameSimilarityRequest analyticRequest = buildAnalyticNameRequest(nameOne, nameTwo);
    KarzaNameSimilarityRequest karzaRequest = buildKarzaNameRequest(nameOne, nameTwo);

    // Execute both API calls with independent error handling
    Mono<MatchingScoreDTO.MatchingScore> trillionScore =
        executeWithErrorHandling(
            analyticApi.checkNameSimilarity(analyticRequest, clientId, loanId),
            this::extractAnalyticNameScore,
            KycValidationVendors.TRILLION,
            "name similarity");

    Mono<MatchingScoreDTO.MatchingScore> karzaScore =
        executeWithErrorHandling(
            karzaApi.checkNameSimilarity(
                karzaRequest, clientId, loanId, "kyc_validation", "KYC_QC"),
            this::extractKarzaNameScore,
            KycValidationVendors.KARZA,
            "name similarity");

    // Zip results and build response with both scores
    return buildParallelResponse(trillionScore, karzaScore, "name");
  }

  /**
   * Executes name similarity check sequentially based on vendor priority with fallback.
   *
   * <p><b>Execution Strategy:</b>
   *
   * <ul>
   *   <li>Calls the priority vendor API first
   *   <li>If successful, returns the score from that vendor
   *   <li>If fails, falls back to the alternative vendor
   *   <li>If both fail, returns null score from the fallback vendor
   * </ul>
   *
   * @param validationPriority the vendor to try first (KARZA or TRILLION)
   * @param nameOne the first name to compare
   * @param nameTwo the second name to compare
   * @return Mono of MatchingScoreDTO containing single MatchingScore from the successful vendor (or
   *     null score if both vendors fail)
   * @see #parallelNameMatchExecution(String, String, String, String) for parallel execution
   */
  public Mono<MatchingScoreDTO> sequenceNameMatchExecution(
      KycValidationVendors validationPriority,
      String nameOne,
      String nameTwo,
      boolean fallbackEnabled,
      String clientId,
      String loanId) {
    log.info(
        "[KYC_QC] Starting sequence name match execution with priority: '{}', fallbackEnabled: {}"
            + " for names: '{}' and '{}'",
        validationPriority,
        fallbackEnabled,
        nameOne,
        nameTwo);

    // Build vendor-specific request objects
    AnalyticNameSimilarityRequest analyticRequest = buildAnalyticNameRequest(nameOne, nameTwo);
    KarzaNameSimilarityRequest karzaRequest = buildKarzaNameRequest(nameOne, nameTwo);

    // Execute based on priority with fallback mechanism
    if (validationPriority == KycValidationVendors.KARZA) {
      return executeKarzaNameMatchWithFallback(
          karzaRequest, analyticRequest, fallbackEnabled, clientId, loanId);
    } else {
      return executeTrillionNameMatchWithFallback(
          analyticRequest, karzaRequest, fallbackEnabled, clientId, loanId);
    }
  }

  // ==================== Name Matching - Request Builders ====================

  /**
   * Builds request object for Trillion Analytic name similarity API.
   *
   * @param nameOne first name to compare
   * @param nameTwo second name to compare
   * @return configured AnalyticNameSimilarityRequest
   */
  private AnalyticNameSimilarityRequest buildAnalyticNameRequest(String nameOne, String nameTwo) {
    return AnalyticNameSimilarityRequest.builder().name1(nameOne).name2(nameTwo).build();
  }

  /**
   * Builds request object for Karza name similarity API with optimal settings.
   *
   * <p>Configuration:
   *
   * <ul>
   *   <li>Type: individual - for person name matching
   *   <li>AllowPartialMatch: true - enables partial name matching
   *   <li>SuppressReorderPenalty: false - applies penalty for name order differences
   * </ul>
   *
   * @param nameOne first name to compare
   * @param nameTwo second name to compare
   * @return configured KarzaNameSimilarityRequest
   */
  private KarzaNameSimilarityRequest buildKarzaNameRequest(String nameOne, String nameTwo) {
    return KarzaNameSimilarityRequest.builder()
        .name1(nameOne)
        .name2(nameTwo)
        .type(KarzaNameSimilarityRequest.Type.individual)
        .preset("s")
        .allowPartialMatch(true)
        .suppressReorderPenalty(false)
        .clientData(
            KarzaNameSimilarityRequest.ClientData.builder()
                .caseId(UUID.randomUUID().toString())
                .build())
        .build();
  }

  // ==================== Name Matching - Sequential Execution ====================

  /**
   * Executes Karza name similarity API with Trillion Analytic as fallback.
   *
   * @param karzaRequest the Karza API request
   * @param analyticRequest the Analytic API request (used if Karza fails)
   * @return Mono of MatchingScoreDTO with single score from successful vendor
   */
  private Mono<MatchingScoreDTO> executeKarzaNameMatchWithFallback(
      KarzaNameSimilarityRequest karzaRequest,
      AnalyticNameSimilarityRequest analyticRequest,
      boolean fallbackEnabled,
      String clientId,
      String loanId) {

    return karzaApi
        .checkNameSimilarity(karzaRequest, clientId, loanId, "kyc_validation", "KYC_QC")
        .map(
            karzaResponse -> {
              // Extract score from Karza response
              Double score = extractKarzaNameScore(karzaResponse);
              log.info("[KYC_QC] Karza API succeeded with score: {}", score);
              return buildMatchingScoreDTO(score, KycValidationVendors.KARZA);
            })
        .onErrorResume(
            error -> {
              log.error("[KYC_QC][Error] Karza API failed: {}", error.getMessage());

              // Check if fallback is enabled before falling back
              if (!fallbackEnabled) {
                log.info(
                    "[KYC_QC] Name fallback is disabled. Marking as CAN_NOT_BE_DONE"
                        + " (returning null score).");
                return Mono.just(buildMatchingScoreDTO(null, KycValidationVendors.KARZA));
              }

              log.info("[KYC_QC] Name fallback is enabled. Falling back to Trillion Analytic API.");
              return executeTrillionNameMatchAsFallback(
                  analyticRequest, KycValidationVendors.TRILLION, clientId, loanId);
            });
  }

  /**
   * Executes Trillion Analytic name similarity API with Karza as fallback.
   *
   * @param analyticRequest the Analytic API request
   * @param karzaRequest the Karza API request (used if Trillion fails)
   * @return Mono of MatchingScoreDTO with single score from successful vendor
   */
  private Mono<MatchingScoreDTO> executeTrillionNameMatchWithFallback(
      AnalyticNameSimilarityRequest analyticRequest,
      KarzaNameSimilarityRequest karzaRequest,
      boolean fallbackEnabled,
      String clientId,
      String loanId) {

    return analyticApi
        .checkNameSimilarity(analyticRequest, clientId, loanId)
        .map(
            analyticResponse -> {
              // Extract score from Trillion Analytic response and convert to decimal
              Double score = extractAnalyticNameScore(analyticResponse);
              log.info("[KYC_QC] Trillion Analytic API succeeded with score: {}", score);
              return buildMatchingScoreDTO(score, KycValidationVendors.TRILLION);
            })
        .onErrorResume(
            error -> {
              log.error("[KYC_QC][Error] Trillion Analytic API failed: {}", error.getMessage());

              // Check if fallback is enabled before falling back
              if (!fallbackEnabled) {
                log.info(
                    "[KYC_QC] Name fallback is disabled. Marking as CAN_NOT_BE_DONE"
                        + " (returning null score).");
                return Mono.just(buildMatchingScoreDTO(null, KycValidationVendors.TRILLION));
              }

              log.info("[KYC_QC] Name fallback is enabled. Falling back to Karza API.");
              return executeKarzaNameMatchAsFallback(
                  karzaRequest, KycValidationVendors.KARZA, clientId, loanId);
            });
  }

  /**
   * Executes Trillion Analytic API as fallback (when primary vendor failed). If this also fails,
   * returns null score with the fallback vendor.
   *
   * @param analyticRequest the Analytic API request
   * @param fallbackVendor the vendor to use if both APIs fail
   * @return Mono of MatchingScoreDTO with score or null if failed
   */
  private Mono<MatchingScoreDTO> executeTrillionNameMatchAsFallback(
      AnalyticNameSimilarityRequest analyticRequest,
      KycValidationVendors fallbackVendor,
      String clientId,
      String loanId) {

    return analyticApi
        .checkNameSimilarity(analyticRequest, clientId, loanId)
        .map(
            analyticResponse -> {
              Double score = extractAnalyticNameScore(analyticResponse);
              log.info("[KYC_QC] Trillion Analytic API (fallback) succeeded with score: {}", score);
              return buildMatchingScoreDTO(score, KycValidationVendors.TRILLION);
            })
        .onErrorResume(
            fallbackError -> {
              // Both APIs failed - return null score
              log.error(
                  "Trillion Analytic API (fallback) also failed: {}", fallbackError.getMessage());
              return Mono.just(buildMatchingScoreDTO(null, fallbackVendor));
            });
  }

  /**
   * Executes Karza API as fallback (when primary vendor failed). If this also fails, returns null
   * score with the fallback vendor.
   *
   * @param karzaRequest the Karza API request
   * @param fallbackVendor the vendor to use if both APIs fail
   * @return Mono of MatchingScoreDTO with score or null if failed
   */
  private Mono<MatchingScoreDTO> executeKarzaNameMatchAsFallback(
      KarzaNameSimilarityRequest karzaRequest,
      KycValidationVendors fallbackVendor,
      String clientId,
      String loanId) {

    return karzaApi
        .checkNameSimilarity(karzaRequest, clientId, loanId, "kyc_validation", "KYC_QC")
        .map(
            karzaResponse -> {
              Double score = extractKarzaNameScore(karzaResponse);
              log.info("[KYC_QC] Karza API (fallback) succeeded with score: {}", score);
              return buildMatchingScoreDTO(score, KycValidationVendors.KARZA);
            })
        .onErrorResume(
            fallbackError -> {
              // Both APIs failed - return null score
              log.error("Karza API (fallback) also failed: {}", fallbackError.getMessage());
              return Mono.just(buildMatchingScoreDTO(null, fallbackVendor));
            });
  }

  // ==================== Score Extractors ====================

  /**
   * Extracts name similarity score from Karza API response after validating the status code.
   *
   * @param karzaNameSimilarityResponse the Karza name similarity response
   * @return the matching score
   * @throws RuntimeException if Karza statusCode is not 101 (success)
   */
  private Double extractKarzaNameScore(KarzaNameSimilarityResponse karzaNameSimilarityResponse) {
    int statusCode = karzaNameSimilarityResponse.getStatusCode();
    if (statusCode != KARZA_SUCCESS_STATUS_CODE) {
      log.error(
          "[KYC_QC][Error] Karza name similarity API returned non-success statusCode: {},"
              + " requestId: {}",
          statusCode,
          karzaNameSimilarityResponse.getRequestId());
      throw new RuntimeException(
          "Karza name similarity API returned non-success statusCode: " + statusCode);
    }
    return (karzaNameSimilarityResponse.getResult() != null)
        ? karzaNameSimilarityResponse.getResult().getScore()
        : null;
  }

  /**
   * Extracts and converts name similarity score from Analytic API response. Converts percentage
   * (0-100) to decimal (0-1) by dividing by 100.
   *
   * @param analyticNameSimilarityResponse the Analytic name similarity response
   * @return the matching score in decimal format, or null if not present
   */
  private Double extractAnalyticNameScore(
      AnalyticNameSimilarityResponse analyticNameSimilarityResponse) {
    Double percentScore = analyticNameSimilarityResponse.getNameMatchPercent();
    if (percentScore != null) {
      return percentScore / 100.0;
    }
    return null;
  }

  /**
   * Extracts and converts face similarity score from Analytic API response. Converts percentage
   * (0-100) to decimal (0-1) by dividing by 100.
   *
   * @param analyticFaceSimilarityResponse the Analytic face similarity response
   * @return the matching score in decimal format, or null if not present
   */
  private Double extractAnalyticFaceScore(
      AnalyticFaceSimilarityResponse analyticFaceSimilarityResponse) {
    Double percentScore = analyticFaceSimilarityResponse.getFaceMatchPercent();
    if (percentScore != null) {
      return percentScore / 100.0;
    }
    return null;
  }

  /**
   * Builds a MatchingScoreDTO with a single score entry for sequential execution.
   *
   * @param score the matching score (can be null if API failed)
   * @param vendor the vendor that provided the score
   * @return MatchingScoreDTO containing single score entry
   */
  private MatchingScoreDTO buildMatchingScoreDTO(Double score, KycValidationVendors vendor) {
    MatchingScoreDTO.MatchingScore matchingScore =
        MatchingScoreDTO.MatchingScore.builder().score(score).vendor(vendor).build();
    return MatchingScoreDTO.builder().matchingScores(Map.of(vendor, matchingScore)).build();
  }

  /**
   * Executes face similarity check on both Trillion Analytic and Karza APIs in parallel.
   *
   * <p><b>Execution Strategy:</b> Both APIs are called simultaneously without blocking each other.
   * If one API fails, it returns a null score for that vendor while the other continues execution.
   *
   * @param face1Base64 the first face image in base64 encoded format
   * @param face2Base64 the second face image in base64 encoded format
   * @return Mono of MatchingScoreDTO containing two MatchingScore objects (one for each vendor)
   *     with null scores for any failed API calls
   * @see #sequenceFaceMatchExecution(KycValidationVendors, String, String, boolean, String, String)
   *     for sequential execution
   */
  public Mono<MatchingScoreDTO> parallelFaceMatchExecution(
      String face1Base64, String face2Base64, String clientId, String loanId) {
    log.info("[KYC_QC] Starting parallel face match execution for provided face images");

    // Build vendor-specific request objects
    AnalyticFaceSimilarityRequest analyticRequest =
        buildAnalyticFaceRequest(face1Base64, face2Base64);
    KarzaFaceSimilarityRequest karzaRequest = buildKarzaFaceRequest(face1Base64, face2Base64);

    // Execute both API calls with independent error handling
    Mono<MatchingScoreDTO.MatchingScore> trillionScore =
        executeWithErrorHandling(
            analyticApi.checkFaceSimilarity(analyticRequest, clientId, loanId),
            this::extractAnalyticFaceScore,
            KycValidationVendors.TRILLION,
            "face similarity");

    Mono<MatchingScoreDTO.MatchingScore> karzaScore =
        executeWithErrorHandling(
            karzaApi.checkFaceSimilarity(karzaRequest, clientId, loanId),
            this::extractKarzaFaceScore,
            KycValidationVendors.KARZA,
            "face similarity");

    // Zip results and build response with both scores
    return buildParallelResponse(trillionScore, karzaScore, "face");
  }

  /**
   * Executes face similarity check sequentially based on vendor priority with fallback.
   *
   * <p><b>Execution Strategy:</b>
   *
   * <ul>
   *   <li>Calls the priority vendor API first
   *   <li>If successful, returns the score from that vendor
   *   <li>If fails, falls back to the alternative vendor
   *   <li>If both fail, returns null score from the fallback vendor
   * </ul>
   *
   * @param validationPriority the vendor to try first (KARZA or TRILLION)
   * @param face1Base64 the first face image in base64 encoded format
   * @param face2Base64 the second face image in base64 encoded format
   * @return Mono of MatchingScoreDTO containing single MatchingScore from the successful vendor (or
   *     null score if both vendors fail)
   * @see #parallelFaceMatchExecution(String, String, String, String) for parallel execution
   */
  public Mono<MatchingScoreDTO> sequenceFaceMatchExecution(
      KycValidationVendors validationPriority,
      String face1Base64,
      String face2Base64,
      boolean fallbackEnabled,
      String clientId,
      String loanId) {
    log.info(
        "[KYC_QC] Starting sequence face match execution with priority: '{}', fallbackEnabled: {}",
        validationPriority,
        fallbackEnabled);

    // Build vendor-specific request objects
    AnalyticFaceSimilarityRequest analyticRequest =
        buildAnalyticFaceRequest(face1Base64, face2Base64);
    KarzaFaceSimilarityRequest karzaRequest = buildKarzaFaceRequest(face1Base64, face2Base64);

    log.info("[KYC_QC] karza and analytic face match request body built.");

    // Execute based on priority with fallback mechanism
    if (validationPriority == KycValidationVendors.KARZA) {
      return executeKarzaFaceMatchWithFallback(
          karzaRequest, analyticRequest, fallbackEnabled, clientId, loanId);
    } else {
      return executeTrillionFaceMatchWithFallback(
          analyticRequest, karzaRequest, fallbackEnabled, clientId, loanId);
    }
  }

  // ==================== Face Matching - Request Builders ====================

  /**
   * Builds request object for Trillion Analytic face similarity API.
   *
   * @param face1Base64 first face image in base64 format
   * @param face2Base64 second face image in base64 format
   * @return configured AnalyticFaceSimilarityRequest
   */
  private AnalyticFaceSimilarityRequest buildAnalyticFaceRequest(
      String face1Base64, String face2Base64) {
    return AnalyticFaceSimilarityRequest.builder().image1(face1Base64).image2(face2Base64).build();
  }

  /**
   * Builds request object for Karza face similarity API.
   *
   * <p>Configuration:
   *
   * <ul>
   *   <li>GetNumberOfFaces: false - only match faces, don't count them
   * </ul>
   *
   * @param face1Base64 first face image in base64 format
   * @param face2Base64 second face image in base64 format
   * @return configured KarzaFaceSimilarityRequest
   */
  private KarzaFaceSimilarityRequest buildKarzaFaceRequest(String face1Base64, String face2Base64) {
    return KarzaFaceSimilarityRequest.builder()
        .image1B64(face1Base64)
        .image2B64(face2Base64)
        .getNumberOfFaces(false)
        .build();
  }

  // ==================== Face Matching - Sequential Execution ====================

  /**
   * Executes Karza face similarity API with Trillion Analytic as fallback.
   *
   * @param karzaRequest the Karza API request
   * @param analyticRequest the Analytic API request (used if Karza fails)
   * @return Mono of MatchingScoreDTO with single score from successful vendor
   */
  private Mono<MatchingScoreDTO> executeKarzaFaceMatchWithFallback(
      KarzaFaceSimilarityRequest karzaRequest,
      AnalyticFaceSimilarityRequest analyticRequest,
      boolean fallbackEnabled,
      String clientId,
      String loanId) {
    log.info("[KYC_QC] Karza face match execution started");

    return Mono.deferContextual(
        contextView ->
            karzaApi
                .checkFaceSimilarity(karzaRequest, clientId, loanId)
                .flatMap(
                    karzaResponse -> {
                      log.info("[KYC_QC] Karza face match api responded successfully");

                      // Extract match score from Karza response
                      Double score = extractKarzaFaceScore(karzaResponse);
                      log.info("[KYC_QC] Karza API succeeded with face match score: {}", score);

                      return Mono.just(buildMatchingScoreDTO(score, KycValidationVendors.KARZA));
                    })
                .onErrorResume(
                    error -> {
                      log.error("[KYC_QC][Error] Karza API failed: {}", error.getMessage());

                      // Check if fallback is enabled before falling back
                      if (!fallbackEnabled) {
                        log.info(
                            "[KYC_QC] Face fallback is disabled. Marking as CAN_NOT_BE_DONE"
                                + " (returning null score).");
                        return Mono.just(buildMatchingScoreDTO(null, KycValidationVendors.KARZA));
                      }

                      log.info(
                          "[KYC_QC] Face fallback is enabled. Falling back to Trillion"
                              + " Analytic API.");
                      return executeTrillionFaceMatchAsFallback(
                          analyticRequest, KycValidationVendors.TRILLION, clientId, loanId);
                    }));
  }

  /**
   * Executes Trillion Analytic face similarity API with Karza as fallback.
   *
   * <p>Falls back to Karza API in the following scenarios:
   *
   * <ul>
   *   <li>Trillion API returns an error (non-200 response)
   *   <li>Trillion API returns 200 but faceMatchPercent is null or invalid
   * </ul>
   *
   * @param analyticRequest the Analytic API request
   * @param karzaRequest the Karza API request (used if Trillion fails or returns invalid score)
   * @return Mono of MatchingScoreDTO with single score from successful vendor
   */
  private Mono<MatchingScoreDTO> executeTrillionFaceMatchWithFallback(
      AnalyticFaceSimilarityRequest analyticRequest,
      KarzaFaceSimilarityRequest karzaRequest,
      boolean fallbackEnabled,
      String clientId,
      String loanId) {

    return analyticApi
        .checkFaceSimilarity(analyticRequest, clientId, loanId)
        .flatMap(
            analyticResponse -> {
              // Extract face match percentage from Trillion Analytic response and convert to
              // decimal
              Double score = extractAnalyticFaceScore(analyticResponse);
              log.info("[KYC_QC] Trillion Analytic API succeeded with face match score: {}", score);

              // Check if score is null or invalid - if so, fallback to Karza
              if (score == null) {
                log.info(
                    "[KYC_QC] Trillion Analytic API returned 200 but faceMatchPercent is null or"
                        + " invalid");

                // Check if fallback is enabled before falling back
                if (!fallbackEnabled) {
                  log.info(
                      "[KYC_QC] Face fallback is disabled. Marking as CAN_NOT_BE_DONE"
                          + " (returning null score).");
                  return Mono.just(buildMatchingScoreDTO(null, KycValidationVendors.TRILLION));
                }

                log.info("[KYC_QC] Face fallback is enabled. Falling back to Karza API.");
                return executeKarzaFaceMatchAsFallback(
                    karzaRequest, KycValidationVendors.KARZA, clientId, loanId);
              }

              return Mono.just(buildMatchingScoreDTO(score, KycValidationVendors.TRILLION));
            })
        .onErrorResume(
            error -> {
              log.error("[KYC_QC][Error] Trillion Analytic API failed: {}", error.getMessage());

              // Check if fallback is enabled before falling back
              if (!fallbackEnabled) {
                log.info(
                    "[KYC_QC] Face fallback is disabled. Marking as CAN_NOT_BE_DONE"
                        + " (returning null score).");
                return Mono.just(buildMatchingScoreDTO(null, KycValidationVendors.TRILLION));
              }

              log.info("[KYC_QC] Face fallback is enabled. Falling back to Karza API.");
              return executeKarzaFaceMatchAsFallback(
                  karzaRequest, KycValidationVendors.KARZA, clientId, loanId);
            });
  }

  /**
   * Executes Trillion Analytic API as fallback for face matching. If this also fails or returns
   * null/invalid score, returns null score with the fallback vendor.
   *
   * @param analyticRequest the Analytic API request
   * @param fallbackVendor the vendor to use if both APIs fail
   * @return Mono of MatchingScoreDTO with score or null if failed
   */
  private Mono<MatchingScoreDTO> executeTrillionFaceMatchAsFallback(
      AnalyticFaceSimilarityRequest analyticRequest,
      KycValidationVendors fallbackVendor,
      String clientId,
      String loanId) {

    return analyticApi
        .checkFaceSimilarity(analyticRequest, clientId, loanId)
        .map(
            analyticResponse -> {
              Double score = extractAnalyticFaceScore(analyticResponse);

              // Log warning if score is null (200 response with null/invalid faceMatchPercent)
              if (score == null) {
                log.info(
                    "[KYC_QC] Trillion Analytic API (fallback) returned 200 but faceMatchPercent is"
                        + " null or invalid. Both vendors failed to return valid score.");
              } else {
                log.info(
                    "[KYC_QC] Trillion Analytic API (fallback) succeeded with face match score: {}",
                    score);
              }

              return buildMatchingScoreDTO(score, KycValidationVendors.TRILLION);
            })
        .onErrorResume(
            fallbackError -> {
              // Both APIs failed - return null score
              log.error(
                  "[KYC_QC] Trillion Analytic API (fallback) also failed: {}",
                  fallbackError.getMessage());
              return Mono.just(buildMatchingScoreDTO(null, fallbackVendor));
            });
  }

  /**
   * Executes Karza API as fallback for face matching. If this also fails, returns null score with
   * the fallback vendor.
   *
   * @param karzaRequest the Karza API request
   * @param fallbackVendor the vendor to use if both APIs fail
   * @return Mono of MatchingScoreDTO with score or null if failed
   */
  private Mono<MatchingScoreDTO> executeKarzaFaceMatchAsFallback(
      KarzaFaceSimilarityRequest karzaRequest,
      KycValidationVendors fallbackVendor,
      String clientId,
      String loanId) {

    return karzaApi
        .checkFaceSimilarity(karzaRequest, clientId, loanId)
        .map(
            karzaResponse -> {
              Double score = extractKarzaFaceScore(karzaResponse);
              log.info("[KYC_QC] Karza API (fallback) succeeded with face match score: {}", score);
              return buildMatchingScoreDTO(score, KycValidationVendors.KARZA);
            })
        .onErrorResume(
            fallbackError -> {
              // Both APIs failed - return null score
              log.error(
                  "[KYC_QC] Karza API (fallback) also failed: {}", fallbackError.getMessage());
              return Mono.just(buildMatchingScoreDTO(null, fallbackVendor));
            });
  }

  /**
   * Extracts face similarity score from Karza API response after validating the status code.
   *
   * @param response the Karza face similarity response
   * @return the normalized match score (0.0–1.0), or null if result is not present
   * @throws RuntimeException if Karza statusCode is not 101 (success)
   */
  private Double extractKarzaFaceScore(KarzaFaceSimilarityResponse response) {
    if (response == null) {
      return null;
    }

    Integer statusCode = response.getStatusCode();
    if (statusCode == null || statusCode != KARZA_SUCCESS_STATUS_CODE) {
      log.error(
          "[KYC_QC][Error] Karza face similarity API returned non-success statusCode: {},"
              + " requestId: {}",
          statusCode,
          response.getRequestId());
      throw new RuntimeException(
          "Karza face similarity API returned non-success statusCode: " + statusCode);
    }

    if (response.getResult() == null) {
      return null;
    }

    Double matchScore = response.getResult().getMatchScore();
    return (matchScore != null) ? matchScore / 100.0 : null;
  }

  // ==================== Common Helper Methods ====================

  /**
   * Generic method to execute API call with consistent error handling and score extraction.
   *
   * <p>This method provides a reusable template for calling any similarity API:
   *
   * <ul>
   *   <li>Executes the API call asynchronously
   *   <li>Extracts the score using the provided function
   *   <li>Handles errors by returning null score instead of propagating exception
   * </ul>
   *
   * @param <T> the type of API response
   * @param apiCall the Mono representing the API call
   * @param scoreExtractor function to extract score from response
   * @param vendor the vendor providing the API
   * @param apiType description of the API type (for logging)
   * @return Mono of MatchingScore with score or null if API failed
   */
  private <T> Mono<MatchingScoreDTO.MatchingScore> executeWithErrorHandling(
      Mono<T> apiCall,
      Function<T, Double> scoreExtractor,
      KycValidationVendors vendor,
      String apiType) {

    return apiCall
        .map(
            response -> {
              // Extract score using provided function
              Double score = scoreExtractor.apply(response);
              return buildMatchingScore(score, vendor);
            })
        .onErrorResume(
            error -> {
              // Log error and return null score instead of failing
              log.error(
                  "Error calling {} API for {}: {}",
                  vendor.getDisplayName(),
                  apiType,
                  error.getMessage());
              return Mono.just(buildMatchingScore(null, vendor));
            });
  }

  /**
   * Builds response for parallel execution containing scores from both vendors.
   *
   * <p>This method:
   *
   * <ul>
   *   <li>Waits for both API calls to complete using Mono.zip
   *   <li>Combines both scores into a single response
   *   <li>Logs completion status with both scores
   * </ul>
   *
   * @param trillionScore Mono of Trillion Analytic score
   * @param karzaScore Mono of Karza score
   * @param matchType type of matching (for logging - "name" or "face")
   * @return Mono of MatchingScoreDTO containing both vendor scores
   */
  private Mono<MatchingScoreDTO> buildParallelResponse(
      Mono<MatchingScoreDTO.MatchingScore> trillionScore,
      Mono<MatchingScoreDTO.MatchingScore> karzaScore,
      String matchType) {

    return Mono.zip(trillionScore, karzaScore)
        .map(
            scoreTuple -> {
              // Extract both scores from the tuple
              MatchingScoreDTO.MatchingScore trillion = scoreTuple.getT1();
              MatchingScoreDTO.MatchingScore karza = scoreTuple.getT2();

              // Log completion with both scores
              log.info(
                  "[KYC_QC] Parallel {} match execution completed. Trillion score: {}, Karza score:"
                      + " {}",
                  matchType,
                  trillion.getScore(),
                  karza.getScore());

              // Build response with both scores
              return MatchingScoreDTO.builder()
                  .matchingScores(
                      Map.of(
                          KycValidationVendors.TRILLION, trillion,
                          KycValidationVendors.KARZA, karza))
                  .build();
            });
  }

  /**
   * Builds a single matching score object for parallel execution.
   *
   * @param score the matching score (can be null)
   * @param vendor the vendor that provided the score
   * @return MatchingScore object
   */
  private MatchingScoreDTO.MatchingScore buildMatchingScore(
      Double score, KycValidationVendors vendor) {
    return MatchingScoreDTO.MatchingScore.builder().score(score).vendor(vendor).build();
  }
}
