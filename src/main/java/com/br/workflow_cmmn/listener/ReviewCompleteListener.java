package com.br.workflow_cmmn.listener;

import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component
public class ReviewCompleteListener implements TaskListener {
    
    @Override
    public void notify(DelegateTask delegateTask) {
        // Set review completion timestamp
        delegateTask.setVariable("reviewCompletedAt", System.currentTimeMillis());
        
        // Get review decision from task variables
        String decision = (String) delegateTask.getVariable("reviewDecision");
        
        // Log review completion
        System.out.println("Review completed for task: " + delegateTask.getName() + 
                          " with decision: " + decision);
    }
}