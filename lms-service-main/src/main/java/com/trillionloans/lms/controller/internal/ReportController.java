package com.trillionloans.lms.controller.internal;

import com.trillionloans.lms.model.request.AdvancedReportRequestDTO;
import com.trillionloans.lms.service.AdvanceReportService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/internal/advancereport")
@AllArgsConstructor
@RestController
@Hidden
@Slf4j
public class ReportController {
  private final AdvanceReportService advanceReportService;

  /**
   * Initializes the advanced report generation process.
   *
   * <p>This endpoint accepts a JSON request containing display parameters, report parameters, and
   * the report name, then triggers the generation of an advanced report.
   *
   * @param requestDTO the request body containing the report details
   * @return a {@link Mono} containing a {@link ResponseEntity} with the result of the
   *     initialization
   */
  @PostMapping
  public Mono<ResponseEntity<Mono<?>>> initialiseAdvanceReport(
      @Valid @RequestBody AdvancedReportRequestDTO requestDTO) {
    return Mono.just(ResponseEntity.ok(advanceReportService.initialiseAdvanceReport(requestDTO)));
  }

  /**
   * Polls for the file location ID using the resource ID.
   *
   * <p>This endpoint checks the status of the report generation process and retrieves the file
   * location ID when it becomes available.
   *
   * @param resourceId the unique identifier for the resource
   * @return a {@link Mono} containing a {@link ResponseEntity} with the file location ID
   */
  @GetMapping("/{resourceId}")
  public Mono<ResponseEntity<Mono<?>>> pollForFileLocationId(
      @Valid @PathVariable String resourceId) {
    return Mono.just(ResponseEntity.ok(advanceReportService.getPollForFileLocationId(resourceId)));
  }

  /**
   * Endpoint to download the report data as a binary file using the specified file location ID.
   *
   * <p>This method streams the report content retrieved from the M2P system and returns it as a
   * downloadable file with the appropriate headers set for file download.
   *
   * @param fileLocationId the unique identifier of the file to be downloaded
   * @return a {@link ResponseEntity} containing a {@link Flux} of {@link DataBuffer} representing
   *     the binary contents of the report file
   */
  @GetMapping(
      value = "/download/{fileLocationId}",
      produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public Flux<DataBuffer> downloadReportData(
      @Valid @PathVariable String fileLocationId, ServerHttpResponse response) {
    response
        .getHeaders()
        .add(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileLocationId + ".csv\"");
    return advanceReportService
        .getDownloadReportData(fileLocationId)
        .doOnSubscribe(sub -> log.info("Started streaming: {}", fileLocationId))
        .doOnComplete(() -> log.info("Completed streaming: {}", fileLocationId))
        .doOnError(e -> log.error("Error: {}", e.getMessage()));
  }
}
