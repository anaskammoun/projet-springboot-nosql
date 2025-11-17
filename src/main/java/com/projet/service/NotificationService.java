package com.projet.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.projet.entity.Notification;
import com.projet.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;

    public Notification save(Notification n) { return repo.save(n); }
    public List<Notification> findAll() { return repo.findAll(); }
    public Notification findById(String id) { return repo.findById(id).orElse(null); }
    public void delete(String id) { repo.deleteById(id); }
}
