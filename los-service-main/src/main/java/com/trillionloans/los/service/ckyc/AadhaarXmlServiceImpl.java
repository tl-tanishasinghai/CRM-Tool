package com.trillionloans.los.service.ckyc;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;
import static com.trillionloans.los.constant.StringConstants.DATE_FORMAT;
import static com.trillionloans.los.constant.StringConstants.FACE_FALLBACK;
import static com.trillionloans.los.constant.StringConstants.FACE_MATCH_EXECUTION;
import static com.trillionloans.los.constant.StringConstants.FACE_MATCH_PRIORITY;
import static com.trillionloans.los.constant.StringConstants.FACE_MATCH_THRESHOLD;
import static com.trillionloans.los.constant.StringConstants.FAILED_UPDATING_LEAD_VIA_XML;
import static com.trillionloans.los.constant.StringConstants.KYC_VALIDATION;
import static com.trillionloans.los.constant.StringConstants.NAME_FALLBACK;
import static com.trillionloans.los.constant.StringConstants.NAME_MATCH_EXECUTION;
import static com.trillionloans.los.constant.StringConstants.NAME_MATCH_PRIORITY;
import static com.trillionloans.los.constant.StringConstants.NAME_MATCH_THRESHOLD;
import static com.trillionloans.los.constant.StringConstants.NO_AADHAAR_XML_FOUND;
import static com.trillionloans.los.constant.StringConstants.TRACE_ID;
import static com.trillionloans.los.constant.StringConstants.UPDATE_LEAD_VIA_XML;
import static com.trillionloans.los.constant.StringConstants.XML_VALIDITY;
import static com.trillionloans.los.util.LeadDataUtil.base64ToXmlDecoder;
import static com.trillionloans.los.util.LeadDataUtil.extractXmlFromBase64;
import static com.trillionloans.los.util.LogUtil.logWithContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.constant.*;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.model.dto.*;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.ProductControl;
import com.trillionloans.los.model.entity.ClientKycDetailsEntity;
import com.trillionloans.los.model.entity.KycQcEntity;
import com.trillionloans.los.model.partner.m2p.M2pAddressDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadUpdateDTO;
import com.trillionloans.los.model.request.AadhaarXmlRequest;
import com.trillionloans.los.model.response.ClientImageResponse;
import com.trillionloans.los.model.response.okyc.M2pAadhaarXmlResponseDTO;
import com.trillionloans.los.repository.AadhaarReferenceIdRepository;
import com.trillionloans.los.service.AadhaarXmlValidationsService;
import com.trillionloans.los.service.KycQcService;
import com.trillionloans.los.service.LoanLevelClientDetailsService;
import com.trillionloans.los.service.db.ProductConfigMasterService;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.util.LeadDataUtil;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import reactor.util.context.ContextView;
import reactor.util.function.Tuple2;

@Component
@AllArgsConstructor
@Service
@Slf4j
public class AadhaarXmlServiceImpl implements AadhaarXmlService {

  private final M2PWrapperApi m2PWrapperApi;
  private final Environment environment;
  private final FamilyDetailResolver familyDetailResolver;
  private final ProductConfigMasterService productConfigMasterService;
  private final KafkaEventProducerService eventProducerService;
  private final AadhaarXmlValidationsService aadhaarXmlValidationsService;
  private final AadhaarReferenceIdRepository aadhaarReferenceIdRepository;
  private final KycQcService kycQcService;
  private final ObjectMapper objectMapper;
  private final LoanLevelClientDetailsService loanLevelClientDetailsService;

  @Override
  public Mono<M2pAadhaarXmlResponseDTO> uploadAadhaarXml(
      AadhaarXmlRequest aadhaarXmlRequest,
      String leadId,
      AadhaarXMLType aadhaarXMLType,
      String loanAppId,
      String productCode) {
    return m2PWrapperApi
        .uploadAadhaarXml(aadhaarXmlRequest, leadId, aadhaarXMLType, loanAppId)
        .flatMap(
            response -> {
              String parentTraceId = MDC.get(TRACE_ID);
              handleUpdateClientFromXml(
                      response, aadhaarXmlRequest, leadId, loanAppId, productCode, aadhaarXMLType)
                  .contextWrite(Context.of(TRACE_ID, parentTraceId))
                  .subscribeOn(Schedulers.boundedElastic())
                  .subscribe();
              return Mono.just(response);
            });
  }

  private Mono<M2pAadhaarXmlResponseDTO> handleUpdateClientFromXml(
      M2pAadhaarXmlResponseDTO response,
      AadhaarXmlRequest aadhaarXmlRequest,
      String leadId,
      String loanId,
      String productCode,
      AadhaarXMLType aadhaarXMLType) {

    return Mono.deferContextual(
        contextView -> {

          // Fire-and-forget: Extract and store referenceId prefix asynchronously with context
          // Only for OKYC type
          if (AadhaarXMLType.OKYC.equals(aadhaarXMLType)) {
            storeReferenceIdPrefixAsync(aadhaarXmlRequest.getRequestString(), leadId, contextView);
          }
          // Extract XML and run both updateClientFromXml and kycValidation in parallel
          Mono<AadhaarXmlDetailsDTO> xmlExtraction =
              extractXmlFromBase64(aadhaarXmlRequest.getRequestString())
                  .cache(); // Cache to avoid extracting XML twice

          // Run updateClientFromXml in parallel
          Mono<?> updateClientMono =
              xmlExtraction
                  .flatMap(
                      clientXmlDetailsDTO ->
                          processClientXmlDetails(
                              clientXmlDetailsDTO, leadId, AddressType.AADHAAR.getDisplayName()))
                  .doOnError(e -> log.error(FAILED_UPDATING_LEAD_VIA_XML, leadId, e.getMessage()))
                  .onErrorResume(e -> Mono.empty());

          // Run kycValidation in parallel
          Mono<?> kycValidationMono =
              xmlExtraction
                  .doOnError(
                      e ->
                          log.error(
                              "[{}] failed for lead {}: stackTrace{}",
                              KYC_VALIDATION,
                              leadId,
                              e.getStackTrace()))
                  .flatMap(
                      clientXmlDetailsDTO ->
                          kycValidation(
                              clientXmlDetailsDTO, leadId, loanId, productCode, aadhaarXMLType))
                  .onErrorResume(e -> Mono.empty());

          // Execute both in parallel and return response when both complete
          return Mono.zip(updateClientMono, kycValidationMono)
              .thenReturn(response)
              .contextWrite(context -> context.putAll(contextView));
        });
  }

