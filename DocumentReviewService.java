package com.company.document;

import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DocumentReviewService {
    
    private final CmmnRuntimeService runtimeService;
    private final CmmnTaskService taskService;
    private final DocumentStorageService storageService;
    private final NotificationService notificationService;
    
    public DocumentReviewService(CmmnRuntimeService runtimeService,
                               CmmnTaskService taskService,
                               DocumentStorageService storageService,
                               NotificationService notificationService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.storageService = storageService;
        this.notificationService = notificationService;
    }
    
    public String startDocumentReview(DocumentSubmissionRequest request) {
        // Store document
        String documentId = storageService.storeDocument(request.getDocumentContent());
        
        // Prepare case variables
        Map<String, Object> variables = new HashMap<>();
        variables.put("documentId", documentId);
        variables.put("documentTitle", request.getTitle());
        variables.put("documentType", request.getType());
        variables.put("priority", request.getPriority());
        variables.put("submittedBy", request.getSubmittedBy());
        variables.put("submissionDate", LocalDateTime.now());
        variables.put("documentUrl", storageService.getDocumentUrl(documentId));
        
        // Start case
        String caseInstanceId = runtimeService.createCaseInstanceBuilder()
            .caseDefinitionKey("documentReview")
            .businessKey("DOC-" + documentId)
            .name("Review: " + request.getTitle())
            .variables(variables)
            .start()
            .getId();
        
        // Notify document coordinator
        notificationService.notifyDocumentSubmitted(caseInstanceId, request);
        
        return caseInstanceId;
    }
    
    public List<Map<String, Object>> getReviewTasks(String userId, List<String> userGroups) {
        return taskService.createTaskQuery()
            .caseDefinitionKey("documentReview")
            .or()
                .taskAssignee(userId)
                .taskCandidateGroupIn(userGroups)
            .endOr()
            .active()
            .list()
            .stream()
            .map(this::taskToMap)
            .toList();
    }
    
    public void completeReviewTask(String taskId, Map<String, Object> formData, String completedBy) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new TaskNotFoundException("Task not found: " + taskId);
        }
        
        // Add completion metadata
        formData.put("completedBy", completedBy);
        formData.put("completedDate", LocalDateTime.now());
        
        // Complete task
        taskService.complete(taskId, formData);
        
        // Send notifications based on task type
        handleTaskCompletion(task, formData);
    }
    
    public DocumentReviewStatus getReviewStatus(String caseInstanceId) {
        Map<String, Object> variables = runtimeService.getVariables(caseInstanceId);
        
        List<Task> activeTasks = taskService.createTaskQuery()
            .caseInstanceId(caseInstanceId)
            .active()
            .list();
        
        return DocumentReviewStatus.builder()
            .caseInstanceId(caseInstanceId)
            .documentTitle((String) variables.get("documentTitle"))
            .currentStage(getCurrentStage(activeTasks))
            .activeTasks(activeTasks.stream().map(Task::getName).toList())
            .progress(calculateProgress(variables, activeTasks))
            .build();
    }
    
    private void handleTaskCompletion(Task task, Map<String, Object> formData) {
        String taskName = task.getName();
        String caseInstanceId = task.getScopeId();
        
        switch (taskName) {
            case "Initial Review":
                handleInitialReviewCompletion(caseInstanceId, formData);
                break;
            case "Legal Review":
                handleLegalReviewCompletion(caseInstanceId, formData);
                break;
            case "Technical Review":
                handleTechnicalReviewCompletion(caseInstanceId, formData);
                break;
            case "Final Approval":
                handleFinalApprovalCompletion(caseInstanceId, formData);
                break;
        }
    }
    
    private void handleInitialReviewCompletion(String caseInstanceId, Map<String, Object> formData) {
        boolean recommendApproval = (Boolean) formData.getOrDefault("recommendApproval", false);
        
        if (!recommendApproval) {
            // Notify submitter of issues
            notificationService.notifyReviewIssues(caseInstanceId, formData);
        }
        
        // Notify specialized reviewers if needed
        if ((Boolean) formData.getOrDefault("requiresLegalReview", false)) {
            notificationService.notifyLegalReviewRequired(caseInstanceId);
        }
        if ((Boolean) formData.getOrDefault("requiresTechnicalReview", false)) {
            notificationService.notifyTechnicalReviewRequired(caseInstanceId);
        }
    }
    
    private void handleLegalReviewCompletion(String caseInstanceId, Map<String, Object> formData) {
        boolean legalApproval = (Boolean) formData.getOrDefault("legalApproval", false);
        String riskLevel = (String) formData.get("legalRiskLevel");
        
        if (!legalApproval || "high".equals(riskLevel)) {
            notificationService.notifyLegalIssues(caseInstanceId, formData);
        }
    }
    
    private void handleTechnicalReviewCompletion(String caseInstanceId, Map<String, Object> formData) {
        boolean technicalApproval = (Boolean) formData.getOrDefault("technicalApproval", false);
        
        if (!technicalApproval) {
            notificationService.notifyTechnicalIssues(caseInstanceId, formData);
        }
    }
    
    private void handleFinalApprovalCompletion(String caseInstanceId, Map<String, Object> formData) {
        String finalDecision = (String) formData.get("finalDecision");
        
        switch (finalDecision) {
            case "approve":
            case "approveWithConditions":
                notificationService.notifyDocumentApproved(caseInstanceId, formData);
                break;
            case "reject":
            case "returnForRevision":
                notificationService.notifyDocumentRejected(caseInstanceId, formData);
                break;
        }
    }
    
    private Map<String, Object> taskToMap(Task task) {
        return Map.of(
            "id", task.getId(),
            "name", task.getName(),
            "caseInstanceId", task.getScopeId(),
            "createTime", task.getCreateTime(),
            "dueDate", task.getDueDate() != null ? task.getDueDate() : "",
            "priority", task.getPriority()
        );
    }
    
    private String getCurrentStage(List<Task> activeTasks) {
        if (activeTasks.isEmpty()) {
            return "Completed";
        }
        
        String firstTaskName = activeTasks.get(0).getName();
        if (firstTaskName.contains("Upload") || firstTaskName.contains("Validate") || firstTaskName.contains("Classify")) {
            return "Document Intake";
        } else if (firstTaskName.contains("Initial Review")) {
            return "Initial Review";
        } else if (firstTaskName.contains("Legal") || firstTaskName.contains("Technical") || firstTaskName.contains("Compliance")) {
            return "Specialized Review";
        } else if (firstTaskName.contains("Final Approval")) {
            return "Final Approval";
        } else {
            return "Processing";
        }
    }
    
    private int calculateProgress(Map<String, Object> variables, List<Task> activeTasks) {
        if (activeTasks.isEmpty()) {
            return 100;
        }
        
        int totalSteps = 5; // Intake, Initial, Specialized, Final, Publish
        int completedSteps = 0;
        
        if (variables.containsKey("intakeComplete")) completedSteps++;
        if (variables.containsKey("initialReviewComplete")) completedSteps++;
        if (variables.containsKey("specializedReviewComplete")) completedSteps++;
        if (variables.containsKey("finalApprovalComplete")) completedSteps++;
        
        return (completedSteps * 100) / totalSteps;
    }
}

// Supporting classes
@Data
@Builder
class DocumentSubmissionRequest {
    private String title;
    private String type;
    private String priority;
    private String submittedBy;
    private byte[] documentContent;
}

@Data
@Builder
class DocumentReviewStatus {
    private String caseInstanceId;
    private String documentTitle;
    private String currentStage;
    private List<String> activeTasks;
    private int progress;
}

class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String message) {
        super(message);
    }
}