package com.br.workflow_cmmn.config;

import com.br.workflow_cmmn.model.Document;
import com.br.workflow_cmmn.model.Workflow;
import com.br.workflow_cmmn.repository.DocumentRepository;
import com.br.workflow_cmmn.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final WorkflowRepository workflowRepository;
    private final DocumentRepository documentRepository;

    @Override
    public void run(String... args) {
        loadDummyWorkflows();
        loadDummyDocuments();
    }

    private void loadDummyWorkflows() {
        if (workflowRepository.count() == 0) {
            String[] workflows = {
                "Document Review Workflow", "Invoice Approval Process", 
                "Employee Onboarding", "Purchase Order Approval", "Leave Request Process"
            };
            
            for (int i = 0; i < workflows.length; i++) {
                Workflow workflow = new Workflow();
                workflow.setName(workflows[i]);
                workflow.setDescription("Sample " + workflows[i].toLowerCase());
                workflow.setCreatedBy(String.valueOf(i + 1));
                workflow.setStatus("ACTIVE");
                workflow.setCreatedAt(LocalDateTime.now().minusDays(i));
                workflow.setWorkflowDefinition("<?xml version=\"1.0\" encoding=\"UTF-8\"?><definitions xmlns=\"http://www.omg.org/spec/CMMN/20151109/MODEL\"><case id=\"workflow" + (i+1) + "\" name=\"" + workflows[i] + "\"><casePlanModel><humanTask id=\"task1\" name=\"Review Task\"/></casePlanModel></case></definitions>");
                workflowRepository.save(workflow);
            }
        }
    }

    private void loadDummyDocuments() {
        if (documentRepository.count() == 0) {
            String[] titles = {"Project Proposal", "Budget Report", "Technical Specification", "User Manual", "Test Results"};
            String[] statuses = {"UPLOADED", "UNDER_REVIEW", "APPROVED", "REJECTED", "UPLOADED"};
            
            for (int i = 0; i < titles.length; i++) {
                Document document = new Document();
                document.setTitle(titles[i]);
                document.setFileName(titles[i].toLowerCase().replace(" ", "_") + ".pdf");
                document.setUploadedBy(String.valueOf((i % 3) + 3)); // Users 3, 4, 5
                document.setAssignedReviewer(String.valueOf((i % 2) + 2)); // Users 2, 3
                document.setStatus(statuses[i]);
                document.setUploadedAt(LocalDateTime.now().minusDays(i + 1));
                if (!statuses[i].equals("UPLOADED")) {
                    document.setReviewedAt(LocalDateTime.now().minusDays(i));
                    document.setComments("Sample review comment for " + titles[i]);
                }
                documentRepository.save(document);
            }
        }
    }
}