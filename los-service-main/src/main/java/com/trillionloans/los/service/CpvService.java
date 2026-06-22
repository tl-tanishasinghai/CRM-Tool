package com.trillionloans.los.service;

import com.trillionloans.los.api.partner.M2PWrapperApi;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@AllArgsConstructor
public class CpvService {
  private final M2PWrapperApi m2PWrapperApi;

  public Mono<?> getCpvStatus(String loanId) {
    return m2PWrapperApi.getCpvStatus(loanId);
  }
}
