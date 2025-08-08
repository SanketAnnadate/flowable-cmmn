package com.br.workflow_cmmn.repository;

import com.br.workflow_cmmn.model.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, Long> {
    List<WorkflowInstance> findByScheduledStartBeforeAndStatus(LocalDateTime dateTime, String status);
}