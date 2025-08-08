package com.br.workflow_cmmn.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class WorkflowInstance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String workflowName;
    private String startedBy;
    private String status; // ACTIVE, COMPLETED, FAILED
    private LocalDateTime scheduledStart;
    private String frequency; // ONCE, DAILY, WEEKLY, MONTHLY
    private LocalDateTime actualStart;
    private LocalDateTime endTime;
    private String uploader;
    private String preparator;
    private String reviewer;
    private String instructions;
}