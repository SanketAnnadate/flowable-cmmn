package com.br.workflow_cmmn.delegate;

import com.br.workflow_cmmn.service.WorkflowExecutionService;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationDelegate implements PlanItemJavaDelegate {
    
    @Autowired
    private WorkflowExecutionService workflowExecutionService;
    
    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        String uploader = (String) planItemInstance.getVariable("uploader");
        String preparator = (String) planItemInstance.getVariable("preparator");
        String reviewer = (String) planItemInstance.getVariable("reviewer");
        
        // Send completion notifications
        if (uploader != null) {
            sendNotification(uploader, "Workflow completed successfully");
        }
        if (preparator != null) {
            sendNotification(preparator, "Workflow completed successfully");
        }
        if (reviewer != null) {
            sendNotification(reviewer, "Workflow completed successfully");
        }
    }
    
    private void sendNotification(String userId, String message) {
        // Use the existing notification service
        // This would be called through the service layer
    }
}