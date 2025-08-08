package com.br.workflow_cmmn.service;

import com.br.workflow_cmmn.model.*;
import com.br.workflow_cmmn.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

/**
 * WorkflowExecutionService - Core service for managing document review workflow lifecycle
 * 
 * WORKFLOW OVERVIEW:
 * This service orchestrates a 3-stage document review process:
 * 1. UPLOAD Stage: User uploads original Excel document
 * 2. PREPARE Stage: Preparator processes and formats the document
 * 3. REVIEW Stage: Reviewer approves or rejects the processed document
 * 
 * WORKFLOW STATES:
 * - SCHEDULED: Workflow created but waiting for scheduled start time
 * - ACTIVE: Workflow running with tasks being processed
 * - COMPLETED: All tasks approved and workflow finished successfully
 * 
 * TASK SEQUENCING:
 * - Upload tasks created only when workflow start time is reached
 * - Prepare tasks created only after upload task completion
 * - Review tasks created only after prepare task completion AND upload verification
 * - Rejection loops back to prepare stage for rework
 * 
 * SCHEDULING:
 * - Workflows can be scheduled for future execution
 * - Automatic scheduler checks every minute for workflows to start
 * - Supports one-time and recurring workflows (DAILY, WEEKLY, MONTHLY)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutionService {
    private final WorkflowInstanceRepository workflowInstanceRepository;
    private final WorkflowTaskRepository workflowTaskRepository;
    private final NotificationRepository notificationRepository;
    
    /**
     * Creates a new workflow instance and determines if it should start immediately or be scheduled
     * 
     * WORKFLOW CREATION LOGIC:
     * 1. Create workflow instance with all participant assignments
     * 2. Check if scheduled start time has passed
     * 3. If time has passed: Mark as ACTIVE and create first upload task
     * 4. If time is future: Mark as SCHEDULED and wait for scheduler
     * 
     * @param name Human-readable workflow name for identification
     * @param startedBy User ID of the admin who created this workflow
     * @param scheduledStart When this workflow should begin (can be past, present, or future)
     * @param frequency How often to repeat: ONCE, DAILY, WEEKLY, MONTHLY
     * @param uploader User ID responsible for uploading the original document
     * @param preparator User ID responsible for processing/formatting the document
     * @param reviewer User ID responsible for final approval/rejection
     * @param instructions Detailed instructions for all participants
     * @return Created WorkflowInstance with appropriate status
     */
    public WorkflowInstance startWorkflow(String name, String startedBy, LocalDateTime scheduledStart, 
                                         String frequency, String uploader, String preparator, String reviewer, String instructions) {
        log.info("=== CREATING NEW WORKFLOW ===");
        log.info("Workflow Name: {}", name);
        log.info("Started By: {}", startedBy);
        log.info("Scheduled Start: {}", scheduledStart);
        log.info("Participants - Uploader: {}, Preparator: {}, Reviewer: {}", uploader, preparator, reviewer);
        
        // Create workflow instance with all metadata
        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowName(name);
        instance.setStartedBy(startedBy);
        instance.setScheduledStart(scheduledStart);
        instance.setFrequency(frequency);
        instance.setUploader(uploader);
        instance.setPreparator(preparator);
        instance.setReviewer(reviewer);
        instance.setInstructions(instructions);
        
        LocalDateTime now = LocalDateTime.now();
        log.debug("Current time: {}, Scheduled start: {}", now, scheduledStart);
        
        // DECISION POINT: Start immediately or schedule for later?
        if (scheduledStart.isBefore(now) || scheduledStart.isEqual(now)) {
            log.info("Scheduled time has passed - Starting workflow immediately");
            instance.setStatus("ACTIVE");
            instance.setActualStart(now);
            
            WorkflowInstance saved = workflowInstanceRepository.save(instance);
            log.info("Workflow instance created with ID: {}", saved.getId());
            
            // Create the first task (start) immediately
            createStartTask(saved);
            log.info("Workflow started successfully with upload task created");
            return saved;
        } else {
            log.info("Scheduled time is in future - Workflow will be scheduled");
            instance.setStatus("SCHEDULED");
            WorkflowInstance saved = workflowInstanceRepository.save(instance);
            log.info("Workflow scheduled with ID: {} - will start at {}", saved.getId(), scheduledStart);
            return saved;
        }
    }
    
    /**
     * Automatic scheduler that runs every minute to check for workflows ready to start
     * 
     * SCHEDULER LOGIC:
     * 1. Find all workflows with status=SCHEDULED and scheduledStart <= now
     * 2. For each workflow: Change status to ACTIVE and record actual start time
     * 3. Create the first upload task to begin the workflow
     * 4. Send notification to uploader about new task
     * 
     * This enables workflows to be created in advance and start automatically
     * at their designated times without manual intervention.
     */
    @Scheduled(fixedRate = 60000) // Runs every 60 seconds (1 minute)
    public void checkScheduledWorkflows() {
        log.debug("=== CHECKING SCHEDULED WORKFLOWS ===");
        LocalDateTime now = LocalDateTime.now();
        
        // Find workflows that are scheduled to start before or at current time
        List<WorkflowInstance> scheduled = workflowInstanceRepository
            .findByScheduledStartBeforeAndStatus(now, "SCHEDULED");
        
        if (scheduled.isEmpty()) {
            log.debug("No scheduled workflows ready to start at {}", now);
            return;
        }
        
        log.info("Found {} scheduled workflows ready to start", scheduled.size());
        
        // Process each scheduled workflow
        for (WorkflowInstance instance : scheduled) {
            log.info("Starting scheduled workflow: '{}' (ID: {})", instance.getWorkflowName(), instance.getId());
            log.debug("Original scheduled time: {}, Starting now at: {}", instance.getScheduledStart(), now);
            
            // Activate the workflow
            instance.setStatus("ACTIVE");
            instance.setActualStart(now);
            workflowInstanceRepository.save(instance);
            
            // Create the first start task
            createStartTask(instance);
            
            log.info("Successfully activated workflow '{}' - upload task created for user {}", 
                    instance.getWorkflowName(), instance.getUploader());
        }
        
        log.info("Completed processing {} scheduled workflows", scheduled.size());
    }
    
    /**
     * Creates the START task for a workflow - this is always the first task
     * 
     * START TASK PURPOSE:
     * - Marks the beginning of the workflow
     * - Can be started manually or automatically at scheduled time
     * - Once completed, triggers UPLOAD task creation
     * 
     * @param instance The workflow instance to create start task for
     */
    private void createStartTask(WorkflowInstance instance) {
        log.info("--- Creating START task ---");
        log.info("Workflow: '{}' (ID: {})", instance.getWorkflowName(), instance.getId());
        
        // Create start task
        WorkflowTask startTask = new WorkflowTask();
        startTask.setTaskName("START");
        startTask.setAssignee(instance.getStartedBy()); // Admin who created workflow
        startTask.setStatus("COMPLETED"); // Auto-complete start task
        startTask.setWorkflowInstanceId(instance.getId());
        startTask.setInstructions("Workflow started");
        startTask.setCreatedAt(LocalDateTime.now());
        startTask.setCompletedAt(LocalDateTime.now());
        startTask.setStartDate(instance.getScheduledStart());
        startTask.setEndDate(LocalDateTime.now());
        
        workflowTaskRepository.save(startTask);
        log.info("START task auto-completed");
        
        // Immediately create UPLOAD task
        createUploadTask(instance);
    }
    
    /**
     * Creates the UPLOAD task after START task completion
     * 
     * UPLOAD TASK PURPOSE:
     * - User uploads the original Excel document that needs to be processed
     * - This task must be completed before any preparation can begin
     * - File is stored on server and path is recorded in task
     * 
     * @param instance The workflow instance to create upload task for
     */
    private void createUploadTask(WorkflowInstance instance) {
        log.info("--- Creating UPLOAD task ---");
        log.info("Workflow: '{}' (ID: {})", instance.getWorkflowName(), instance.getId());
        log.info("Assigned to uploader: {}", instance.getUploader());
        
        // Create upload task with all required information
        WorkflowTask uploadTask = new WorkflowTask();
        uploadTask.setTaskName("UPLOAD");
        uploadTask.setAssignee(instance.getUploader());
        uploadTask.setStatus("PENDING");
        uploadTask.setWorkflowInstanceId(instance.getId());
        uploadTask.setInstructions(instance.getInstructions());
        uploadTask.setCreatedAt(LocalDateTime.now());
        uploadTask.setStartDate(LocalDateTime.now());
        uploadTask.setEndDate(LocalDateTime.now().plusDays(1)); // 1 day to complete
        
        // Save task to database
        WorkflowTask savedTask = workflowTaskRepository.save(uploadTask);
        log.info("Upload task created successfully with ID: {}", savedTask.getId());
        
        // Notify the assigned uploader
        String notificationMessage = "New upload task assigned: " + instance.getWorkflowName();
        sendNotification(instance.getUploader(), notificationMessage, "INFO");
        log.info("Notification sent to uploader: {}", instance.getUploader());
        
        log.debug("Upload task creation completed - waiting for user to upload file");
    }
    
    /**
     * Completes an upload task when user successfully uploads a file
     * 
     * UPLOAD COMPLETION PROCESS:
     * 1. Mark upload task as COMPLETED and record file path
     * 2. Record completion timestamp for audit trail
     * 3. Automatically trigger creation of PREPARE task for next stage
     * 4. Notify preparator that their task is ready
     * 
     * This method is called from the web controller when file upload succeeds.
     * 
     * @param taskId ID of the upload task being completed
     * @param filePath Server path where the uploaded file was saved
     * @param userComments Comments added by the uploader
     */
    public void completeUploadTask(Long taskId, String filePath, String userComments) {
        log.info("=== COMPLETING UPLOAD TASK ===");
        log.info("Task ID: {}", taskId);
        log.info("File uploaded to: {}", filePath);
        
        // Find and validate the upload task
        WorkflowTask task = workflowTaskRepository.findById(taskId)
            .orElseThrow(() -> {
                log.error("Upload task not found with ID: {}", taskId);
                return new RuntimeException("Upload task not found: " + taskId);
            });
        
        log.info("Found upload task for workflow: {}, assigned to: {}", 
                task.getWorkflowInstanceId(), task.getAssignee());
        
        // Update task with completion details
        task.setOriginalFilePath(filePath);
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        task.setUserComments(userComments);
        workflowTaskRepository.save(task);
        
        log.info("Upload task marked as COMPLETED at {}", task.getCompletedAt());
        
        // Get the parent workflow instance
        WorkflowInstance instance = workflowInstanceRepository.findById(task.getWorkflowInstanceId())
            .orElseThrow(() -> {
                log.error("Workflow instance not found with ID: {}", task.getWorkflowInstanceId());
                return new RuntimeException("Workflow instance not found: " + task.getWorkflowInstanceId());
            });
        
        log.info("Retrieved workflow instance: '{}'", instance.getWorkflowName());
        
        // WORKFLOW PROGRESSION: Upload completed -> Create prepare task
        log.info("Upload stage completed - proceeding to PREPARE stage");
        createPrepareTask(instance, filePath);
        
        log.info("Upload task completion process finished successfully");
    }
    
    /**
     * Creates a PREPARE task after upload completion - second stage of workflow
     * 
     * PREPARE TASK PURPOSE:
     * - Preparator downloads the original uploaded file
     * - Processes/formats the document according to requirements
     * - Uploads the prepared version back to the system
     * - Both original and prepared files will be available for review
     * 
     * @param instance The workflow instance to create prepare task for
     * @param originalFile Path to the original uploaded file
     */
    private void createPrepareTask(WorkflowInstance instance, String originalFile) {
        log.info("--- Creating PREPARE task ---");
        log.info("Workflow: '{}' (ID: {})", instance.getWorkflowName(), instance.getId());
        log.info("Assigned to preparator: {}", instance.getPreparator());
        log.info("Original file to process: {}", originalFile);
        
        // Create preparation task with reference to original file
        WorkflowTask prepareTask = new WorkflowTask();
        prepareTask.setTaskName("PREPARE");
        prepareTask.setAssignee(instance.getPreparator());
        prepareTask.setStatus("PENDING");
        prepareTask.setWorkflowInstanceId(instance.getId());
        prepareTask.setOriginalFilePath(originalFile); // Reference to file to be processed
        prepareTask.setInstructions(instance.getInstructions());
        prepareTask.setCreatedAt(LocalDateTime.now());
        prepareTask.setStartDate(LocalDateTime.now());
        prepareTask.setEndDate(LocalDateTime.now().plusDays(2)); // 2 days to complete
        
        // Save task to database
        WorkflowTask savedTask = workflowTaskRepository.save(prepareTask);
        log.info("Prepare task created successfully with ID: {}", savedTask.getId());
        
        // Notify the assigned preparator
        String notificationMessage = "New preparation task assigned for: " + instance.getWorkflowName();
        sendNotification(instance.getPreparator(), notificationMessage, "INFO");
        log.info("Notification sent to preparator: {}", instance.getPreparator());
        
        log.debug("Prepare task creation completed - waiting for user to process and upload prepared file");
    }
    
    /**
     * Completes a prepare task when user uploads the processed file
     * 
     * PREPARE COMPLETION PROCESS:
     * 1. Mark prepare task as COMPLETED and record prepared file path
     * 2. Record completion timestamp for audit trail
     * 3. Automatically trigger creation of REVIEW task for final stage
     * 4. Notify reviewer that both files are ready for comparison
     * 
     * @param taskId ID of the prepare task being completed
     * @param preparedFile Server path where the prepared file was saved
     * @param userComments Comments added by the preparator
     */
    public void completePrepareTask(Long taskId, String preparedFile, String userComments) {
        log.info("=== COMPLETING PREPARE TASK ===");
        log.info("Task ID: {}", taskId);
        log.info("Prepared file uploaded to: {}", preparedFile);
        
        // Find and validate the prepare task
        WorkflowTask task = workflowTaskRepository.findById(taskId)
            .orElseThrow(() -> {
                log.error("Prepare task not found with ID: {}", taskId);
                return new RuntimeException("Prepare task not found: " + taskId);
            });
        
        log.info("Found prepare task for workflow: {}, assigned to: {}", 
                task.getWorkflowInstanceId(), task.getAssignee());
        log.info("Original file was: {}", task.getOriginalFilePath());
        
        // Update task with completion details
        task.setPreparedFilePath(preparedFile);
        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        task.setUserComments(userComments);
        workflowTaskRepository.save(task);
        
        log.info("Prepare task marked as COMPLETED at {}", task.getCompletedAt());
        
        // Get the parent workflow instance
        WorkflowInstance instance = workflowInstanceRepository.findById(task.getWorkflowInstanceId())
            .orElseThrow(() -> {
                log.error("Workflow instance not found with ID: {}", task.getWorkflowInstanceId());
                return new RuntimeException("Workflow instance not found: " + task.getWorkflowInstanceId());
            });
        
        log.info("Retrieved workflow instance: '{}'", instance.getWorkflowName());
        
        // WORKFLOW PROGRESSION: Prepare completed -> Create review task
        log.info("Prepare stage completed - proceeding to REVIEW stage");
        createReviewTask(instance, task.getOriginalFilePath(), preparedFile);
        
        log.info("Prepare task completion process finished successfully");
    }
    
    /**
     * Creates a REVIEW task after prepare completion - final stage of workflow
     * 
     * REVIEW TASK PURPOSE:
     * - Reviewer compares original and prepared files
     * - Makes decision to APPROVE or REJECT the prepared document
     * - If approved: Workflow completes successfully
     * - If rejected: Creates new prepare task for rework
     * 
     * SECURITY CHECK: Verifies upload task was completed to prevent review without upload
     * 
     * @param instance The workflow instance to create review task for
     * @param originalFile Path to the original uploaded file
     * @param preparedFile Path to the prepared/processed file
     */
    private void createReviewTask(WorkflowInstance instance, String originalFile, String preparedFile) {
        log.info("--- Creating REVIEW task ---");
        log.info("Workflow: '{}' (ID: {})", instance.getWorkflowName(), instance.getId());
        log.info("Assigned to reviewer: {}", instance.getReviewer());
        log.info("Files for review - Original: {}, Prepared: {}", originalFile, preparedFile);
        
        // SECURITY VALIDATION: Ensure upload task was actually completed
        // This prevents review tasks from being created without proper upload
        log.debug("Validating that upload task was completed before creating review task");
        List<WorkflowTask> uploadTasks = workflowTaskRepository.findByWorkflowInstanceId(instance.getId())
            .stream()
            .filter(t -> "UPLOAD".equals(t.getTaskName()) && "COMPLETED".equals(t.getStatus()))
            .toList();
        
        if (uploadTasks.isEmpty()) {
            log.error("SECURITY VIOLATION: Attempt to create review task without completed upload task");
            log.error("Workflow ID: {}, Instance: '{}'", instance.getId(), instance.getWorkflowName());
            throw new RuntimeException("Cannot create review task: Upload task not completed");
        }
        
        log.info("Upload task validation passed - {} completed upload tasks found", uploadTasks.size());
        
        // Create review task with both file references
        WorkflowTask reviewTask = new WorkflowTask();
        reviewTask.setTaskName("REVIEW");
        reviewTask.setAssignee(instance.getReviewer());
        reviewTask.setStatus("PENDING");
        reviewTask.setWorkflowInstanceId(instance.getId());
        reviewTask.setOriginalFilePath(originalFile);
        reviewTask.setPreparedFilePath(preparedFile);
        reviewTask.setInstructions(instance.getInstructions());
        reviewTask.setCreatedAt(LocalDateTime.now());
        reviewTask.setStartDate(LocalDateTime.now());
        reviewTask.setEndDate(LocalDateTime.now().plusDays(1)); // 1 day to complete
        
        // Save task to database
        WorkflowTask savedTask = workflowTaskRepository.save(reviewTask);
        log.info("Review task created successfully with ID: {}", savedTask.getId());
        
        // Notify the assigned reviewer
        String notificationMessage = "New review task assigned for: " + instance.getWorkflowName();
        sendNotification(instance.getReviewer(), notificationMessage, "INFO");
        log.info("Notification sent to reviewer: {}", instance.getReviewer());
        
        log.debug("Review task creation completed - waiting for reviewer decision");
    }
    
    /**
     * Completes a review task with either approval or rejection decision
     * 
     * REVIEW COMPLETION PROCESS:
     * - If APPROVED: Mark workflow as COMPLETED and notify all participants
     * - If REJECTED: Create new PREPARE task for rework and notify preparator
     * 
     * APPROVAL PATH: Workflow ends successfully
     * REJECTION PATH: Workflow loops back to prepare stage for corrections
     * 
     * @param taskId ID of the review task being completed
     * @param decision Either "APPROVED" or "REJECTED"
     * @param message Reviewer's feedback/comments
     */
    public void completeReviewTask(Long taskId, String decision, String message) {
        log.info("=== COMPLETING REVIEW TASK ===");
        log.info("Task ID: {}", taskId);
        log.info("Review Decision: {}", decision);
        log.info("Reviewer Message: {}", message);
        
        // Find and validate the review task
        WorkflowTask task = workflowTaskRepository.findById(taskId)
            .orElseThrow(() -> {
                log.error("Review task not found with ID: {}", taskId);
                return new RuntimeException("Review task not found: " + taskId);
            });
        
        // Get the parent workflow instance
        WorkflowInstance instance = workflowInstanceRepository.findById(task.getWorkflowInstanceId())
            .orElseThrow(() -> {
                log.error("Workflow instance not found with ID: {}", task.getWorkflowInstanceId());
                return new RuntimeException("Workflow instance not found: " + task.getWorkflowInstanceId());
            });
        
        log.info("Processing review for workflow: '{}' by reviewer: {}", 
                instance.getWorkflowName(), task.getAssignee());
        
        // DECISION BRANCH: Handle approval vs rejection
        if ("APPROVED".equals(decision)) {
            log.info("*** DOCUMENT APPROVED - COMPLETING WORKFLOW ***");
            
            // Mark review task as completed
            task.setStatus("COMPLETED");
            task.setReviewerMessage(message);
            task.setUserComments(message); // Reviewer comments
            task.setCompletedAt(LocalDateTime.now());
            workflowTaskRepository.save(task);
            
            // Create END task to complete workflow
            createEndTask(instance);
            
        } else {
            log.warn("*** DOCUMENT REJECTED - SENDING FOR REWORK ***");
            log.warn("Rejection reason: {}", message);
            
            // Mark review task as rejected
            task.setStatus("REJECTED");
            task.setReviewerMessage(message);
            task.setUserComments(message); // Reviewer comments
            task.setCompletedAt(LocalDateTime.now());
            workflowTaskRepository.save(task);
            
            // WORKFLOW LOOP: Create new prepare task for rework
            log.info("Creating new PREPARE task for rework");
            createPrepareTask(instance, task.getOriginalFilePath());
            
            // Notify preparator about rejection with specific feedback
            String rejectionMessage = "Document rejected for: " + instance.getWorkflowName() + 
                                    ". Reviewer feedback: " + message;
            sendNotification(instance.getPreparator(), rejectionMessage, "ERROR");
            
            log.info("Rejection notification sent to preparator with feedback");
        }
        
        log.info("Review task completion process finished");
    }
    
    /**
     * Creates the END task to complete workflow automatically
     * 
     * END TASK PURPOSE:
     * - Marks the completion of the workflow
     * - Automatically completed when review is approved
     * - Sends notifications to all participants
     * 
     * @param instance The workflow instance to create end task for
     */
    private void createEndTask(WorkflowInstance instance) {
        log.info("--- Creating END task ---");
        log.info("Workflow: '{}' (ID: {})", instance.getWorkflowName(), instance.getId());
        
        // Create end task
        WorkflowTask endTask = new WorkflowTask();
        endTask.setTaskName("END");
        endTask.setAssignee(instance.getStartedBy()); // Admin who created workflow
        endTask.setStatus("COMPLETED"); // Auto-complete end task
        endTask.setWorkflowInstanceId(instance.getId());
        endTask.setInstructions("Workflow completed successfully");
        endTask.setCreatedAt(LocalDateTime.now());
        endTask.setCompletedAt(LocalDateTime.now());
        endTask.setStartDate(LocalDateTime.now());
        endTask.setEndDate(LocalDateTime.now());
        
        workflowTaskRepository.save(endTask);
        log.info("END task auto-completed");
        
        // Mark entire workflow as completed
        instance.setStatus("COMPLETED");
        instance.setEndTime(LocalDateTime.now());
        workflowInstanceRepository.save(instance);
        
        log.info("Workflow '{}' completed successfully at {}", 
                instance.getWorkflowName(), instance.getEndTime());
        
        // Notify all participants of successful completion
        String successMessage = "Workflow '" + instance.getWorkflowName() + "' completed successfully!";
        sendNotification(instance.getUploader(), successMessage, "SUCCESS");
        sendNotification(instance.getPreparator(), successMessage, "SUCCESS");
        sendNotification(instance.getReviewer(), successMessage, "SUCCESS");
        
        log.info("Success notifications sent to all participants");
    }
    
    /**
     * Sends a notification to a specific user
     * 
     * NOTIFICATION SYSTEM:
     * - Creates database record for user notifications
     * - Notifications appear in user's dashboard dropdown
     * - Types: INFO (blue), SUCCESS (green), ERROR (red)
     * - All notifications start as unread
     * 
     * @param userId Target user ID to receive notification
     * @param message Notification message content
     * @param type Notification type for styling (INFO, SUCCESS, ERROR)
     */
    private void sendNotification(String userId, String message, String type) {
        log.debug("Sending {} notification to user {}: {}", type, userId, message);
        
        // Create notification record
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRead(false); // All notifications start as unread
        notification.setCreatedAt(LocalDateTime.now());
        
        // Save to database for display in user dashboard
        Notification saved = notificationRepository.save(notification);
        log.debug("Notification saved with ID: {} for user: {}", saved.getId(), userId);
    }
    
    /**
     * Retrieves unread notifications for a specific user
     * Used by dashboard to show notification dropdown
     * 
     * @param userId User ID to get notifications for
     * @return List of unread notifications for the user
     */
    public List<Notification> getNotifications(String userId) {
        log.debug("Retrieving unread notifications for user: {}", userId);
        List<Notification> notifications = notificationRepository.findByUserIdAndReadFalse(userId);
        log.debug("Found {} unread notifications for user: {}", notifications.size(), userId);
        return notifications;
    }
    
    /**
     * Retrieves all workflow instances in the system
     * Used by admin dashboard to show complete workflow overview
     * 
     * @return List of all workflow instances
     */
    public List<WorkflowInstance> getAllWorkflowInstances() {
        log.debug("Retrieving all workflow instances for admin dashboard");
        List<WorkflowInstance> instances = workflowInstanceRepository.findAll();
        log.debug("Found {} workflow instances", instances.size());
        return instances;
    }
    
    /**
     * Retrieves all tasks for a specific workflow
     * Used by admin dashboard to show task details within workflows
     * 
     * @param workflowId Workflow instance ID to get tasks for
     * @return List of tasks belonging to the workflow
     */
    public List<WorkflowTask> getTasksByWorkflowId(Long workflowId) {
        log.debug("Retrieving tasks for workflow ID: {}", workflowId);
        List<WorkflowTask> tasks = workflowTaskRepository.findByWorkflowInstanceId(workflowId);
        log.debug("Found {} tasks for workflow ID: {}", tasks.size(), workflowId);
        return tasks;
    }
    
    /**
     * Gets upcoming tasks for a user from scheduled workflows
     * 
     * @param userId User ID to get upcoming tasks for
     * @return List of upcoming workflow instances where user is assigned
     */
    public List<WorkflowInstance> getUpcomingTasksForUser(String userId) {
        log.debug("Getting upcoming tasks for user: {}", userId);
        List<WorkflowInstance> scheduled = workflowInstanceRepository
            .findAll()
            .stream()
            .filter(w -> "SCHEDULED".equals(w.getStatus()))
            .filter(w -> userId.equals(w.getUploader()) || userId.equals(w.getPreparator()) || userId.equals(w.getReviewer()))
            .collect(java.util.stream.Collectors.toList());
        
        log.debug("Found {} upcoming workflows for user: {}", scheduled.size(), userId);
        return scheduled;
    }
    
    /**
     * Gets active pending tasks for a user
     * 
     * @param userId User ID to get active tasks for
     * @return List of pending tasks assigned to user
     */
    public List<WorkflowTask> getActiveTasksForUser(String userId) {
        log.debug("Getting active tasks for user: {}", userId);
        List<WorkflowTask> activeTasks = workflowTaskRepository
            .findByAssignee(userId)
            .stream()
            .filter(t -> "PENDING".equals(t.getStatus()))
            .collect(java.util.stream.Collectors.toList());
        
        log.debug("Found {} active tasks for user: {}", activeTasks.size(), userId);
        return activeTasks;
    }
    
    /**
     * Gets active review tasks specifically for reviewers
     * Ensures only REVIEW tasks with both original and prepared files are shown
     * 
     * @param userId Reviewer user ID
     * @return List of pending review tasks ready for review
     */
    public List<WorkflowTask> getActiveReviewTasksForUser(String userId) {
        log.debug("Getting active review tasks for reviewer: {}", userId);
        List<WorkflowTask> reviewTasks = workflowTaskRepository
            .findByAssignee(userId)
            .stream()
            .filter(t -> "PENDING".equals(t.getStatus()))
            .filter(t -> "REVIEW".equals(t.getTaskName()))
            .filter(t -> t.getOriginalFilePath() != null && t.getPreparedFilePath() != null)
            .collect(java.util.stream.Collectors.toList());
        
        log.debug("Found {} active review tasks for reviewer: {}", reviewTasks.size(), userId);
        return reviewTasks;
    }
}