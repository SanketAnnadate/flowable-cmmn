package com.br.workflow_cmmn.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String fileName;
    private String filePath;
    private String uploadedBy;
    private String assignedReviewer;
    private String status; // UPLOADED, UNDER_REVIEW, APPROVED, REJECTED
    private String comments;
    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt;
}