package com.br.workflow_cmmn.listener;

import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

@Component
public class TaskCreationListener implements TaskListener {
    
    @Override
    public void notify(DelegateTask delegateTask) {
        // Set task creation timestamp
        delegateTask.setVariable("taskCreatedAt", System.currentTimeMillis());
        
        // Log task creation
        System.out.println("Task created: " + delegateTask.getName() + 
                          " assigned to: " + delegateTask.getAssignee());
    }
}