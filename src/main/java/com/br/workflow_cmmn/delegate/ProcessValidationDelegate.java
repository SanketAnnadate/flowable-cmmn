package com.br.workflow_cmmn.delegate;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class ProcessValidationDelegate implements PlanItemJavaDelegate {
    
    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        String originalFile = (String) planItemInstance.getVariable("originalFilePath");
        String processedFile = (String) planItemInstance.getVariable("preparedFilePath");
        
        boolean isValid = originalFile != null && processedFile != null;
        
        planItemInstance.setVariable("processValidated", isValid);
        
        if (!isValid) {
            throw new RuntimeException("Processing validation failed. Missing files.");
        }
    }
}