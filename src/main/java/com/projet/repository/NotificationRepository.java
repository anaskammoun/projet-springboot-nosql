package com.projet.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.projet.entity.Notification;

public interface NotificationRepository extends MongoRepository<Notification, String> {}