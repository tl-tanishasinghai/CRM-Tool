package com.trillionloans.lms.service.db;

import com.trillionloans.lms.model.dto.internal.ClientConsentDTO;
import com.trillionloans.lms.model.entity.ClientConsentEntity;
import com.trillionloans.lms.model.response.ClientConsentResponse;
import com.trillionloans.lms.repository.ClientConsentRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
@Slf4j
public class ClientConsentService {

  private ClientConsentRepository clientConsentRepository;

  public Mono<ClientConsentEntity> save(String clientID, ClientConsentDTO clientConsentDTO) {
    ClientConsentEntity clientConsentEntity =
        ClientConsentEntity.builder()
            .clientId(clientID)
            .consentKey(clientConsentDTO.getConsentKey())
            .consentStatus(clientConsentDTO.getConsentStatus())
            .ipAddress(clientConsentDTO.getIpAddress())
            .build();
    return clientConsentRepository.save(clientConsentEntity);
  }

  public Mono<ClientConsentResponse> findTopByClientIdOrderByCreatedAtDesc(String clientId) {
    return clientConsentRepository
        .findTopByClientIdOrderByCreatedAtDesc(clientId)
        .map(
            entity -> {
              ClientConsentResponse response = new ClientConsentResponse();
              response.setClientId(entity.getClientId());
              response.setIpAddress(entity.getIpAddress());
              response.setConsentKey(entity.getConsentKey());
              response.setConsentStatus(entity.getConsentStatus());
              return response;
            });
  }
}
