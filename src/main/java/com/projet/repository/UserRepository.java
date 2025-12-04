package com.projet.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.projet.entity.User;

public interface UserRepository extends MongoRepository<User, String> {
    User findByEmail(String email);
}
