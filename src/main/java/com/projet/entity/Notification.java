package com.projet.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Data
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private String type;     // conteneur plein / incident
    private String message;
    private String collectPointId;

    private long timestamp;
}
