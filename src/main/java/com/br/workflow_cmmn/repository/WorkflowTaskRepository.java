package com.br.workflow_cmmn.repository;

import com.br.workflow_cmmn.model.WorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, Long> {
    List<WorkflowTask> findByAssignee(String assignee);
    List<WorkflowTask> findByWorkflowInstanceId(Long workflowInstanceId);
}