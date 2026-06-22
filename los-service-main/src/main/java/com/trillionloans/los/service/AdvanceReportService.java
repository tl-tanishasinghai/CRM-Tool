package com.trillionloans.los.service;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.request.m2p.AdvancedReportRequestDTO;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for managing the advanced report generation, retrieval, and download processes.
 *
 * <p>This service coordinates report generation via the M2PWrapperApi, polling for completion
 * status, and finally downloading the generated report data.
 */
@Service
@Slf4j
public class AdvanceReportService {

  private final M2PWrapperApi m2PWrapperApi;

  /**
   * Constructor for injecting the M2PWrapperApi dependency.
   *
   * @param m2PWrapperApi the API client for interacting with the M2P system
   */
  @Autowired
  public AdvanceReportService(M2PWrapperApi m2PWrapperApi) {
    this.m2PWrapperApi = m2PWrapperApi;
  }

  /**
   * Initializes the advanced report generation process.
   *
   * <p>This method forwards the request to the M2P system to start generating a report based on the
   * provided parameters.
   *
   * @param requestDTO the request data containing parameters for report generation
   * @return a {@link Mono} representing the result of the initialization process
   */
  public Mono<Object> initialiseAdvanceReport(@Valid AdvancedReportRequestDTO requestDTO) {
    return m2PWrapperApi.callAdvancedReportApi(requestDTO);
  }

  /**
   * Polls for the file location ID of the generated report.
   *
   * <p>This method queries the M2P system for the completion status of the report and retrieves the
   * file location ID if the report is ready.
   *
   * @param resourceId the unique identifier for the report generation request
   * @return a {@link Mono} representing the file location ID or status response
   */
  public Mono<Object> getPollForFileLocationId(@Valid String resourceId) {
    return m2PWrapperApi.pollForFileLocationId(resourceId);
  }

  /**
   * Retrieves the report data from the M2P system using the given file location ID.
   *
   * <p>This method downloads the report as a binary stream in the form of {@link DataBuffer}s. It
   * delegates the actual download process to the M2P wrapper API.
   *
   * @param fileLocationId the unique identifier representing the location of the report file
   * @return a {@link Flux} of {@link DataBuffer} representing the binary content of the report
   */
  public Flux<DataBuffer> getDownloadReportData(@Valid String fileLocationId) {
    return m2PWrapperApi.downloadReportData(fileLocationId);
  }
}
