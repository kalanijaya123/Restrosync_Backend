package com.restrosync.backend.repository;

import com.restrosync.backend.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    @org.springframework.data.mongodb.repository.Query("{ 'username': ?0 }")
    Optional<User> findByUsername(String username);

    @org.springframework.data.mongodb.repository.Query("{ 'email': ?0 }")
    Optional<User> findByEmail(String email);

    java.util.List<User> findByRole(String role);
}