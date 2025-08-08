package com.br.workflow_cmmn.service;

import com.br.workflow_cmmn.model.Workflow;
import com.br.workflow_cmmn.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final WorkflowRepository workflowRepository;
    
    public Workflow createWorkflow(String name, String description, String createdBy, String definition) {
        Workflow workflow = new Workflow();
        workflow.setName(name);
        workflow.setDescription(description);
        workflow.setCreatedBy(createdBy);
        workflow.setStatus("ACTIVE");
        workflow.setCreatedAt(LocalDateTime.now());
        workflow.setWorkflowDefinition(definition);
        return workflowRepository.save(workflow);
    }
    
    public List<Workflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }
}