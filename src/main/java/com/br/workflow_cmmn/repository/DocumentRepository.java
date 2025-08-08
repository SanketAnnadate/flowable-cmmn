package com.br.workflow_cmmn.repository;

import com.br.workflow_cmmn.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUploadedBy(String uploadedBy);
    List<Document> findByAssignedReviewer(String assignedReviewer);
    List<Document> findByStatus(String status);
}