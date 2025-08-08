package com.br.workflow_cmmn.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class WorkflowTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String taskName; // START, UPLOAD, PREPARE, REVIEW, END
    private String assignee;
    private String status; // PENDING, COMPLETED, REJECTED
    private Long workflowInstanceId;
    private String originalFilePath;
    private String preparedFilePath;
    private String reviewerMessage;
    private String instructions;
    private String userComments; // Comments added by user performing the task
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime startDate; // When task should start
    private LocalDateTime endDate; // When task should end
}