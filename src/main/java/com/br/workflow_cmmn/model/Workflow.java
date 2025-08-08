package com.br.workflow_cmmn.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Workflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    private String createdBy;
    private String status; // ACTIVE, INACTIVE
    private LocalDateTime createdAt;
    
    @Column(columnDefinition = "TEXT")
    private String workflowDefinition;
}