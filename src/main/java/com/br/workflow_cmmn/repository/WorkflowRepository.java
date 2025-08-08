package com.br.workflow_cmmn.repository;

import com.br.workflow_cmmn.model.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
}