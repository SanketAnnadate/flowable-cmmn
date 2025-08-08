package com.br.workflow_cmmn.service;

import com.br.workflow_cmmn.model.*;
import com.br.workflow_cmmn.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

/**
 * DataInitializationService - Populates the database with sample workflow data on startup
 * 
 * PURPOSE:
 * This service creates realistic sample data to demonstrate all possible workflow states
 * and task combinations. It runs automatically when the application starts and only
 * executes if the database is empty (no existing workflow instances).
 * 
 * SAMPLE DATA CREATED:
 * 1. SCHEDULED workflow - Demonstrates future workflow scheduling
 * 2. ACTIVE with UPLOAD pending - Shows initial workflow state
 * 3. ACTIVE with PREPARE pending - Shows workflow after upload completion
 * 4. ACTIVE with REVIEW pending - Shows workflow ready for final approval
 * 5. ACTIVE with REJECTION cycle - Shows rework loop when reviewer rejects
 * 6. COMPLETED workflow - Shows successful workflow completion
 * 
 * REALISTIC FEATURES:
 * - Proper task sequencing (upload -> prepare -> review)
 * - Realistic timestamps spread across multiple days
 * - Sample file paths for uploaded documents
 * - Reviewer feedback messages for rejections and approvals
 * - Notifications for task assignments and completions
 * - User assignments across different roles
 * 
 * USER ASSIGNMENTS:
 * - User 1: Admin (creates workflows)
 * - User 2: Uploader/Preparator
 * - User 3: Uploader/Reviewer  
 * - User 4: Preparator/Reviewer
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements CommandLineRunner {
    
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowTaskRepository workflowTaskRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Runs automatically on application startup
     * Only creates sample data if database is empty to avoid duplicates
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("=== DATA INITIALIZATION SERVICE STARTING ===");
        
        long existingWorkflows = workflowInstanceRepository.count();
        log.info("Found {} existing workflow instances in database", existingWorkflows);
        
        if (existingWorkflows == 0) {
            log.info("Database is empty - creating sample workflow data");
            createDummyWorkflows();
            log.info("Sample data creation completed successfully");
        } else {
            log.info("Database already contains data - skipping sample data creation");
        }
        
        log.info("=== DATA INITIALIZATION SERVICE COMPLETED ===");
    }

    /**
     * Creates 6 sample workflows demonstrating all possible states and transitions
     * 
     * WORKFLOW STATES DEMONSTRATED:
     * 1. SCHEDULED - Future workflow waiting to start
     * 2. ACTIVE/UPLOAD - Workflow started, waiting for file upload
     * 3. ACTIVE/PREPARE - Upload done, waiting for document preparation
     * 4. ACTIVE/REVIEW - Prepare done, waiting for reviewer decision
     * 5. ACTIVE/REWORK - Document rejected, back to preparation
     * 6. COMPLETED - All stages approved, workflow finished
     */
    private void createDummyWorkflows() {
        log.info("Creating sample workflow data...");
        LocalDateTime now = LocalDateTime.now();
        log.debug("Base timestamp for sample data: {}", now);
        
        // 1. Scheduled workflow (not started yet)
        WorkflowInstance w1 = createWorkflow("Future Monthly Report", "1", "2", "3", "4", "SCHEDULED", now.plusHours(2));
        // No tasks created yet - will be created when scheduled time hits
        
        // 2. Workflow with PENDING upload task
        WorkflowInstance w2 = createWorkflow("Monthly Sales Report", "1", "2", "3", "4", "ACTIVE", now.minusDays(1));
        createTask(w2.getId(), "UPLOAD", "2", "PENDING", now.minusDays(1), null, null, null, "Please upload the monthly sales data in Excel format");
        
        // 3. Workflow with PENDING prepare task (upload completed)
        WorkflowInstance w3 = createWorkflow("Quarterly Budget Review", "1", "3", "4", "2", "ACTIVE", now.minusDays(2));
        createTask(w3.getId(), "UPLOAD", "3", "COMPLETED", now.minusDays(2), now.minusDays(2), "uploads/budget_original.xlsx", null, null);
        createTask(w3.getId(), "PREPARE", "4", "PENDING", now.minusDays(2), null, "uploads/budget_original.xlsx", null, "Review and format the budget data according to company standards");
        
        // 4. Workflow with PENDING review task (prepare completed)
        WorkflowInstance w4 = createWorkflow("Annual Performance Analysis", "1", "4", "2", "3", "ACTIVE", now.minusDays(3));
        createTask(w4.getId(), "UPLOAD", "4", "COMPLETED", now.minusDays(3), now.minusDays(3), "uploads/performance_original.xlsx", null, null);
        createTask(w4.getId(), "PREPARE", "2", "COMPLETED", now.minusDays(2), now.minusDays(2), "uploads/performance_original.xlsx", "uploads/performance_prepared.xlsx", null);
        createTask(w4.getId(), "REVIEW", "3", "PENDING", now.minusDays(2), null, "uploads/performance_original.xlsx", "uploads/performance_prepared.xlsx", null);
        
        // 5. Workflow with REJECTED review (back to prepare)
        WorkflowInstance w5 = createWorkflow("Weekly Inventory Report", "1", "2", "4", "3", "ACTIVE", now.minusDays(4));
        createTask(w5.getId(), "UPLOAD", "2", "COMPLETED", now.minusDays(4), now.minusDays(4), "uploads/inventory_original.xlsx", null, null);
        createTask(w5.getId(), "PREPARE", "4", "COMPLETED", now.minusDays(3), now.minusDays(3), "uploads/inventory_original.xlsx", "uploads/inventory_prepared_v1.xlsx", null);
        createTask(w5.getId(), "REVIEW", "3", "REJECTED", now.minusDays(3), now.minusDays(1), "uploads/inventory_original.xlsx", "uploads/inventory_prepared_v1.xlsx", "Data formatting is incorrect. Please fix column headers and resubmit.");
        createTask(w5.getId(), "PREPARE", "4", "PENDING", now.minusDays(1), null, "uploads/inventory_original.xlsx", null, null);
        
        // 6. COMPLETED workflow
        WorkflowInstance w6 = createWorkflow("Customer Satisfaction Survey", "1", "3", "2", "4", "COMPLETED", now.minusDays(5));
        w6.setEndTime(now.minusHours(2));
        workflowInstanceRepository.save(w6);
        createTask(w6.getId(), "UPLOAD", "3", "COMPLETED", now.minusDays(5), now.minusDays(5), "uploads/survey_original.xlsx", null, null);
        createTask(w6.getId(), "PREPARE", "2", "COMPLETED", now.minusDays(4), now.minusDays(4), "uploads/survey_original.xlsx", "uploads/survey_prepared.xlsx", null);
        createTask(w6.getId(), "REVIEW", "4", "COMPLETED", now.minusDays(4), now.minusHours(2), "uploads/survey_original.xlsx", "uploads/survey_prepared.xlsx", "Excellent work! Data is clean and well-formatted. Approved.");
        
        // Create some notifications
        createNotification("2", "New upload task assigned: Monthly Sales Report", "INFO", now.minusDays(1));
        createNotification("4", "New preparation task assigned", "INFO", now.minusDays(2));
        createNotification("3", "New review task assigned", "INFO", now.minusDays(2));
        createNotification("4", "Document rejected: Data formatting is incorrect. Please fix column headers and resubmit.", "ERROR", now.minusDays(1));
        createNotification("3", "Workflow completed successfully!", "SUCCESS", now.minusHours(2));
        createNotification("2", "Workflow completed successfully!", "SUCCESS", now.minusHours(2));
        createNotification("4", "Workflow completed successfully!", "SUCCESS", now.minusHours(2));
    }
    
    private WorkflowInstance createWorkflow(String name, String startedBy, String uploader, String preparator, String reviewer, String status, LocalDateTime startTime) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowName(name);
        instance.setStartedBy(startedBy);
        instance.setStatus(status);
        instance.setScheduledStart(startTime);
        instance.setFrequency("ONCE");
        instance.setUploader(uploader);
        instance.setPreparator(preparator);
        instance.setReviewer(reviewer);
        instance.setInstructions("Please complete your assigned tasks according to company standards.");
        instance.setActualStart(startTime);
        return workflowInstanceRepository.save(instance);
    }
    
    private void createTask(Long workflowId, String taskName, String assignee, String status, LocalDateTime created, LocalDateTime completed, String originalFile, String preparedFile, String message) {
        WorkflowTask task = new WorkflowTask();
        task.setWorkflowInstanceId(workflowId);
        task.setTaskName(taskName);
        task.setAssignee(assignee);
        task.setStatus(status);
        task.setCreatedAt(created);
        task.setCompletedAt(completed);
        task.setOriginalFilePath(originalFile);
        task.setPreparedFilePath(preparedFile);
        task.setReviewerMessage(message);
        task.setInstructions("Complete this task according to workflow requirements");
        workflowTaskRepository.save(task);
    }
    
    private void createNotification(String userId, String message, String type, LocalDateTime created) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false);
        notification.setCreatedAt(created);
        notificationRepository.save(notification);
    }
}