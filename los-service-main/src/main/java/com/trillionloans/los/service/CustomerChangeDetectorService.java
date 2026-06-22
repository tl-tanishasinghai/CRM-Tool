package com.trillionloans.los.service;

import static com.trillionloans.los.constant.StringConstants.ACCOUNT_NUMBER;
import static com.trillionloans.los.constant.StringConstants.ADDRESS_LINE_ONE;
import static com.trillionloans.los.constant.StringConstants.ADDRESS_LINE_TWO;
import static com.trillionloans.los.constant.StringConstants.ALTERNATE_MOBILE_NUMBER;
import static com.trillionloans.los.constant.StringConstants.BANK_ACCOUNT_TYPE;
import static com.trillionloans.los.constant.StringConstants.DOB;
import static com.trillionloans.los.constant.StringConstants.EMAIL;
import static com.trillionloans.los.constant.StringConstants.FIRST_NAME;
import static com.trillionloans.los.constant.StringConstants.GENDER;
import static com.trillionloans.los.constant.StringConstants.IFSC_CODE;
import static com.trillionloans.los.constant.StringConstants.LAST_NAME;
import static com.trillionloans.los.constant.StringConstants.MIDDLE_NAME;
import static com.trillionloans.los.constant.StringConstants.MOBILE_NUMBER;
import static com.trillionloans.los.constant.StringConstants.NAME;
import static com.trillionloans.los.constant.StringConstants.POSTAL_CODE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.dto.internal.FieldChange;
import com.trillionloans.los.model.entity.CustomerDataVariance;
import com.trillionloans.los.model.partner.m2p.M2pAddressDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pBankDetailsDTO;
import com.trillionloans.los.model.partner.m2p.M2pLeadRequestDTO;
import com.trillionloans.los.repository.CustomerDataVarianceRepository;
import io.r2dbc.postgresql.codec.Json;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
@AllArgsConstructor
public class CustomerChangeDetectorService {

  private final M2PWrapperApi m2pWrapperApi;
  private final CustomerDataVarianceRepository customerDataVarianceRepository;
  private final ObjectMapper objectMapper;

  /**
   * Asynchronously detects changes between new lead data and existing customer data.
   *
   * @param clientId The ID of the existing customer.
   * @param newData The new data from the API request.
   */
  public Mono<Void> detectChangesAsync(Integer clientId, M2pLeadRequestDTO newData) {
    return Mono.deferContextual(
        contextView -> {
          return performComparison(String.valueOf(clientId), newData)
              .subscribeOn(Schedulers.boundedElastic())
              .contextWrite(context -> context.putAll(contextView))
              .doOnError(
                  error ->
                      log.error(
                          "error during change detection for client: {}. error: {}",
                          clientId,
                          error.getMessage()))
              .then();
        });
  }

