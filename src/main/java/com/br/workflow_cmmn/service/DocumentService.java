package com.br.workflow_cmmn.service;

import com.br.workflow_cmmn.model.Document;
import com.br.workflow_cmmn.model.WorkflowTask;
import com.br.workflow_cmmn.repository.DocumentRepository;
import com.br.workflow_cmmn.repository.WorkflowTaskRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final WorkflowTaskRepository workflowTaskRepository;

    
    public Document uploadDocument(String title, String fileName, String uploadedBy, String processor, String reviewer) {
        Document document = new Document();
        document.setTitle(title);
        document.setFileName(fileName);
        document.setUploadedBy(uploadedBy);
        document.setAssignedReviewer(reviewer);
        document.setStatus("UPLOADED");
        document.setUploadedAt(LocalDateTime.now());
        
        Document saved = documentRepository.save(document);
        
        // Create upload task
        WorkflowTask uploadTask = new WorkflowTask();
        uploadTask.setTaskName("UPLOAD");
        uploadTask.setAssignee(uploadedBy);
        uploadTask.setStatus("COMPLETED");
        uploadTask.setOriginalFilePath(fileName);
        uploadTask.setCreatedAt(LocalDateTime.now());
        uploadTask.setCompletedAt(LocalDateTime.now());
        workflowTaskRepository.save(uploadTask);
        
        // Create process task
        WorkflowTask processTask = new WorkflowTask();
        processTask.setTaskName("PROCESS");
        processTask.setAssignee(processor);
        processTask.setStatus("PENDING");
        processTask.setOriginalFilePath(fileName);
        processTask.setCreatedAt(LocalDateTime.now());
        workflowTaskRepository.save(processTask);
        
        return saved;
    }
    
    public void processDocument(Long taskId, String processedFile) {
        WorkflowTask task = workflowTaskRepository.findById(taskId).orElseThrow();
        task.setPreparedFilePath(processedFile);
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        workflowTaskRepository.save(task);
        
        // Create review task
        WorkflowTask reviewTask = new WorkflowTask();
        reviewTask.setTaskName("REVIEW");
        reviewTask.setAssignee(getReviewerForTask(task));
        reviewTask.setStatus("PENDING");
        reviewTask.setOriginalFilePath(task.getOriginalFilePath());
        reviewTask.setPreparedFilePath(processedFile);
        reviewTask.setCreatedAt(LocalDateTime.now());
        workflowTaskRepository.save(reviewTask);
    }
    
    public void reviewDocuments(Long taskId, String decision, String comments) {
        WorkflowTask task = workflowTaskRepository.findById(taskId).orElseThrow();
        task.setReviewerMessage(comments);
        task.setCompletedAt(LocalDateTime.now());
        
        if ("APPROVED".equals(decision)) {
            task.setStatus("COMPLETED");
        } else {
            task.setStatus("REJECTED");
            // Reopen process task
            WorkflowTask newProcessTask = new WorkflowTask();
            newProcessTask.setTaskName("PROCESS");
            newProcessTask.setAssignee(getProcessorForTask(task));
            newProcessTask.setStatus("PENDING");
            newProcessTask.setOriginalFilePath(task.getOriginalFilePath());
            newProcessTask.setCreatedAt(LocalDateTime.now());
            workflowTaskRepository.save(newProcessTask);
        }
        workflowTaskRepository.save(task);
    }
    
    private String getReviewerForTask(WorkflowTask task) {
        return "2"; // Default reviewer
    }
    
    private String getProcessorForTask(WorkflowTask task) {
        return "3"; // Default processor
    }
    
    public List<Document> getDocumentsByUploader(String uploaderId) {
        return documentRepository.findByUploadedBy(uploaderId);
    }
    
    public List<Document> getDocumentsByReviewer(String reviewerId) {
        return documentRepository.findByAssignedReviewer(reviewerId);
    }
    
    public List<WorkflowTask> getTasksByAssignee(String assignee) {
        return workflowTaskRepository.findByAssignee(assignee);
    }
    
    public List<WorkflowTask> getPendingTasksByAssignee(String assignee) {
        return workflowTaskRepository.findByAssignee(assignee)
            .stream()
            .filter(task -> "PENDING".equals(task.getStatus()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    public List<Document> getAllDocuments() {
        return documentRepository.findAll();
    }
    
    public List<WorkflowTask> getAllTasks() {
        return workflowTaskRepository.findAll();
    }
}