package com.restrosync.backend.repository;

import com.restrosync.backend.model.Table;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TableRepository extends MongoRepository<Table, String> {
    java.util.Optional<Table> findByNumber(String number);
}