  /**
   * Orchestrates the entire change detection process.
   *
   * @param clientId The id of the client
   * @param newData The DTO containing the new customer information from the LSP request.
   */
  private Mono<Void> performComparison(String clientId, M2pLeadRequestDTO newData) {
    return m2pWrapperApi
        .getLeadData(clientId)
        .switchIfEmpty(
            Mono.fromRunnable(
                () -> log.error("could not find existing data for clientId: {}.", clientId)))
        .flatMap(
            existingData -> {
              List<FieldChange> changesList = new ArrayList<>();

              compareNormalizedFields(
                  changesList,
                  FIRST_NAME,
                  existingData.getFirstName(),
                  newData.getClientData().getFirstName());
              compareNormalizedFields(
                  changesList,
                  MIDDLE_NAME,
                  existingData.getMiddleName(),
                  newData.getClientData().getMiddleName());
              compareNormalizedFields(
                  changesList,
                  LAST_NAME,
                  existingData.getLastName(),
                  newData.getClientData().getLastName());

              compareNormalizedDates(
                  changesList,
                  DOB,
                  existingData.getDateOfBirth(),
                  newData.getClientData().getDateOfBirth(),
                  clientId);
              compareNormalizedFields(
                  changesList,
                  GENDER,
                  existingData.getGender(),
                  newData.getClientData().getGender());

              addChangeToList(
                  changesList,
                  MOBILE_NUMBER,
                  existingData.getMobileNo(),
                  newData.getClientData().getMobileNo());
              addChangeToList(
                  changesList,
                  ALTERNATE_MOBILE_NUMBER,
                  existingData.getAlternateMobileNo(),
                  newData.getClientData().getAlternateMobileNo());
              addChangeToList(
                  changesList, EMAIL, existingData.getEmail(), newData.getClientData().getEmail());

              List<M2pAddressDetailsDTO> newAddresses = newData.getAddressData();
              if (newAddresses != null && !newAddresses.isEmpty()) {
                for (M2pAddressDetailsDTO newAddress : newAddresses) {
                  if (newAddress
                      .getAddressType()
                      .get(0)
                      .equalsIgnoreCase(existingData.getAddressType())) {
                    compareNormalizedFields(
                        changesList,
                        ADDRESS_LINE_ONE,
                        existingData.getAddressLineOne(),
                        newAddress.getAddressLineOne());
                    compareNormalizedFields(
                        changesList,
                        ADDRESS_LINE_TWO,
                        existingData.getAddressLineTwo(),
                        newAddress.getAddressLineTwo());
                    addChangeToList(
                        changesList,
                        POSTAL_CODE,
                        existingData.getPostalCode(),
                        newAddress.getPostalCode());
                  }
                }
              }

              List<M2pBankDetailsDTO> newBankDetailsList = newData.getBankDetailsData();
              if (newBankDetailsList != null && !newBankDetailsList.isEmpty()) {
                M2pBankDetailsDTO newBankDetails = newData.getBankDetailsData().get(0);
                compareNormalizedFields(
                    changesList,
                    BANK_ACCOUNT_TYPE,
                    existingData.getBankAccountType(),
                    newBankDetails.getAccountType());
                compareNormalizedFields(
                    changesList, NAME, existingData.getName(), newBankDetails.getName());
                addChangeToList(
                    changesList,
                    ACCOUNT_NUMBER,
                    existingData.getAccountNumber(),
                    newBankDetails.getAccountNumber());
                addChangeToList(
                    changesList,
                    IFSC_CODE,
                    existingData.getIfscCode(),
                    newBankDetails.getIfscCode());
              }

              if (changesList.isEmpty()) {
                log.info("no data variances found for clientId: {}", clientId);
                return Mono.empty();
              }

              return Mono.fromCallable(
                      () -> {
                        String changesJsonString = objectMapper.writeValueAsString(changesList);
                        Json changesJsonbObject = Json.of(changesJsonString);

                        CustomerDataVariance varianceEvent = new CustomerDataVariance();
                        varianceEvent.setClientId(clientId);
                        varianceEvent.setChangedFields(changesJsonbObject);
                        return varianceEvent;
                      })
                  .doOnError(
                      error ->
                          log.error(
                              "failed to serialize change list for clientId: {}. error: {}",
                              clientId,
                              error.getMessage()))
                  .flatMap(
                      varianceEvent ->
                          customerDataVarianceRepository
                              .save(varianceEvent)
                              .doOnSuccess(
                                  savedEntity ->
                                      log.info(
                                          "saved variance for clientId: {}",
                                          savedEntity.getClientId()))
                              .doOnError(
                                  error ->
                                      log.error(
                                          "failed to save variance event for clientId: {}. error:"
                                              + " {}",
                                          varianceEvent.getClientId(),
                                          error.getMessage())))
                  .then();
            });
  }

  /**
   * Safely compares two field values and adds a FieldChange DTO to a list if they are different.
   * This helper method is null-safe.
   *
   * @param changes The list of FieldChange objects to which a new change will be added.
   * @param fieldName The machine-readable name of the attribute being compared (e.g., "mobileNo").
   * @param oldValue The original value of the field, which can be null.
   * @param newValue The new value of the field from the request, which can be null.
   */
  private void addChangeToList(
      List<FieldChange> changes, String fieldName, String oldValue, String newValue) {
    if (newValue != null && !newValue.isBlank() && !Objects.equals(oldValue, newValue)) {
      changes.add(new FieldChange(fieldName, oldValue, newValue));
    }
  }

  /**
   * Compares date strings by parsing them into a standard format first.
   *
   * @param changes The list of FieldChange objects to add to.
   * @param fieldName The name of the address field being checked.
   * @param oldDateStr The original date string from the database.
   * @param newDateStr The new date string from the API request.
   * @param clientId The id of the client
   */
  private void compareNormalizedDates(
      List<FieldChange> changes,
      String fieldName,
      String oldDateStr,
      String newDateStr,
      String clientId) {
    try {
      LocalDate oldDate =
          (oldDateStr != null && !oldDateStr.isEmpty())
              ? LocalDate.parse(
                  oldDateStr, DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
              : null;
      LocalDate newDate =
          (newDateStr != null && !newDateStr.isEmpty())
              ? LocalDate.parse(newDateStr, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
              : null;

      if (newDate != null && !Objects.equals(oldDate, newDate)) {
        changes.add(new FieldChange(fieldName, oldDateStr, newDateStr));
      }
    } catch (DateTimeParseException e) {
      log.error("could not parse date strings for field: {}, clientId : {}", fieldName, clientId);
    }
  }

  /**
   * Compares two strings by normalizing them first. Normalization includes converting to lowercase
   * and removing all whitespace.
   *
   * @param changes The list of FieldChange objects to add to.
   * @param fieldName The name of the field being checked.
   * @param oldValue The original string from the database.
   * @param newValue The new string from the API request.
   */
  private void compareNormalizedFields(
      List<FieldChange> changes, String fieldName, String oldValue, String newValue) {
    String oldNormalized = (oldValue == null) ? "" : oldValue;
    String newNormalized = (newValue == null) ? "" : newValue;

    oldNormalized = oldNormalized.toLowerCase().replaceAll("\\s+", "");
    newNormalized = newNormalized.toLowerCase().replaceAll("\\s+", "");

    if (!Objects.equals(oldNormalized, newNormalized)) {
      changes.add(new FieldChange(fieldName, oldValue, newValue));
    }
  }
}
