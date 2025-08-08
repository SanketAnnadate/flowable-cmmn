package com.br.workflow_cmmn.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String userId;
    private String message;
    private String type; // INFO, SUCCESS, ERROR
    private boolean read;
    private LocalDateTime createdAt;
}