package com.restrosync.backend.repository;

import com.restrosync.backend.model.MenuItem;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MenuRepository extends MongoRepository<MenuItem, String> {
}