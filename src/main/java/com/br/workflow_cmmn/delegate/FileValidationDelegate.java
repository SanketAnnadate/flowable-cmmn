package com.br.workflow_cmmn.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;

import org.springframework.stereotype.Component;

/**
 * FileValidationDelegate - Flowable service task for validating uploaded files
 * 
 * This delegate is automatically called by the Flowable CMMN engine after a file upload
 * to ensure the uploaded file meets the required format specifications.
 * 
 * Validation Rules:
 * - File must exist (not null)
 * - File must have .xlsx or .xls extension (Excel formats only)
 * - File name must be valid
 * 
 * If validation fails, the workflow will be terminated with an error.
 * If validation passes, the workflow continues to the next stage.
 */
@Slf4j
@Component
public class FileValidationDelegate implements PlanItemJavaDelegate {
    

    
    /**
     * Main execution method called by Flowable engine
     * Validates the uploaded file format and sets validation result
     * 
     * @param planItemInstance - Current workflow execution context with variables
     */
    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        log.info("Starting file validation for workflow instance: {}", 
                planItemInstance.getCaseInstanceId());
        
        // Extract uploaded file name from workflow variables
        String fileName = (String) planItemInstance.getVariable("uploadedFileName");
        log.debug("Validating file: {}", fileName);
        
        // Perform validation checks
        boolean isValid = validateFileFormat(fileName);
        
        // Store validation result in workflow variables for later use
        planItemInstance.setVariable("fileValidated", isValid);
        planItemInstance.setVariable("validationTimestamp", System.currentTimeMillis());
        
        if (isValid) {
            log.info("File validation PASSED for: {}", fileName);
        } else {
            log.error("File validation FAILED for: {}. Invalid format detected.", fileName);
            // Throw exception to stop workflow execution
            throw new RuntimeException("Invalid file format: " + fileName + 
                ". Only Excel files (.xlsx, .xls) are allowed.");
        }
    }
    
    /**
     * Validates if the uploaded file has the correct format
     * 
     * @param fileName - Name of the uploaded file
     * @return true if file format is valid, false otherwise
     */
    private boolean validateFileFormat(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("File name is null or empty");
            return false;
        }
        
        // Check for Excel file extensions
        String lowerFileName = fileName.toLowerCase();
        boolean isExcelFile = lowerFileName.endsWith(".xlsx") || lowerFileName.endsWith(".xls");
        
        if (!isExcelFile) {
            log.warn("File {} does not have Excel extension (.xlsx or .xls)", fileName);
        }
        
        return isExcelFile;
    }
}