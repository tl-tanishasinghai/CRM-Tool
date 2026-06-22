package com.trillionloans.los.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import com.trillionloans.los.model.request.m2p.AdvancedReportRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
class AdvanceReportServiceTest {

  @Mock private M2PWrapperApi m2PWrapperApi;

  @InjectMocks private AdvanceReportService advanceReportService;

  @Test
  void testInitialiseAdvanceReport() {
    AdvancedReportRequestDTO requestDTO = new AdvancedReportRequestDTO();
    Object mockResponse = new Object();
    when(m2PWrapperApi.callAdvancedReportApi(requestDTO)).thenReturn(Mono.just(mockResponse));

    Mono<Object> result = advanceReportService.initialiseAdvanceReport(requestDTO);

    StepVerifier.create(result).expectNext(mockResponse).verifyComplete();

    verify(m2PWrapperApi, times(1)).callAdvancedReportApi(requestDTO);
  }

  @Test
  void testGetPollForFileLocationId() {
    String resourceId = "resource123";
    Object mockResponse = new Object();
    when(m2PWrapperApi.pollForFileLocationId(resourceId)).thenReturn(Mono.just(mockResponse));

    Mono<Object> result = advanceReportService.getPollForFileLocationId(resourceId);

    StepVerifier.create(result).expectNext(mockResponse).verifyComplete();

    verify(m2PWrapperApi, times(1)).pollForFileLocationId(resourceId);
  }

  @Test
  void testDownloadReportData() {
    String fileLocationId = "file123";
    byte[] mockBytes = new byte[] {1, 2, 3};
    DataBuffer mockDataBuffer = new DefaultDataBufferFactory().wrap(mockBytes);

    when(m2PWrapperApi.downloadReportData(fileLocationId)).thenReturn(Flux.just(mockDataBuffer));

    Flux<DataBuffer> result = advanceReportService.getDownloadReportData(fileLocationId);

    StepVerifier.create(result)
        .consumeNextWith(
            dataBuffer -> {
              byte[] bytes = new byte[dataBuffer.readableByteCount()];
              dataBuffer.read(bytes);
              assertArrayEquals(mockBytes, bytes);
            })
        .verifyComplete();

    verify(m2PWrapperApi, times(1)).downloadReportData(fileLocationId);
  }
}