  /**
   * Asynchronously extracts the first 4 digits of referenceId from Aadhaar XML and stores it in the
   * database. This operation is fire-and-forget and will not affect the main flow.
   */
  private void storeReferenceIdPrefixAsync(
      String base64Xml, String clientId, ContextView parentContext) {
    extractReferenceIdPrefixFromBase64Xml(base64Xml)
        .flatMap(referenceIdPrefix -> saveReferenceIdPrefix(clientId, referenceIdPrefix))
        .contextWrite(ctx -> ctx.putAll(parentContext))
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            savedEntity ->
                logWithContext(
                    parentContext,
                    () ->
                        log.info(
                            "[CLIENT_KYC_DETAILS][AADHAAR_REFERENCE_ID] Successfully stored"
                                + " referenceId prefix for clientId: {}",
                            clientId)),
            error ->
                logWithContext(
                    parentContext,
                    () ->
                        log.error(
                            "[CLIENT_KYC_DETAILS][AADHAAR_REFERENCE_ID] Failed to store referenceId"
                                + " prefix for clientId: {}, error: {}",
                            clientId,
                            error.getMessage())));
  }

  /**
   * Extracts the first 4 digits of referenceId from the base64 encoded Aadhaar XML.
   *
   * @param base64Xml the base64 encoded XML string
   * @return Mono containing the first 4 digits of referenceId, or empty if not found
   */
  private Mono<String> extractReferenceIdPrefixFromBase64Xml(String base64Xml) {
    return Mono.defer(
        () -> {
          try {
            String decodedXml = base64ToXmlDecoder(base64Xml);
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc =
                builder.parse(
                    new ByteArrayInputStream(decodedXml.getBytes(StandardCharsets.UTF_8)));

            String referenceId = doc.getDocumentElement().getAttribute("referenceId");

            if (referenceId == null || referenceId.isBlank()) {
              log.error(
                  "[CLIENT_KYC_DETAILS][AADHAAR_REFERENCE_ID] referenceId is null or blank in XML,"
                      + " skipping storage");
              return Mono.empty();
            }

            if (referenceId.length() < 4) {
              log.error(
                  "[CLIENT_KYC_DETAILS][AADHAAR_REFERENCE_ID] referenceId is too short (length:"
                      + " {}), skipping storage",
                  referenceId.length());
              return Mono.empty();
            }

            return Mono.just(referenceId.substring(0, 4));
          } catch (Exception e) {
            log.error(
                "[CLIENT_KYC_DETAILS][AADHAAR_REFERENCE_ID] Error extracting referenceId from XML:"
                    + " {}",
                e.getMessage());
            return Mono.empty();
          }
        });
  }

  /** Saves the referenceId prefix to the database. */
  private Mono<ClientKycDetailsEntity> saveReferenceIdPrefix(
      String clientId, String referenceIdPrefix) {
    ClientKycDetailsEntity entity =
        ClientKycDetailsEntity.builder()
            .clientId(clientId)
            .documentType(DocumentType.AADHAAR_OKYC)
            .documentId(referenceIdPrefix)
            .build();
    return aadhaarReferenceIdRepository.save(entity);
  }

  public Mono<?> kycValidation(
      AadhaarXmlDetailsDTO clientXmlDetailsDTO,
      String leadId,
      String loanId,
      String productCode,
      AadhaarXMLType aadhaarXMLType) {
    log.info("KYC Validation started for leadId: {}", leadId);

    AadhaarXmlValidationDTO aadhaarXmlValidationDTO =
        AadhaarXmlValidationDTO.builder()
            .clientXmlDetailsDTO(clientXmlDetailsDTO)
            .leadId(leadId)
            .loanId(loanId)
            .aadhaarXMLType(aadhaarXMLType)
            .build();

    return triggerProductControlFlow(aadhaarXmlValidationDTO, productCode, KYC_VALIDATION);
  }

  public <T> Mono<?> triggerProductControlFlow(
      T requestBody, String productCode, String flowIdentifier) {

    // fetching product configuration from database based on product code
    Mono<Tuple2<String, ProductControl>> productConfigTuple =
        productConfigMasterService.getProductConfigMasterData(productCode);
    return productConfigTuple.flatMap(
        productControlConfigData -> {

          // extracting data from product configuration
          String partnerCode = productControlConfigData.getT1();
          ProductControl.Flow flowData =
              productConfigMasterService.getFlowFromProductConfig(
                  productControlConfigData.getT2(), flowIdentifier);

          // npe checks for product configuration data
          if (!Objects.isNull(flowData)) {
            // extracting parameters for driving the callback-cta flow for partners
            String functionName = flowData.getFunctionName();
            try {
              // trying reflection api for holding the method
              // based on the function name found in product configuration
              Method method = getMethod(requestBody, functionName);

              // invoking the method for callback-cta flow
              return (Mono<?>) method.invoke(this, requestBody, productCode, partnerCode, flowData);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
              return Mono.error(e);
            }
          } else {
            log.info(
                "{} identifier is not enabled for product-code: {}", flowIdentifier, productCode);
            return Mono.just(
                flowIdentifier + " identifier is not enabled for product-code: " + productCode);
          }
        });
  }

  private Mono<?> updateClientFromXml(String base64Xml, String leadId) {
    return extractXmlFromBase64(base64Xml)
        .flatMap(
            clientXmlDetailsDTO ->
                processClientXmlDetails(
                    clientXmlDetailsDTO, leadId, AddressType.AADHAAR.getDisplayName()));
  }

  private <T> Method getMethod(T requestBody, String functionName) throws NoSuchMethodException {
    Class<?> requestBodyClass = requestBody.getClass();
    if (requestBodyClass == LinkedHashMap.class) {
      requestBodyClass = Object.class;
    }
    return this.getClass()
        .getMethod(
            functionName, requestBodyClass, String.class, String.class, ProductControl.Flow.class);
  }

  // method trigger by Product config of kycValidation
  public Mono<?> registerAsyncKycValidation(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      String productCode,
      String partnerCode,
      ProductControl.Flow flowData) {

    if (aadhaarXmlValidationDTO.getAadhaarXMLType() == null) {
      log.info(
          "[KYC_QC] AadhaarXMLType is null, so setting it to {}, for loan Application Id {}",
          AadhaarXMLType.DIGI_LOCKER.getDisplayName(),
          aadhaarXmlValidationDTO.getLoanId());
      aadhaarXmlValidationDTO.setAadhaarXMLType(AadhaarXMLType.DIGI_LOCKER);
    }

    KycQcEntity kycQcEntity =
        KycQcEntity.builder()
            .clientId(aadhaarXmlValidationDTO.getLeadId())
            .loanId(aadhaarXmlValidationDTO.getLoanId())
            .xmlTs(
                aadhaarXmlValidationDTO.getClientXmlDetailsDTO() != null
                    ? aadhaarXmlValidationDTO.getClientXmlDetailsDTO().getTs()
                    : "")
            .kycType(aadhaarXmlValidationDTO.getAadhaarXMLType().name())
            .productCode(productCode)
            .build();

    log.info(
        "[KYC_QC] Starting async KYC validation for leadId: {}, productCode: {},"
            + " partnerCode: {}",
        aadhaarXmlValidationDTO.getLeadId(),
        productCode,
        partnerCode);

    // Validate KYC decoupling flag
    if (!isKycValidationDecoupled(flowData, aadhaarXmlValidationDTO.getLeadId(), productCode)) {
      return Mono.just("[KYC_QC] KYC Validation is not decoupled for product code: " + productCode);
    }

    log.info(
        "[KYC_QC] Checking if KYC validations already exist for loanId: {}, productCode: {}",
        aadhaarXmlValidationDTO.getLoanId(),
        productCode);

    return kycQcService
        .findByLoanIdWithFinalizedStatuses(aadhaarXmlValidationDTO.getLoanId())
        .flatMap(
            existingKycQc -> {
              log.info(
                  "[KYC_QC] KYC Validations already completed for loan Application Id: {},"
                      + " product code: {}, nameStatus: {}, faceStatus: {}",
                  aadhaarXmlValidationDTO.getLoanId(),
                  productCode,
                  existingKycQc.getFinalNameMatchStatus(),
                  existingKycQc.getFinalFaceMatchStatus());
              return Mono.<Object>just(
                  "[KYC_QC] KYC Validations already for loan Application Id "
                      + aadhaarXmlValidationDTO.getLoanId()
                      + ", product code: "
                      + productCode);
            })
        .switchIfEmpty(
            Mono.defer(
                () -> {
                  log.info(
                      "[KYC_QC] No existing finalized KYC record found for loanId: {},"
                          + " proceeding with validations",
                      aadhaarXmlValidationDTO.getLoanId());

                  // Validate required config fields
                  validateRequiredConfigFields(
                      flowData, aadhaarXmlValidationDTO.getLeadId(), productCode);

                  log.info(
                      "[KYC_QC] All required config fields validated successfully for leadId: {},"
                          + " productCode: {}",
                      aadhaarXmlValidationDTO.getLeadId(),
                      productCode);

                  if (AadhaarXMLType.DIGI_LOCKER.equals(
                      aadhaarXmlValidationDTO.getAadhaarXMLType())) {
                    Long xmlValidityMonths =
                        Long.parseLong(flowData.getConditions().get(XML_VALIDITY).toString());

                    return performXmlValidityCheckWithLogging(
                            aadhaarXmlValidationDTO, xmlValidityMonths, productCode, kycQcEntity)
                        .then(
                            Mono.defer(
                                () ->
                                    executeMatchValidations(
                                        aadhaarXmlValidationDTO,
                                        productCode,
                                        flowData,
                                        kycQcEntity,
                                        aadhaarXmlValidationDTO.getAadhaarXMLType())));
                  }

                  return executeMatchValidations(
                      aadhaarXmlValidationDTO,
                      productCode,
                      flowData,
                      kycQcEntity,
                      aadhaarXmlValidationDTO.getAadhaarXMLType());
                }));
  }

  public Mono<?> processClientXmlDetails(
      AadhaarXmlDetailsDTO aadhaarXmlDetailsDTO, String leadId, String addressType) {
    M2pAddressDetailsDTO addressDetails = buildAddressDetails(aadhaarXmlDetailsDTO, addressType);
    M2pLeadUpdateDTO addressUpdate = buildAddressUpdateDTO(addressDetails);

    return m2PWrapperApi
        .updateLead(addressUpdate, leadId)
        .doOnSuccess(r -> log.info(UPDATE_LEAD_VIA_XML, leadId))
        .onErrorResume(
            e -> {
              log.error(
                  "[UPDATE_LEAD_VIA_XML] failed to update aadhaar address for lead {}: {}",
                  leadId,
                  e.getMessage());
              return Mono.empty();
            })
        .then(familyDetailResolver.resolve(aadhaarXmlDetailsDTO, leadId));
  }

  public M2pAddressDetailsDTO buildAddressDetails(AadhaarXmlDetailsDTO dto, String addressType) {

    dto = sanitizeAddress(dto);

    String addressLineOne =
        Stream.of(
                dto.getCareOf(),
                dto.getHouse(),
                dto.getStreet(),
                dto.getLandmark(),
                dto.getLocality(),
                dto.getVtc(),
                dto.getSubdistrict(),
                dto.getDistrict(),
                dto.getState(),
                dto.getPincode())
            .filter(s -> Objects.nonNull(s) && !s.isBlank())
            .collect(Collectors.joining(","));

    String addressLineTwo = "";
    if (addressLineOne.length() > 200) {
      int splitIndex = addressLineOne.lastIndexOf(',', 200);
      if (splitIndex == -1) splitIndex = 200;
      addressLineTwo = addressLineOne.substring(splitIndex + 1).trim();
      addressLineOne = addressLineOne.substring(0, splitIndex).trim();
    }

    return M2pAddressDetailsDTO.builder()
        .addressType(List.of(String.valueOf(addressType)))
        .addressLineOne(addressLineOne)
        .addressLineTwo(addressLineTwo)
        .landmark(dto.getLandmark())
        .postalCode(dto.getPincode())
        .build();
  }

  private M2pLeadUpdateDTO buildAddressUpdateDTO(M2pAddressDetailsDTO addressDetails) {
    M2pLeadUpdateDTO.M2pLeadUpdateDTOBuilder m2pLeadUpdateDTOBuilder =
        M2pLeadUpdateDTO.builder()
            .addressData(List.of(addressDetails))
            .locale("en")
            .dateFormat(DATE_FORMAT);

    return m2pLeadUpdateDTOBuilder.build();
  }

  private String sanitize(String input) {
    if (input == null) return null;
    return input.replaceAll("[^A-Za-z0-9:,@+ _\\t\\r\\n\"().\\-{}\\[\\]/]", "");
  }

  // Sanitize DTO to remove special Characters not required for credit bureau
  private AadhaarXmlDetailsDTO sanitizeAddress(AadhaarXmlDetailsDTO dto) {
    return AadhaarXmlDetailsDTO.builder()
        .careOf(sanitize(dto.getCareOf()))
        .house(sanitize(dto.getHouse()))
        .street(sanitize(dto.getStreet()))
        .landmark(sanitize(dto.getLandmark()))
        .locality(sanitize(dto.getLocality()))
        .vtc(sanitize(dto.getVtc()))
        .subdistrict(sanitize(dto.getSubdistrict()))
        .district(sanitize(dto.getDistrict()))
        .state(sanitize(dto.getState()))
        .pincode(sanitize(dto.getPincode()))
        .build();
  }

  public Mono<AadhaarXmlDetailsDTO> getLatestAadhaarXml(String clientId) {
    return m2PWrapperApi
        .getLatestAadhaarXmlDocId(clientId)
        .switchIfEmpty(Mono.error(new NotFoundException(NO_AADHAAR_XML_FOUND)))
        .flatMap(
            documentList -> {
              log.info(
                  "[GET LATEST AADHAAR XML][SUCCESS] client latest aadhar xml doc id: {}, against"
                      + " client: {}",
                  documentList.getId(),
                  clientId);
              return m2PWrapperApi
                  .getLatestAadhaarXml(clientId, documentList.getId())
                  .switchIfEmpty(Mono.error(new NotFoundException(NO_AADHAAR_XML_FOUND)))
                  .flatMap(LeadDataUtil::parseXml);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[GET LATEST AADHAAR XML][ERROR] Failed fetching aadhaar xml for client: {}"
                      + " exception: {}",
                  clientId,
                  error.getMessage(),
                  error);
              return Mono.error(new NotFoundException(NO_AADHAAR_XML_FOUND));
            });
  }

  /**
   * Executes both name and face match validations.
   *
   * @param aadhaarXmlValidationDTO the validation DTO
   * @param productCode the product code
   * @param flowData the flow configuration
   * @return Mono with validation result
   */
  private Mono<?> executeMatchValidations(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      String productCode,
      ProductControl.Flow flowData,
      KycQcEntity kycQcEntity,
      AadhaarXMLType aadhaarXMLType) {

    // Extract config parameters
    KycValidationExecution nameMatchExecution =
        KycValidationExecution.valueOf(
            flowData.getConditions().get(NAME_MATCH_EXECUTION).toString());
    KycValidationVendors nameMatchPriority =
        KycValidationVendors.valueOf(flowData.getConditions().get(NAME_MATCH_PRIORITY).toString());
    KycValidationExecution faceMatchExecution =
        KycValidationExecution.valueOf(
            flowData.getConditions().get(FACE_MATCH_EXECUTION).toString());
    KycValidationVendors faceMatchPriority =
        KycValidationVendors.valueOf(flowData.getConditions().get(FACE_MATCH_PRIORITY).toString());
    Double nameMatchThreshold =
        Double.parseDouble(flowData.getConditions().get(NAME_MATCH_THRESHOLD).toString());
    Double faceMatchThreshold =
        Double.parseDouble(flowData.getConditions().get(FACE_MATCH_THRESHOLD).toString());
    boolean nameFallbackEnabled =
        Boolean.parseBoolean(flowData.getConditions().get(NAME_FALLBACK).toString());
    boolean faceFallbackEnabled =
        Boolean.parseBoolean(flowData.getConditions().get(FACE_FALLBACK).toString());

    log.info(
        "[KYC_QC] Thresholds - Name Match: {}, Face Match: {} for leadId: {}",
        nameMatchThreshold,
        faceMatchThreshold,
        aadhaarXmlValidationDTO.getLeadId());
    log.info(
        "[KYC_QC] Fallback config - nameFallback: {}, faceFallback: {} for leadId: {}",
        nameFallbackEnabled,
        faceFallbackEnabled,
        aadhaarXmlValidationDTO.getLeadId());
    log.info(
        "[KYC_QC] Name match execution mode: {} for leadId: {}, productCode: {}",
        nameMatchExecution,
        aadhaarXmlValidationDTO.getLeadId(),
        productCode);

    // Extract Aadhaar data
    String clientNameFromAadhaar = aadhaarXmlValidationDTO.getClientXmlDetailsDTO().getName();
    String clientPhotoFromAadhaar =
        aadhaarXmlValidationDTO.getClientXmlDetailsDTO().getPhotoBase64();

    log.info(
        "[KYC_QC] Extracted Aadhaar data - Name: {}, Photo present: {} for leadId:" + " {}",
        clientNameFromAadhaar,
        (clientPhotoFromAadhaar != null && !clientPhotoFromAadhaar.isEmpty()),
        aadhaarXmlValidationDTO.getLeadId());

    // Fetch client data from LOS and execute validations
    return fetchClientDataFromLos(aadhaarXmlValidationDTO, productCode)
        .flatMap(
            clientData ->
                executeNameAndFaceMatchValidations(
                    aadhaarXmlValidationDTO,
                    productCode,
                    (String) clientData[0],
                    (String) clientData[1],
                    clientNameFromAadhaar,
                    clientPhotoFromAadhaar,
                    nameMatchExecution,
                    nameMatchPriority,
                    nameMatchThreshold,
                    faceMatchExecution,
                    faceMatchPriority,
                    faceMatchThreshold,
                    nameFallbackEnabled,
                    faceFallbackEnabled,
                    kycQcEntity));
  }

  /**
   * Fetches client name and photo from LOS system.
   *
   * @param aadhaarXmlValidationDTO the validation DTO
   * @return Mono with array [clientNameFromLos, clientPhotoFromLos]
   */
  private Mono<Object[]> fetchClientDataFromLos(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO, String productCode) {
    log.info(
        "[KYC_QC] Fetching client image ID from M2P for leadId: {}",
        aadhaarXmlValidationDTO.getLeadId());

    // Fetch name from cache with DB Fallback
    Mono<String> clientName =
        loanLevelClientDetailsService
            .fetchLoanLevelClientDetails(
                aadhaarXmlValidationDTO.getLeadId(),
                aadhaarXmlValidationDTO.getLoanId(),
                productCode)
            .map(
                loanLevelClientDetailsCacheDTO ->
                    constructFullName(
                        loanLevelClientDetailsCacheDTO.getFirstName(),
                        loanLevelClientDetailsCacheDTO.getMiddleName(),
                        loanLevelClientDetailsCacheDTO.getLastName()))
            .doOnSuccess(
                response ->
                    log.info(
                        "[LOAN_LEVEL_CLIENT_DATA][KYC_QC] successfully fetched client name details"
                            + " for leadId: {}, loanApplicationId: {}",
                        aadhaarXmlValidationDTO.getLeadId(),
                        aadhaarXmlValidationDTO.getLoanId()))
            .doOnError(
                e ->
                    log.error(
                        "[ERROR][LOAN_LEVEL_CLIENT_DATA][KYC_QC] failed to fetch name details for"
                            + " leadId: {}, loanApplicationId: {}",
                        aadhaarXmlValidationDTO.getLeadId(),
                        aadhaarXmlValidationDTO.getLoanId(),
                        e));

    // Fetch Image ID from M2P
    Mono<ClientImageResponse> clientImageId =
        m2PWrapperApi
            .getClientImageId(aadhaarXmlValidationDTO.getLeadId())
            .doOnSuccess(
                response ->
                    log.info(
                        "[KYC_QC] Successfully fetched client image ID for leadId: {},"
                            + " imageId: {}",
                        aadhaarXmlValidationDTO.getLeadId(),
                        response != null ? response.getImageId() : null))
            .doOnError(
                e ->
                    log.error(
                        "[KYC_QC][ERROR] Failed to fetch image ID for leadId: {}",
                        aadhaarXmlValidationDTO.getLeadId(),
                        e));

    return Mono.zip(clientName, clientImageId)
        .flatMap(
            tuple -> {
              String clientNameFromLos = tuple.getT1();
              ClientImageResponse clientImageResponse = tuple.getT2();

              Long imageId =
                  (clientImageResponse != null) ? clientImageResponse.getImageId() : null;

              // Fetch client photo using image ID
              if (imageId != null) {
                log.info(
                    "[KYC_QC] Fetching client photo by imageId: {} for leadId: {}",
                    imageId,
                    aadhaarXmlValidationDTO.getLeadId());
                return m2PWrapperApi
                    .getClientImageByImageId(aadhaarXmlValidationDTO.getLeadId(), imageId)
                    .doOnSuccess(
                        photo ->
                            log.info(
                                "[KYC_QC] Successfully fetched client photo for"
                                    + " leadId: {}, photo length: {}",
                                aadhaarXmlValidationDTO.getLeadId(),
                                photo != null ? photo.length() : 0))
                    .doOnError(
                        error ->
                            log.error(
                                "[KYC_QC][ERROR] Failed to fetch client photo for"
                                    + " leadId: {}, imageId: {}, error: {}",
                                aadhaarXmlValidationDTO.getLeadId(),
                                imageId,
                                error.getMessage()))
                    .map(
                        clientPhotoFromLos ->
                            new Object[] {
                              clientNameFromLos, stripImageDataPrefix(clientPhotoFromLos)
                            });
              } else {
                log.error(
                    "[KYC_QC] No imageId found for leadId: {}, client photo will be" + " null",
                    aadhaarXmlValidationDTO.getLeadId());
                return Mono.just(new Object[] {clientNameFromLos, null});
              }
            })
        .defaultIfEmpty(new Object[] {null, null})
        .doOnNext(
            data -> {
              if (data[0] == null && data[1] == null) {
                log.error(
                    "[KYC_QC] No client image data found (defaultIfEmpty triggered)"
                        + " for leadId: {}",
                    aadhaarXmlValidationDTO.getLeadId());
              }
            });
  }

  /**
   * Executes name and face match validations in sequence.
   *
   * @param aadhaarXmlValidationDTO the validation DTO
   * @param productCode the product code
   * @param clientNameFromLos name from LOS
   * @param clientPhotoFromLos photo from LOS
   * @param clientNameFromAadhaar name from Aadhaar
   * @param clientPhotoFromAadhaar photo from Aadhaar
   * @param nameMatchExecution name match execution mode
   * @param nameMatchPriority name match priority vendor
   * @param nameMatchThreshold name match threshold
   * @param faceMatchExecution face match execution mode
   * @param faceMatchPriority face match priority vendor
   * @param faceMatchThreshold face match threshold
   * @param nameFallbackEnabled whether name match fallback is enabled in sequential mode
   * @param faceFallbackEnabled whether face match fallback is enabled in sequential mode
   * @return Mono with validation result
   */
  private Mono<?> executeNameAndFaceMatchValidations(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      String productCode,
      String clientNameFromLos,
      String clientPhotoFromLos,
      String clientNameFromAadhaar,
      String clientPhotoFromAadhaar,
      KycValidationExecution nameMatchExecution,
      KycValidationVendors nameMatchPriority,
      Double nameMatchThreshold,
      KycValidationExecution faceMatchExecution,
      KycValidationVendors faceMatchPriority,
      Double faceMatchThreshold,
      boolean nameFallbackEnabled,
      boolean faceFallbackEnabled,
      KycQcEntity kycQcEntity) {

    log.info(
        "[KYC_QC] STARTING Name match execution section for leadId: {}, productCode:" + " {}",
        aadhaarXmlValidationDTO.getLeadId(),
        productCode);

    // Execute face match after name match
    return executeNameMatch(
            aadhaarXmlValidationDTO,
            productCode,
            clientNameFromLos,
            clientNameFromAadhaar,
            nameMatchExecution,
            nameMatchPriority,
            nameMatchThreshold,
            nameFallbackEnabled)
        .flatMap(
            nameScores -> {

              // set NameMatch details in kycQc (rounded to 2 decimal places)
              kycQcEntity.setKarzaNameMatchScore(
                  roundToTwoDecimals(nameScores.getNameMatchScoreFromKarza()));
              kycQcEntity.setAnalyticNameMatchScore(
                  roundToTwoDecimals(nameScores.getNameMatchScoreFromTrillion()));
              kycQcEntity.setFinalNameMatchScore(
                  roundToTwoDecimals(nameScores.getFinalNameMatchScore()));
              kycQcEntity.setFinalNameMatchStatus(nameScores.getFinalNameMatchStatus());
              Event nameMatchEvent = null;
              switch (nameScores.getFinalNameMatchStatus()) {
                case "VERIFIED" -> nameMatchEvent = Event.KYC_QC_NAME_MATCH_VERIFIED;
                case "REJECTED" -> nameMatchEvent = Event.KYC_QC_NAME_MATCH_REJECTED;
                case "CAN_NOT_BE_DONE" -> nameMatchEvent = Event.KYC_QC_NAME_MATCH_CANNOT_BE_DONE;
                default ->
                    log.error(
                        "[KYC_QC] Unknown name match status: {} for loanId: {}, leadId: {}",
                        nameScores.getFinalNameMatchStatus(),
                        aadhaarXmlValidationDTO.getLoanId(),
                        aadhaarXmlValidationDTO.getLeadId());
              }
              if (nameMatchEvent != null) {
                Event finalNameMatchEvent = nameMatchEvent;
                publishEventKafkaAsync(
                    () ->
                        eventProducerService.publishEvent(
                            new EventContext(
                                finalNameMatchEvent,
                                aadhaarXmlValidationDTO.getLoanId(),
                                aadhaarXmlValidationDTO.getLeadId()),
                            null,
                            null));
              }

              log.info(
                  "[KYC_QC] STARTING Face match execution section for loanId: {},"
                      + " leadId: {}, productCode: {}",
                  aadhaarXmlValidationDTO.getLoanId(),
                  aadhaarXmlValidationDTO.getLeadId(),
                  productCode);

              return executeFaceMatch(
                      aadhaarXmlValidationDTO,
                      productCode,
                      clientPhotoFromLos,
                      clientPhotoFromAadhaar,
                      faceMatchExecution,
                      faceMatchPriority,
                      faceMatchThreshold,
                      faceFallbackEnabled)
                  .flatMap(
                      faceScores -> {
                        // set faceMatch details in kycQc (rounded to 2 decimal places)
                        kycQcEntity.setKarzaFaceMatchScore(
                            roundToTwoDecimals(faceScores.getFaceMatchScoreFromKarza()));
                        kycQcEntity.setAnalyticFaceMatchScore(
                            roundToTwoDecimals(faceScores.getFaceMatchScoreFromTrillion()));
                        kycQcEntity.setFinalFaceMatchScore(
                            roundToTwoDecimals(faceScores.getFinalFaceMatchScore()));
                        kycQcEntity.setFinalFaceMatchStatus(faceScores.getFinalFaceMatchStatus());
                        Event faceMatchEvent = null;
                        switch (faceScores.getFinalFaceMatchStatus()) {
                          case "VERIFIED" -> faceMatchEvent = Event.KYC_QC_FACE_MATCH_VERIFIED;
                          case "REJECTED" -> faceMatchEvent = Event.KYC_QC_FACE_MATCH_REJECTED;
                          case "CAN_NOT_BE_DONE" ->
                              faceMatchEvent = Event.KYC_QC_FACE_MATCH_CANNOT_BE_DONE;
                          default ->
                              log.error(
                                  "[KYC_QC] Unknown face match status: {} for loanId: {}, leadId:"
                                      + " {}",
                                  faceScores.getFinalFaceMatchStatus(),
                                  aadhaarXmlValidationDTO.getLoanId(),
                                  aadhaarXmlValidationDTO.getLeadId());
                        }
                        if (faceMatchEvent != null) {
                          Event finalFaceMatchEvent = faceMatchEvent;
                          publishEventKafkaAsync(
                              () ->
                                  eventProducerService.publishEvent(
                                      new EventContext(
                                          finalFaceMatchEvent,
                                          aadhaarXmlValidationDTO.getLoanId(),
                                          aadhaarXmlValidationDTO.getLeadId()),
                                      null,
                                      null));
                        }

                        log.info(
                            "[KYC_QC] Completed async KYC validation processing for"
                                + " leadId: {}, productCode: {}",
                            aadhaarXmlValidationDTO.getLeadId(),
                            productCode);
                        log.info(
                            "[KYC_QC] Summary - Name Match Scores [Karza: {},"
                                + " Trillion: {}, Final: {}], Face Match Scores [Karza: {},"
                                + " Trillion: {}, Final: {}] for leadId: {}",
                            nameScores.getNameMatchScoreFromKarza(),
                            nameScores.getNameMatchScoreFromTrillion(),
                            nameScores.getFinalNameMatchScore(),
                            faceScores.getFaceMatchScoreFromKarza(),
                            faceScores.getFaceMatchScoreFromTrillion(),
                            faceScores.getFinalFaceMatchScore(),
                            aadhaarXmlValidationDTO.getLeadId());

                        // set non-functional details in kycQc
                        kycQcEntity.setCreatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
                        kycQcEntity.setUpdatedAt(LocalDateTime.now(ZoneId.of(ASIA_KOLKATA)));
                        kycQcEntity.setIsDeleted(false);

                        String ctaName =
                            AadhaarXMLType.OKYC.equals(aadhaarXmlValidationDTO.getAadhaarXMLType())
                                ? "okyc-xml-validation"
                                : "xml-validation";

                        log.info(
                            "[KYC_QC] Hitting CTA: {} for loanId: {}, aadhaarXMLType: {}",
                            ctaName,
                            kycQcEntity.getLoanId(),
                            aadhaarXmlValidationDTO.getAadhaarXMLType());

                        return m2PWrapperApi
                            .registerCta(kycQcEntity.getLoanId(), ctaName)
                            .flatMap(xmlValidationCtaResponse -> kycQcService.save(kycQcEntity))
                            .onErrorResume(
                                e -> {
                                  log.error(
                                      "[KYC_QC] Error hitting {} CTA for loanId: {}",
                                      ctaName,
                                      kycQcEntity.getLoanId());
                                  log.info(
                                      "[KYC_QC] Storing xml validation details in database in case"
                                          + " of failed CTA: {}",
                                      ctaName);
                                  return kycQcService.save(kycQcEntity);
                                });
                      });
            });
  }

  /** Executes name match validation based on execution mode. */
  private Mono<NameMatchScores> executeNameMatch(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      String productCode,
      String clientNameFromLos,
      String clientNameFromAadhaar,
      KycValidationExecution nameMatchExecution,
      KycValidationVendors nameMatchPriority,
      Double nameMatchThreshold,
      boolean nameFallbackEnabled) {

    if (KycValidationExecution.PARALLEL.equals(nameMatchExecution)) {
      log.info(
          "[KYC_QC] Name match execution mode is PARALLEL for leadId: {}," + " productCode: {}",
          aadhaarXmlValidationDTO.getLeadId(),
          productCode);
      return executeParallelNameMatch(
          aadhaarXmlValidationDTO,
          clientNameFromLos,
          clientNameFromAadhaar,
          nameMatchPriority,
          nameMatchThreshold,
          nameFallbackEnabled);
    } else if (KycValidationExecution.SEQUENCE.equals(nameMatchExecution)) {
      log.info(
          "[KYC_QC] Name match execution mode is SEQUENCE for loanId: {}, leadId: {},"
              + " productCode: {}",
          aadhaarXmlValidationDTO.getLoanId(),
          aadhaarXmlValidationDTO.getLeadId(),
          productCode);
      return executeSequenceNameMatch(
          aadhaarXmlValidationDTO,
          clientNameFromLos,
          clientNameFromAadhaar,
          nameMatchPriority,
          nameMatchThreshold,
          nameFallbackEnabled);
    } else {
      log.error(
          "[KYC_QC] Unknown name match execution mode: {} for leadId: {}",
          nameMatchExecution,
          aadhaarXmlValidationDTO.getLeadId());
      return Mono.just(new NameMatchScores());
    }
  }

  /** Executes parallel name match validation. */
  private Mono<NameMatchScores> executeParallelNameMatch(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      String clientNameFromLos,
      String clientNameFromAadhaar,
      KycValidationVendors nameMatchPriority,
      Double nameMatchThreshold,
      boolean nameFallbackEnabled) {

    return aadhaarXmlValidationsService
        .parallelNameMatchExecution(
            clientNameFromLos,
            clientNameFromAadhaar,
            aadhaarXmlValidationDTO.getLeadId(),
            aadhaarXmlValidationDTO.getLoanId())
        .map(
            nameMatchResult ->
                extractNameMatchScores(
                    nameMatchResult,
                    nameMatchPriority,
                    aadhaarXmlValidationDTO,
                    nameFallbackEnabled))
        .flatMap(
            scores ->
                updateNameMatchScoreAndReturn(
                    scores,
                    aadhaarXmlValidationDTO,
                    nameMatchThreshold,
                    clientNameFromAadhaar,
                    clientNameFromLos))
        .doOnError(
            error ->
                log.error(
                    "[KYC_QC][ERROR] Name match execution failed for leadId: {}," + " error: {}",
                    aadhaarXmlValidationDTO.getLeadId(),
                    error.getMessage()))
        .onErrorReturn(new NameMatchScores());
  }

  /** Executes sequence name match validation. */
  private Mono<NameMatchScores> executeSequenceNameMatch(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      String clientNameFromLos,
      String clientNameFromAadhaar,
      KycValidationVendors nameMatchPriority,
      Double nameMatchThreshold,
      boolean nameFallbackEnabled) {

    return aadhaarXmlValidationsService
        .sequenceNameMatchExecution(
            nameMatchPriority,
            clientNameFromLos,
            clientNameFromAadhaar,
            nameFallbackEnabled,
            aadhaarXmlValidationDTO.getLeadId(),
            aadhaarXmlValidationDTO.getLoanId())
        .map(
            nameMatchResult ->
                extractNameMatchScores(
                    nameMatchResult,
                    nameMatchPriority,
                    aadhaarXmlValidationDTO,
                    nameFallbackEnabled))
        .flatMap(
            scores ->
                updateNameMatchScoreAndReturn(
                    scores,
                    aadhaarXmlValidationDTO,
                    nameMatchThreshold,
                    clientNameFromAadhaar,
                    clientNameFromLos))
        .doOnError(
            error ->
                log.error(
                    "[KYC_QC][ERROR] Name match execution (SEQUENCE) failed for"
                        + " leadId: {}, error: {}",
                    aadhaarXmlValidationDTO.getLeadId(),
                    error.getMessage()))
        .onErrorReturn(new NameMatchScores());
  }

  /** Executes parallel face match validation. */
  private Mono<FaceMatchScores> executeParallelFaceMatch(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      String clientPhotoFromLos,
      String clientPhotoFromAadhaar,
      KycValidationVendors faceMatchPriority,
      Double faceMatchThreshold,
      boolean faceFallbackEnabled) {

    return aadhaarXmlValidationsService
        .parallelFaceMatchExecution(
            clientPhotoFromLos,
            clientPhotoFromAadhaar,
            aadhaarXmlValidationDTO.getLeadId(),
            aadhaarXmlValidationDTO.getLoanId())
        .map(
            faceMatchResult ->
                extractFaceMatchScores(
                    faceMatchResult,
                    faceMatchPriority,
                    aadhaarXmlValidationDTO,
                    faceFallbackEnabled))
        .flatMap(
            scores ->
                updateFaceMatchScoreAndReturn(scores, aadhaarXmlValidationDTO, faceMatchThreshold))
        .doOnError(
            error ->
                log.error(
                    "[KYC_QC][ERROR] Face match execution failed for leadId: {}," + " error: {}",
                    aadhaarXmlValidationDTO.getLeadId(),
                    error.getMessage()))
        .onErrorReturn(new FaceMatchScores());
  }

  /** Executes sequence face match validation. */
  private Mono<FaceMatchScores> executeSequenceFaceMatch(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      String clientPhotoFromLos,
      String clientPhotoFromAadhaar,
      KycValidationVendors faceMatchPriority,
      Double faceMatchThreshold,
      boolean faceFallbackEnabled) {

    return aadhaarXmlValidationsService
        .sequenceFaceMatchExecution(
            faceMatchPriority,
            clientPhotoFromLos,
            clientPhotoFromAadhaar,
            faceFallbackEnabled,
            aadhaarXmlValidationDTO.getLeadId(),
            aadhaarXmlValidationDTO.getLoanId())
        .map(
            faceMatchResult ->
                extractFaceMatchScores(
                    faceMatchResult,
                    faceMatchPriority,
                    aadhaarXmlValidationDTO,
                    faceFallbackEnabled))
        .flatMap(
            scores ->
                updateFaceMatchScoreAndReturn(scores, aadhaarXmlValidationDTO, faceMatchThreshold))
        .doOnError(
            error ->
                log.error(
                    "[KYC_QC][ERROR] Face match execution (SEQUENCE) failed for"
                        + " leadId: {}, error: {}",
                    aadhaarXmlValidationDTO.getLeadId(),
                    error.getMessage()))
        .onErrorReturn(new FaceMatchScores());
  }

  /** Extracts name match scores from result and calculates final score. */
  private NameMatchScores extractNameMatchScores(
      com.trillionloans.los.model.dto.MatchingScoreDTO nameMatchResult,
      KycValidationVendors nameMatchPriority,
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      boolean nameFallbackEnabled) {

    log.info(
        "[KYC_QC] Name match result received for leadId: {}", aadhaarXmlValidationDTO.getLeadId());

    NameMatchScores scores = new NameMatchScores();

    if (nameMatchResult != null && nameMatchResult.getMatchingScores() != null) {
      // Extract Karza score
      if (nameMatchResult.getMatchingScores().containsKey(KycValidationVendors.KARZA)) {
        scores.setNameMatchScoreFromKarza(
            nameMatchResult.getMatchingScores().get(KycValidationVendors.KARZA).getScore());
        log.info(
            "[KYC_QC] Name match score from KARZA: {} for leadId: {}",
            scores.getNameMatchScoreFromKarza(),
            aadhaarXmlValidationDTO.getLeadId());
      }

      // Extract Trillion score
      if (nameMatchResult.getMatchingScores().containsKey(KycValidationVendors.TRILLION)) {
        scores.setNameMatchScoreFromTrillion(
            nameMatchResult.getMatchingScores().get(KycValidationVendors.TRILLION).getScore());
        log.info(
            "[KYC_QC] Name match score from TRILLION: {} for leadId: {}",
            scores.getNameMatchScoreFromTrillion(),
            aadhaarXmlValidationDTO.getLeadId());
      }

      // Calculate final score based on priority with fallback
      scores.setFinalNameMatchScore(
          calculateFinalScore(
              scores.getNameMatchScoreFromKarza(),
              scores.getNameMatchScoreFromTrillion(),
              nameMatchPriority,
              nameFallbackEnabled));

      log.info(
          "[KYC_QC] Final name match score: {} (priority: {}, fallbackEnabled: {}) for leadId: {}",
          scores.getFinalNameMatchScore(),
          nameMatchPriority,
          nameFallbackEnabled,
          aadhaarXmlValidationDTO.getLeadId());
    }

    return scores;
  }

  /** Extracts face match scores from result and calculates final score. */
  private FaceMatchScores extractFaceMatchScores(
      com.trillionloans.los.model.dto.MatchingScoreDTO faceMatchResult,
      KycValidationVendors faceMatchPriority,
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      boolean faceFallbackEnabled) {

    log.info(
        "[KYC_QC] Face match result received for leadId: {}", aadhaarXmlValidationDTO.getLeadId());

    FaceMatchScores scores = new FaceMatchScores();

    if (faceMatchResult != null && faceMatchResult.getMatchingScores() != null) {
      // Extract Karza score
      if (faceMatchResult.getMatchingScores().containsKey(KycValidationVendors.KARZA)) {
        scores.setFaceMatchScoreFromKarza(
            faceMatchResult.getMatchingScores().get(KycValidationVendors.KARZA).getScore());
        log.info(
            "[KYC_QC] Face match score from KARZA: {} for leadId: {}",
            scores.getFaceMatchScoreFromKarza(),
            aadhaarXmlValidationDTO.getLeadId());
      }

      // Extract Trillion score
      if (faceMatchResult.getMatchingScores().containsKey(KycValidationVendors.TRILLION)) {
        scores.setFaceMatchScoreFromTrillion(
            faceMatchResult.getMatchingScores().get(KycValidationVendors.TRILLION).getScore());
        log.info(
            "[KYC_QC] Face match score from TRILLION: {} for leadId: {}",
            scores.getFaceMatchScoreFromTrillion(),
            aadhaarXmlValidationDTO.getLeadId());
      }

      // Calculate final score based on priority with fallback
      scores.setFinalFaceMatchScore(
          calculateFinalScore(
              scores.getFaceMatchScoreFromKarza(),
              scores.getFaceMatchScoreFromTrillion(),
              faceMatchPriority,
              faceFallbackEnabled));

      log.info(
          "[KYC_QC] Final face match score: {} (priority: {}, fallbackEnabled: {}) for leadId: {}",
          scores.getFinalFaceMatchScore(),
          faceMatchPriority,
          faceFallbackEnabled,
          aadhaarXmlValidationDTO.getLeadId());
    }

    return scores;
  }

  /** Updates name match score in M2P and returns the scores object. */
  private Mono<NameMatchScores> updateNameMatchScoreAndReturn(
      NameMatchScores scores,
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      Double nameMatchThreshold,
      String clientNameFromAadhaar,
      String clientNameFromLos) {

    String nameCheckName =
        AadhaarXMLType.OKYC.equals(aadhaarXmlValidationDTO.getAadhaarXMLType())
            ? "aadhaar-okyc-name-match"
            : "aadhaar-xml-name-match";

    log.info(
        "[KYC_QC] Using nameCheckName: {} for loanId: {}, aadhaarXMLType: {}",
        nameCheckName,
        aadhaarXmlValidationDTO.getLoanId(),
        aadhaarXmlValidationDTO.getAadhaarXMLType());

    return updateMatchScoreInM2P(
            scores.getFinalNameMatchScore(),
            nameMatchThreshold,
            nameCheckName,
            clientNameFromAadhaar,
            clientNameFromLos,
            aadhaarXmlValidationDTO.getLoanId())
        .flatMap(
            status -> {
              scores.setFinalNameMatchStatus(status);
              return Mono.just(scores);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[KYC_QC] Failed to update name match score in M2P, continuing with"
                      + " validation. Error: {}",
                  error.getMessage());
              return updateMatchScoreInM2P(
                      null,
                      nameMatchThreshold,
                      nameCheckName,
                      clientNameFromAadhaar,
                      clientNameFromLos,
                      aadhaarXmlValidationDTO.getLoanId())
                  .thenReturn(scores)
                  .onErrorResume(e -> Mono.just(scores));
            });
  }

  /** Updates face match score in M2P and returns the scores object. */
  private Mono<FaceMatchScores> updateFaceMatchScoreAndReturn(
      FaceMatchScores scores,
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      Double faceMatchThreshold) {

    String faceCheckName =
        AadhaarXMLType.OKYC.equals(aadhaarXmlValidationDTO.getAadhaarXMLType())
            ? "aadhaar-okyc-face-match"
            : "aadhaar-xml-face-match";

    log.info(
        "[KYC_QC] Using faceCheckName: {} for loanId: {}, aadhaarXMLType: {}",
        faceCheckName,
        aadhaarXmlValidationDTO.getLoanId(),
        aadhaarXmlValidationDTO.getAadhaarXMLType());

    return updateMatchScoreInM2P(
            scores.getFinalFaceMatchScore(),
            faceMatchThreshold,
            faceCheckName,
            null,
            null,
            aadhaarXmlValidationDTO.getLoanId())
        .flatMap(
            status -> {
              scores.setFinalFaceMatchStatus(status);
              return Mono.just(scores);
            })
        .onErrorResume(
            error -> {
              log.error(
                  "[KYC_QC] Failed to update face match score in M2P, continuing with"
                      + " validation. Error: {}",
                  error.getMessage());
              return updateMatchScoreInM2P(
                      null,
                      faceMatchThreshold,
                      faceCheckName,
                      null,
                      null,
                      aadhaarXmlValidationDTO.getLoanId())
                  .thenReturn(scores)
                  .onErrorResume(e -> Mono.just(scores));
            });
  }

  /**
   * Performs XML validity check based on TS (timestamp) from Aadhaar XML. Validates if the XML is
   * still valid by checking TS against current date + configured validity months.
   *
   * @param clientXmlDetailsDTO the client XML details containing TS (timestamp)
   * @param xmlValidityMonths the configured validity period in months
   * @param loanId the loan application ID
   * @return Mono<Void> that completes when M2P update is done
   */
  private Mono<Void> performXmlValidityCheck(
      AadhaarXmlDetailsDTO clientXmlDetailsDTO,
      Long xmlValidityMonths,
      String loanId,
      KycQcEntity kycQcEntity) {
    String ts = clientXmlDetailsDTO.getTs();
    String status;
    String dataJson;

    try {
      if (ts == null || ts.isEmpty()) {
        // TS is null - cannot determine validity
        status = "CAN_NOT_BE_DONE";
        dataJson = "";
        log.error("[KYC_QC] Ts is null for loanId: {}, status: CAN_NOT_DONE", loanId);
      } else {
        // Parse TS date with timezone offset (e.g., "2025-12-26T01:21:30.419+05:30")
        LocalDateTime tsDate;
        try {
          // Use ISO_DATE_TIME format to handle timezone offset
          tsDate = LocalDateTime.parse(ts, DateTimeFormatter.ISO_DATE_TIME);
          log.info("[KYC_QC] Parsed TS date: {} for loanId: {}", tsDate, loanId);
        } catch (Exception e) {
          log.error(
              "[KYC_QC] Failed to parse TS: {} for loanId: {}, error: {}",
              ts,
              loanId,
              e.getMessage());
          status = "CAN_NOT_BE_DONE";
          dataJson = "";
          return updateXmlValidityInM2P(status, dataJson, loanId);
        }

        String tsDateOnly = tsDate.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Current date in IST
        LocalDateTime currentDate = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        // Normalize order to always calculate forward
        LocalDateTime start = tsDate.isBefore(currentDate) ? tsDate : currentDate;
        LocalDateTime end = tsDate.isBefore(currentDate) ? currentDate : tsDate;

        // Calculate calendar difference
        Period period = Period.between(start.toLocalDate(), end.toLocalDate());

        int monthsDifference = period.getYears() * 12 + period.getMonths();
        int daysDifference = period.getDays();

        log.info(
            "[KYC_QC] Current date: {}, TS date: {}, Months diff: {}, Days diff: {} for loanId: {}",
            currentDate,
            tsDate,
            monthsDifference,
            daysDifference,
            loanId);

        // Check validity (days-aware)
        boolean isValid =
            monthsDifference < xmlValidityMonths
                || (monthsDifference == xmlValidityMonths && daysDifference == 0);

        if (isValid) {
          status = "VERIFIED";
          dataJson =
              objectMapper.writeValueAsString(Map.of("Initiation Date as per XML", tsDateOnly));
          log.info(
              "[KYC_QC] XML is VERIFIED for loanId: {}, TS: {}, Months diff: {}, Days diff: {}",
              loanId,
              ts,
              monthsDifference,
              daysDifference);
        } else {
          status = "REJECTED";
          dataJson =
              objectMapper.writeValueAsString(Map.of("Initiation Date as per XML", tsDateOnly));
          log.info(
              "[KYC_QC] XML is REJECTED for loanId: {}, TS: {}, Months diff: {}, Days diff: {}",
              loanId,
              ts,
              monthsDifference,
              daysDifference);
        }
      }

      // set xml validity details
      kycQcEntity.setXmlValidityStatus(status);
      Event xmlValidityEvent =
          switch (status) {
            case "VERIFIED" -> Event.XML_VALIDITY_VERIFIED;
            case "REJECTED" -> Event.XML_VALIDITY_REJECTED;
            case "CANNOT_BE_DONE" -> Event.XML_VALIDITY_CANNOT_BE_DONE;
            default -> Event.valueOf("UNKNOWN_STATUS");
          };
      publishEventKafkaAsync(
          () ->
              eventProducerService.publishEvent(
                  new EventContext(xmlValidityEvent, loanId, kycQcEntity.getClientId()),
                  null,
                  null));
      return updateXmlValidityInM2P(status, dataJson, loanId);
    } catch (JsonProcessingException e) {
      log.error(
          "[KYC_QC] Failed to serialize TS data for loanId: {}, error: {}", loanId, e.getMessage());
      return Mono.error(e);
    }
  }

  /**
   * Updates XML validity status in M2P qc_checks_loan_application datatable.
   *
   * @param status the validation status (VERIFIED, REJECTED, or CAN_NOT_BE_DONE)
   * @param dataJson the JSON data containing TTL information
   * @param loanId the loan application ID
   * @return Mono<Void> that completes when update is done
   */
  private Mono<Void> updateXmlValidityInM2P(String status, String dataJson, String loanId) {
    QcChecksDataTableDTO xmlValidityDTO =
        QcChecksDataTableDTO.builder()
            .checkName("aadhaar-xml-validity")
            .status(status)
            .data(dataJson)
            .locale("en")
            .dateFormat("dd MMMM yyyy")
            .build();

    log.info(
        "[KYC_QC] Updating XML validity in M2P for loanId: {}, status: {}, data: {}",
        loanId,
        status,
        dataJson);

    return m2PWrapperApi
        .updateQcChecksDataTable(xmlValidityDTO, loanId)
        .doOnSuccess(
            r -> {
              log.info(
                  "[KYC_QC] Successfully updated XML validity in M2P for loanId: {},"
                      + " status: {}",
                  loanId,
                  status);
            })
        .doOnError(
            error ->
                log.error(
                    "[KYC_QC][ERROR] Failed to update XML validity in M2P for loanId:"
                        + " {}, error: {}",
                    loanId,
                    error.getMessage()))
        .then();
  }

  /**
   * Constructs full name from firstname, middlename, and lastname
   *
   * @param firstname the first name
   * @param middlename the middle name
   * @param lastname the last name
   * @return the full name constructed by concatenating the non-empty name parts with spaces
   */
  private String constructFullName(String firstname, String middlename, String lastname) {
    StringBuilder nameBuilder = new StringBuilder();
    if (firstname != null && !firstname.isEmpty()) {
      nameBuilder.append(firstname);
    }
    if (middlename != null && !middlename.isEmpty()) {
      if (!nameBuilder.isEmpty()) nameBuilder.append(" ");
      nameBuilder.append(middlename);
    }
    if (lastname != null && !lastname.isEmpty()) {
      if (!nameBuilder.isEmpty()) nameBuilder.append(" ");
      nameBuilder.append(lastname);
    }
    return nameBuilder.toString();
  }

  /**
   * Strips the data URI prefix from base64 encoded image data. Removes prefixes like
   * "data:image/jpeg;base64," or "data:image/png;base64," etc.
   *
   * @param imageData the image data with potential prefix
   * @return the image data without the prefix, or null if input is null
   */
  private String stripImageDataPrefix(String imageData) {
    if (imageData == null || imageData.isEmpty()) {
      return imageData;
    }

    // Check if the image data contains the data URI prefix
    int commaIndex = imageData.indexOf(',');
    if (commaIndex > 0 && imageData.substring(0, commaIndex).contains("base64")) {
      log.debug("[KYC_QC] Stripping image data prefix, original length: {}", imageData.length());
      String strippedData = imageData.substring(commaIndex + 1);
      log.debug("[KYC_QC] Stripped image data prefix, new length: {}", strippedData.length());
      return strippedData;
    }

    // Return as-is if no prefix found
    return imageData;
  }

  /**
   * Calculates the final score based on priority with fallback logic. If the priority vendor's
   * score is null, falls back to the other vendor's score only if fallback is enabled.
   *
   * @param karzaScore the score from Karza vendor
   * @param trillionScore the score from Trillion vendor
   * @param priority the priority vendor
   * @param fallbackEnabled whether fallback to the other vendor is enabled when priority score is
   *     null
   * @return the final score based on priority with fallback, or null if both are null or fallback
   *     is disabled
   */
  private Double calculateFinalScore(
      Double karzaScore,
      Double trillionScore,
      KycValidationVendors priority,
      boolean fallbackEnabled) {
    if (KycValidationVendors.KARZA.equals(priority)) {
      // Priority is Karza, use Karza score if available
      if (karzaScore != null) {
        return karzaScore;
      } else {
        // Check if fallback is enabled before falling back to Trillion
        if (!fallbackEnabled) {
          log.info(
              "[KYC_QC] Karza score is null and fallback is disabled."
                  + " Marking as CAN_NOT_BE_DONE (returning null).");
          return null;
        }
        log.info(
            "[KYC_QC] Karza score is null, fallback is enabled."
                + " Falling back to Trillion score: {}",
            trillionScore);
        return trillionScore;
      }
    } else if (KycValidationVendors.TRILLION.equals(priority)) {
      // Priority is Trillion, use Trillion score if available
      if (trillionScore != null) {
        return trillionScore;
      } else {
        // Check if fallback is enabled before falling back to Karza
        if (!fallbackEnabled) {
          log.info(
              "[KYC_QC] Trillion score is null and fallback is disabled."
                  + " Marking as CAN_NOT_BE_DONE (returning null).");
          return null;
        }
        log.info(
            "[KYC_QC] Trillion score is null, fallback is enabled."
                + " Falling back to Karza score: {}",
            karzaScore);
        return karzaScore;
      }
    } else {
      // Unknown priority, try Karza first, then Trillion
      log.error(
          "[KYC_QC] Unknown priority vendor: {}, defaulting to Karza then Trillion", priority);
      return karzaScore != null ? karzaScore : trillionScore;
    }
  }

  /**
   * Rounds a Double value to 2 decimal places using HALF_UP rounding. Returns null if input is
   * null.
   *
   * @param value the value to round
   * @return the rounded value, or null if input is null
   */
  private Double roundToTwoDecimals(Double value) {
    if (value == null) {
      return null;
    }
    return Math.round(value * 100.0) / 100.0;
  }

  /**
   * Updates match score (name or face) in M2P qc_checks_loan_application datatable. Determines
   * status based on score and threshold comparison.
   *
   * @param matchScore the final match score (decimal 0-1), null for CAN_NOT_BE_DONE
   * @param threshold the threshold for VERIFIED/REJECTED determination (decimal 0-1)
   * @param checkName the check name (e.g. "aadhaar-xml-name-match", "aadhaar-okyc-name-match",
   *     "aadhaar-xml-face-match", or "aadhaar-okyc-face-match")
   * @param nameFromAadhaar the name as per Aadhaar (only for name match, null for face match)
   * @param nameFromLos the name from LOS (only for name match, null for face match)
   * @param loanId the loan application ID
   * @return Mono<String> return status of the match
   */
  private Mono<String> updateMatchScoreInM2P(
      Double matchScore,
      Double threshold,
      String checkName,
      String nameFromAadhaar,
      String nameFromLos,
      String loanId) {
    String status;
    String dataJson = "";
    String scoreString = "";
    boolean isNameMatch =
        "aadhaar-xml-name-match".equals(checkName) || "aadhaar-okyc-name-match".equals(checkName);
    boolean isFaceMatch =
        "aadhaar-xml-face-match".equals(checkName) || "aadhaar-okyc-face-match".equals(checkName);

    try {
      if (matchScore == null) {
        // Score is null - cannot determine validity
        status = "CAN_NOT_BE_DONE";
        log.info(
            "[KYC_QC][UPDATE_MATCH_SCORE] Match score is null for loanId: {}, checkName: {},"
                + " status: CAN_NOT_BE_DONE",
            loanId,
            checkName);
      } else {
        // Convert score from decimal (0-1) to percentage (0-100) and round to 2 decimal places
        double scorePercentage = Math.round(matchScore * 100.0 * 100.0) / 100.0;
        scoreString = String.format("%.2f", scorePercentage);

        // Determine status based on threshold
        if (matchScore >= threshold) {
          status = "VERIFIED";
          log.info(
              "[KYC_QC][UPDATE_MATCH_SCORE] Match score {} >= threshold {}, status: VERIFIED for"
                  + " loanId: {}, checkName: {}",
              matchScore,
              threshold,
              loanId,
              checkName);
        } else {
          status = "REJECTED";
          log.info(
              "[KYC_QC][UPDATE_MATCH_SCORE] Match score {} < threshold {}, status: REJECTED for"
                  + " loanId: {}, checkName: {}",
              matchScore,
              threshold,
              loanId,
              checkName);
        }

        // Build data JSON for name match only
        if (isNameMatch) {
          dataJson =
              objectMapper.writeValueAsString(
                  Map.of(
                      "nameMatchScore", scorePercentage,
                      "Name as Per Aadhaar", nameFromAadhaar != null ? nameFromAadhaar : "",
                      "Name", nameFromLos != null ? nameFromLos : ""));
        }

        if (isFaceMatch) {
          dataJson = objectMapper.writeValueAsString(Map.of("matchScore", scorePercentage));
        }
      }

      QcChecksDataTableDTO matchDTO =
          QcChecksDataTableDTO.builder()
              .checkName(checkName)
              .status(status)
              .data(dataJson)
              .score(scoreString)
              .locale("en")
              .dateFormat("dd MMMM yyyy")
              .build();

      log.info(
          "[KYC_QC][UPDATE_MATCH_SCORE] Updating match score in M2P for loanId: {}, checkName: {},"
              + " status: {}, score: {}%",
          loanId, checkName, status, scoreString);

      return m2PWrapperApi
          .updateQcChecksDataTable(matchDTO, loanId)
          .doOnSuccess(
              r ->
                  log.info(
                      "[KYC_QC][UPDATE_MATCH_SCORE] Successfully updated match score in M2P for"
                          + " loanId: {}, checkName: {}, status: {}",
                      loanId,
                      checkName,
                      status))
          .doOnError(
              error ->
                  log.error(
                      "[KYC_QC][UPDATE_MATCH_SCORE][ERROR] Failed to update match score in M2P for"
                          + " loanId: {}, checkName: {}, error: {}",
                      loanId,
                      checkName,
                      error.getMessage()))
          .thenReturn(status);
    } catch (JsonProcessingException e) {
      log.error(
          "[KYC_QC][UPDATE_MATCH_SCORE][ERROR] Failed to serialize match data for loanId: {},"
              + " checkName: {}, error: {}",
          loanId,
          checkName,
          e.getMessage());
      return Mono.error(e);
    }
  }

  /** Executes face match validation based on execution mode. */
  private Mono<FaceMatchScores> executeFaceMatch(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      String productCode,
      String clientPhotoFromLos,
      String clientPhotoFromAadhaar,
      KycValidationExecution faceMatchExecution,
      KycValidationVendors faceMatchPriority,
      Double faceMatchThreshold,
      boolean faceFallbackEnabled) {

    if (KycValidationExecution.PARALLEL.equals(faceMatchExecution)) {
      log.info(
          "[KYC_QC] Face match execution mode is PARALLEL for loanId: {}, leadId: {},"
              + " productCode: {}",
          aadhaarXmlValidationDTO.getLoanId(),
          aadhaarXmlValidationDTO.getLeadId(),
          productCode);
      return executeParallelFaceMatch(
          aadhaarXmlValidationDTO,
          clientPhotoFromLos,
          clientPhotoFromAadhaar,
          faceMatchPriority,
          faceMatchThreshold,
          faceFallbackEnabled);
    } else if (KycValidationExecution.SEQUENCE.equals(faceMatchExecution)) {
      log.info(
          "[KYC_QC] Face match execution mode is SEQUENCE for loanId: {}, leadId: {},"
              + " productCode: {}",
          aadhaarXmlValidationDTO.getLoanId(),
          aadhaarXmlValidationDTO.getLeadId(),
          productCode);
      return executeSequenceFaceMatch(
          aadhaarXmlValidationDTO,
          clientPhotoFromLos,
          clientPhotoFromAadhaar,
          faceMatchPriority,
          faceMatchThreshold,
          faceFallbackEnabled);
    } else {
      log.error(
          "[KYC_QC] Unknown face match execution mode: {} for leadId: {}",
          faceMatchExecution,
          aadhaarXmlValidationDTO.getLeadId());
      return Mono.just(new FaceMatchScores());
    }
  }

  /**
   * Checks if KYC validation is decoupled based on product configuration.
   *
   * @param flowData the flow configuration data
   * @param leadId the lead ID for logging
   * @param productCode the product code for logging
   * @return true if KYC validation is decoupled, false otherwise
   */
  private boolean isKycValidationDecoupled(
      ProductControl.Flow flowData, String leadId, String productCode) {
    boolean isDecoupled =
        flowData.getConditions() != null
            && flowData.getConditions().containsKey("kycValidationFlag")
            && Boolean.TRUE.equals(flowData.getConditions().get("kycValidationFlag"));

    log.info(
        "[KYC_QC] KYC validation decouple check result: {} for leadId: {}," + " productCode: {}",
        isDecoupled,
        leadId,
        productCode);

    if (!isDecoupled) {
      log.info(
          "[KYC_QC] KYC Validation is not decoupled for product code: {}, leadId: {}."
              + " Skipping validation.",
          productCode,
          leadId);
    }

    return isDecoupled;
  }

  /**
   * Validates that all required config fields are present. If any field is missing, sets a default
   * value and logs a warning instead of throwing an error. The defaults are set directly in
   * flowData.conditions so they are available for subsequent processing.
   *
   * @param flowData the flow configuration data
   * @param leadId the lead ID for logging
   * @param productCode the product code for logging
   */
  private void validateRequiredConfigFields(
      ProductControl.Flow flowData, String leadId, String productCode) {

    // Default values for KYC validation config fields
    final String defaultNameMatchExecution = KycValidationExecution.PARALLEL.name();
    final String defaultFaceMatchExecution = KycValidationExecution.PARALLEL.name();
    final String defaultNameMatchPriority = KycValidationVendors.KARZA.name();
    final String defaultFaceMatchPriority = KycValidationVendors.KARZA.name();
    final String defaultNameMatchThreshold = "0.5";
    final String defaultFaceMatchThreshold = "0.5";
    final String defaultNameFallback = "false";
    final String defaultFaceFallback = "false";
    final String defaultXmlValidity = "24";

    // Initialize conditions map if null and set it back to flowData
    if (flowData.getConditions() == null) {
      log.info(
          "[KYC_QC][CONFIG] conditions map is null for leadId: {}, productCode: {}. "
              + "Initializing with empty HashMap.",
          leadId,
          productCode);
      flowData.setConditions(new HashMap<>());
    }

    // Get reference to the conditions map (now guaranteed to be non-null)
    HashMap<String, Object> conditions = flowData.getConditions();

    // Check and set default for NAME_MATCH_EXECUTION
    if (!conditions.containsKey(NAME_MATCH_EXECUTION)) {
      log.info(
          "[KYC_QC][CONFIG] {} is missing in product config for leadId: {}, productCode: {}. "
              + "Setting default value: {}",
          NAME_MATCH_EXECUTION,
          leadId,
          productCode,
          defaultNameMatchExecution);
      conditions.put(NAME_MATCH_EXECUTION, defaultNameMatchExecution);
    }

    // Check and set default for FACE_MATCH_EXECUTION
    if (!conditions.containsKey(FACE_MATCH_EXECUTION)) {
      log.info(
          "[KYC_QC][CONFIG] {} is missing in product config for leadId: {}, productCode: {}. "
              + "Setting default value: {}",
          FACE_MATCH_EXECUTION,
          leadId,
          productCode,
          defaultFaceMatchExecution);
      conditions.put(FACE_MATCH_EXECUTION, defaultFaceMatchExecution);
    }

    // Check and set default for NAME_MATCH_PRIORITY
    if (!conditions.containsKey(NAME_MATCH_PRIORITY)) {
      log.info(
          "[KYC_QC][CONFIG] {} is missing in product config for leadId: {}, productCode: {}. "
              + "Setting default value: {}",
          NAME_MATCH_PRIORITY,
          leadId,
          productCode,
          defaultNameMatchPriority);
      conditions.put(NAME_MATCH_PRIORITY, defaultNameMatchPriority);
    }

    // Check and set default for FACE_MATCH_PRIORITY
    if (!conditions.containsKey(FACE_MATCH_PRIORITY)) {
      log.info(
          "[KYC_QC][CONFIG] {} is missing in product config for leadId: {}, productCode: {}. "
              + "Setting default value: {}",
          FACE_MATCH_PRIORITY,
          leadId,
          productCode,
          defaultFaceMatchPriority);
      conditions.put(FACE_MATCH_PRIORITY, defaultFaceMatchPriority);
    }

    // Check and set default for NAME_MATCH_THRESHOLD
    if (!conditions.containsKey(NAME_MATCH_THRESHOLD)) {
      log.info(
          "[KYC_QC][CONFIG] {} is missing in product config for leadId: {}, productCode: {}. "
              + "Setting default value: {}",
          NAME_MATCH_THRESHOLD,
          leadId,
          productCode,
          defaultNameMatchThreshold);
      conditions.put(NAME_MATCH_THRESHOLD, defaultNameMatchThreshold);
    }

    // Check and set default for FACE_MATCH_THRESHOLD
    if (!conditions.containsKey(FACE_MATCH_THRESHOLD)) {
      log.info(
          "[KYC_QC][CONFIG] {} is missing in product config for leadId: {}, productCode: {}. "
              + "Setting default value: {}",
          FACE_MATCH_THRESHOLD,
          leadId,
          productCode,
          defaultFaceMatchThreshold);
      conditions.put(FACE_MATCH_THRESHOLD, defaultFaceMatchThreshold);
    }

    // Check and set default for NAME_FALLBACK
    if (!conditions.containsKey(NAME_FALLBACK)) {
      log.info(
          "[KYC_QC][CONFIG] {} is missing in product config for leadId: {}, productCode: {}. "
              + "Setting default value: {}",
          NAME_FALLBACK,
          leadId,
          productCode,
          defaultNameFallback);
      conditions.put(NAME_FALLBACK, defaultNameFallback);
    }

    // Check and set default for FACE_FALLBACK
    if (!conditions.containsKey(FACE_FALLBACK)) {
      log.info(
          "[KYC_QC][CONFIG] {} is missing in product config for leadId: {}, productCode: {}. "
              + "Setting default value: {}",
          FACE_FALLBACK,
          leadId,
          productCode,
          defaultFaceFallback);
      conditions.put(FACE_FALLBACK, defaultFaceFallback);
    }

    // Check and set default for XML_VALIDITY
    if (!conditions.containsKey(XML_VALIDITY)) {
      log.info(
          "[KYC_QC][CONFIG] {} is missing in product config for leadId: {}, productCode: {}. "
              + "Setting default value: {} months",
          XML_VALIDITY,
          leadId,
          productCode,
          defaultXmlValidity);
      conditions.put(XML_VALIDITY, defaultXmlValidity);
    }
  }

  /**
   * Performs XML validity check with comprehensive logging.
   *
   * @param aadhaarXmlValidationDTO the validation DTO
   * @param xmlValidityMonths the validity period in months
   * @param productCode the product code for logging
   * @return Mono<Void> that completes when check is done
   */
  private Mono<Void> performXmlValidityCheckWithLogging(
      AadhaarXmlValidationDTO aadhaarXmlValidationDTO,
      Long xmlValidityMonths,
      String productCode,
      KycQcEntity kycQcEntity) {
    log.info(
        "[KYC_QC] XML validity check configured for {} months for leadId: {}," + " productCode: {}",
        xmlValidityMonths,
        aadhaarXmlValidationDTO.getLeadId(),
        productCode);

    return performXmlValidityCheck(
            aadhaarXmlValidationDTO.getClientXmlDetailsDTO(),
            xmlValidityMonths,
            aadhaarXmlValidationDTO.getLoanId(),
            kycQcEntity)
        .doOnSuccess(
            v ->
                log.info(
                    "[KYC_QC] XML validity check completed and updated in M2P for" + " loanId: {}",
                    aadhaarXmlValidationDTO.getLoanId()))
        .doOnError(
            error ->
                log.error(
                    "[KYC_QC][ERROR] XML validity check failed for loanId: {}, error:" + " {}",
                    aadhaarXmlValidationDTO.getLoanId(),
                    error.getMessage()))
        .onErrorResume(
            error -> {
              log.error(
                  "[KYC_QC] Continuing despite XML validity check failure for loanId:" + " {}",
                  aadhaarXmlValidationDTO.getLeadId());
              return Mono.empty();
            });
  }

  private void publishEventKafkaAsync(Runnable eventTask) {
    Mono.fromRunnable(eventTask)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(null, error -> log.error("Error while publishing the event", error));
  }
}
