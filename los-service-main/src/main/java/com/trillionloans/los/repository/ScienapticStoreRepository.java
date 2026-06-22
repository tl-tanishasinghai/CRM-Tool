package com.trillionloans.los.repository;

import com.trillionloans.los.model.entity.ScienapticEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for ScienapticEntity operations. This interface extends the R2dbcRepository
 * and provides methods for reactive database operations such as saving, finding, and deleting
 * ScienapticEntity instances. Created on: 2024-09-24 Last updated on: 2024-09-24
 *
 * @author Ganesh Budhwant
 */
@Repository
public interface ScienapticStoreRepository extends R2dbcRepository<ScienapticEntity, Long> {}
