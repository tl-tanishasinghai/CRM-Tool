package com.trillionloans.los.service.db;

import com.trillionloans.los.model.entity.ScienapticEntity;
import com.trillionloans.los.repository.ScienapticStoreRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service class for handling operations related to ScienapticEntity. This service provides methods
 * to interact with the ScienapticStoreRepository, such as saving entities to the database. Created
 * on: 2024-09-24 Last updated on: 2024-09-24
 *
 * @author Ganesh Budhwant
 */
@Service
@AllArgsConstructor
public class ScienapticStoreService {
  private ScienapticStoreRepository scienapticStoreRepository;

  public Mono<ScienapticEntity> save(ScienapticEntity scienapticEntity) {
    return scienapticStoreRepository.save(scienapticEntity);
  }
}
