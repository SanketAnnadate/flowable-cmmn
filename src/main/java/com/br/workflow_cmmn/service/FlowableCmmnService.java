package com.br.workflow_cmmn.service;

import com.br.workflow_cmmn.listener.WorkflowEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FlowableCmmnService {
    
    private final CmmnRuntimeService cmmnRuntimeService;
    private final CmmnTaskService cmmnTaskService;
    
    public CaseInstance startWorkflow(String workflowName, String startedBy, String uploader, 
                                    String preparator, String reviewer, String instructions) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("workflowName", workflowName);
            variables.put("startedBy", startedBy);
            variables.put("uploader", uploader);
            variables.put("preparator", preparator);
            variables.put("reviewer", reviewer);
            variables.put("instructions", instructions);
            variables.put("startTime", LocalDateTime.now());
            variables.put("approved", false);
            variables.put("needsRework", false);
            variables.put("status", "ACTIVE");
            
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("documentReviewCase")
                .name(workflowName)
                .variables(variables)
                .start();
                
            log.info("Started CMMN case: {} with ID: {}", workflowName, caseInstance.getId());
            return caseInstance;
        } catch (FlowableException e) {
            log.error("Failed to start workflow: {}", workflowName, e);
            throw new RuntimeException("Workflow creation failed: " + e.getMessage(), e);
        }
    }
    

    
    @Transactional(readOnly = true)
    public List<Task> getTasksForUser(String userId) {
        return cmmnTaskService.createTaskQuery()
            .taskAssignee(userId)
            .orderByTaskCreateTime().desc()
            .list();
    }
    
    @Transactional(readOnly = true)
    public List<Task> getPendingTasksForUser(String userId) {
        return cmmnTaskService.createTaskQuery()
            .taskAssignee(userId)
            .orderByTaskCreateTime().desc()
            .list();
    }
    
    @Transactional(readOnly = true)
    public List<Task> getActiveTasks() {
        return cmmnTaskService.createTaskQuery()
            .orderByTaskCreateTime().desc()
            .list();
    }
    
    @Transactional(readOnly = true)
    public List<Task> getTasksByCaseInstance(String caseInstanceId) {
        return cmmnTaskService.createTaskQuery()
            .caseInstanceId(caseInstanceId)
            .orderByTaskCreateTime().desc()
            .list();
    }
    
    @Transactional(readOnly = true)
    public List<CaseInstance> getAllCaseInstances() {
        return cmmnRuntimeService.createCaseInstanceQuery()
            .orderByStartTime().desc()
            .list();
    }
    
    @Transactional(readOnly = true)
    public Task getTaskById(String taskId) {
        return cmmnTaskService.createTaskQuery()
            .taskId(taskId)
            .singleResult();
    }
    
    public void completeUploadTask(String taskId, String filePath, String comments) {
        Task task = validateTaskExists(taskId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("originalFilePath", filePath);
        variables.put("uploadComments", comments);
        variables.put("uploadTime", LocalDateTime.now());
        variables.put("uploadCompleted", true);
        
        cmmnTaskService.complete(taskId, variables);
        log.info("Completed upload task: {} for case: {}", taskId, task.getScopeId());
    }
    
    public void completePrepareTask(String taskId, String preparedFilePath, String comments) {
        Task task = validateTaskExists(taskId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("preparedFilePath", preparedFilePath);
        variables.put("prepareComments", comments);
        variables.put("prepareTime", LocalDateTime.now());
        variables.put("prepareCompleted", true);
        
        cmmnTaskService.complete(taskId, variables);
        log.info("Completed prepare task: {} for case: {}", taskId, task.getScopeId());
    }
    
    public void completeReviewTask(String taskId, boolean approved, String comments) {
        Task task = validateTaskExists(taskId);
        
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        variables.put("reviewComments", comments);
        variables.put("reviewTime", LocalDateTime.now());
        variables.put("reviewCompleted", true);
        
        if (!approved) {
            // Check if this is final rejection or needs rework
            boolean needsRework = comments.toLowerCase().contains("rework") || 
                                comments.toLowerCase().contains("revise");
            variables.put("needsRework", needsRework);
            variables.put("status", needsRework ? "REWORK" : "REJECTED");
        } else {
            variables.put("needsRework", false);
            variables.put("status", "COMPLETED");
        }
        
        cmmnTaskService.complete(taskId, variables);
        log.info("Completed review task: {} for case: {} with decision: {}", 
                taskId, task.getScopeId(), approved ? "APPROVED" : "REJECTED");
    }
    
    @Transactional(readOnly = true)
    public List<CaseInstance> getActiveCaseInstances() {
        return cmmnRuntimeService.createCaseInstanceQuery()
                .orderByStartTime().desc()
                .list();
    }
    
    @Transactional(readOnly = true)
    public List<CaseInstance> getCaseInstancesByUser(String userId) {
        return cmmnRuntimeService.createCaseInstanceQuery()
                .variableValueEquals("startedBy", userId)
                .orderByStartTime().desc()
                .list();
    }
    
    @Transactional(readOnly = true)
    public CaseInstance getCaseInstance(String caseInstanceId) {
        return cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(caseInstanceId)
                .singleResult();
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getCaseVariables(String caseInstanceId) {
        return cmmnRuntimeService.getVariables(caseInstanceId);
    }
    
    public void terminateCase(String caseInstanceId) {
        try {
            cmmnRuntimeService.terminateCaseInstance(caseInstanceId);
            log.info("Terminated case instance: {}", caseInstanceId);
        } catch (Exception e) {
            log.error("Failed to terminate case: {}", caseInstanceId, e);
            throw new RuntimeException("Case termination failed", e);
        }
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> getWorkflowStatus(String caseInstanceId) {
        Map<String, Object> status = new HashMap<>();
        
        CaseInstance caseInstance = getCaseInstance(caseInstanceId);
        if (caseInstance == null) {
            status.put("error", "Case not found");
            return status;
        }
        
        Map<String, Object> variables = getCaseVariables(caseInstanceId);
        List<Task> tasks = getTasksByCaseInstance(caseInstanceId);
        
        status.put("caseId", caseInstanceId);
        status.put("name", caseInstance.getName());
        status.put("state", caseInstance.getState());
        status.put("variables", variables);
        status.put("activeTasks", tasks.size());
        
        return status;
    }
    
    private Task validateTaskExists(String taskId) {
        Task task = getTaskById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Task not found: " + taskId);
        }
        return task;
    }
}