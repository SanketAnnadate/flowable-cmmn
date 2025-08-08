# Advanced CMMN Examples for Production

## 1. Customer Support Case Management

### Business Requirements
- Multi-channel support (email, chat, phone)
- Escalation based on priority and SLA
- Knowledge base integration
- Customer satisfaction tracking

### CMMN Model Features
```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/CMMN/20151109/MODEL"
             xmlns:flowable="http://flowable.org/cmmn"
             targetNamespace="http://www.flowable.org/casedef">

  <case id="customerSupport" name="Customer Support Case">
    <casePlanModel id="supportPlan" name="Support Plan" flowable:autoComplete="true">

      <!-- Initial Triage Stage -->
      <planItem id="triageStage" definitionRef="triageStageTask"/>
      
      <!-- Resolution Attempts -->
      <planItem id="level1Support" definitionRef="level1SupportTask">
        <entryCriterion sentryRef="triageComplete"/>
      </planItem>
      
      <planItem id="level2Support" definitionRef="level2SupportTask">
        <entryCriterion sentryRef="escalateToLevel2"/>
      </planItem>
      
      <planItem id="level3Support" definitionRef="level3SupportTask">
        <entryCriterion sentryRef="escalateToLevel3"/>
      </planItem>
      
      <!-- Customer Communication -->
      <planItem id="customerUpdate" definitionRef="customerUpdateTask">
        <entryCriterion sentryRef="needsCustomerUpdate"/>
      </planItem>
      
      <!-- SLA Monitoring -->
      <planItem id="slaWarning" definitionRef="slaWarningTask">
        <entryCriterion sentryRef="slaBreachWarning"/>
      </planItem>
      
      <!-- Resolution -->
      <planItem id="resolution" definitionRef="resolutionTask">
        <entryCriterion sentryRef="issueResolved"/>
      </planItem>
      
      <!-- Milestones -->
      <planItem id="caseClosedMilestone" definitionRef="caseClosed">
        <entryCriterion sentryRef="resolutionConfirmed"/>
      </planItem>

      <!-- Stage: Initial Triage -->
      <stage id="triageStageTask" name="Initial Triage">
        <planItem id="categorizeIssue" definitionRef="categorizeIssueTask"/>
        <planItem id="setPriority" definitionRef="setPriorityTask"/>
        <planItem id="checkKnowledgeBase" definitionRef="checkKnowledgeBaseTask"/>
        
        <humanTask id="categorizeIssueTask" name="Categorize Issue" 
                   flowable:candidateGroups="support-agents">
          <extensionElements>
            <flowable:formField id="category" name="Issue Category" type="enum">
              <flowable:value id="technical" name="Technical"/>
              <flowable:value id="billing" name="Billing"/>
              <flowable:value id="account" name="Account"/>
            </flowable:value>
            <flowable:formField id="subcategory" name="Subcategory" type="string"/>
          </extensionElements>
        </humanTask>
        
        <humanTask id="setPriorityTask" name="Set Priority" 
                   flowable:candidateGroups="support-agents">
          <extensionElements>
            <flowable:formField id="priority" name="Priority" type="enum">
              <flowable:value id="low" name="Low"/>
              <flowable:value id="medium" name="Medium"/>
              <flowable:value id="high" name="High"/>
              <flowable:value id="critical" name="Critical"/>
            </flowable:value>
            <flowable:formField id="slaHours" name="SLA Hours" type="long"/>
          </extensionElements>
        </humanTask>
        
        <serviceTask id="checkKnowledgeBaseTask" name="Check Knowledge Base"
                     flowable:class="com.company.support.CheckKnowledgeBaseDelegate">
          <extensionElements>
            <flowable:field name="category">
              <flowable:expression>${category}</flowable:expression>
            </flowable:field>
          </extensionElements>
        </serviceTask>
      </stage>

      <!-- Support Level Tasks -->
      <humanTask id="level1SupportTask" name="Level 1 Support" 
                 flowable:candidateGroups="level1-support">
        <extensionElements>
          <flowable:formField id="resolution" name="Resolution" type="string"/>
          <flowable:formField id="escalate" name="Escalate to Level 2" type="boolean"/>
          <flowable:formField id="resolved" name="Issue Resolved" type="boolean"/>
        </extensionElements>
      </humanTask>
      
      <humanTask id="level2SupportTask" name="Level 2 Support" 
                 flowable:candidateGroups="level2-support">
        <extensionElements>
          <flowable:formField id="resolution" name="Resolution" type="string"/>
          <flowable:formField id="escalate" name="Escalate to Level 3" type="boolean"/>
          <flowable:formField id="resolved" name="Issue Resolved" type="boolean"/>
        </extensionElements>
      </humanTask>
      
      <humanTask id="level3SupportTask" name="Level 3 Support" 
                 flowable:candidateGroups="level3-support">
        <extensionElements>
          <flowable:formField id="resolution" name="Resolution" type="string"/>
          <flowable:formField id="resolved" name="Issue Resolved" type="boolean"/>
        </extensionElements>
      </humanTask>

      <!-- Communication and Monitoring -->
      <serviceTask id="customerUpdateTask" name="Send Customer Update"
                   flowable:class="com.company.support.CustomerNotificationDelegate"/>
      
      <serviceTask id="slaWarningTask" name="SLA Breach Warning"
                   flowable:class="com.company.support.SlaWarningDelegate"/>
      
      <humanTask id="resolutionTask" name="Confirm Resolution" 
                 flowable:candidateGroups="support-agents">
        <extensionElements>
          <flowable:formField id="customerSatisfied" name="Customer Satisfied" type="boolean"/>
          <flowable:formField id="resolutionNotes" name="Resolution Notes" type="string"/>
        </extensionElements>
      </humanTask>

      <milestone id="caseClosed" name="Case Closed"/>

      <!-- Sentries (Business Rules) -->
      <sentry id="triageComplete">
        <planItemOnPart sourceRef="triageStage">
          <standardEvent>complete</standardEvent>
        </planItemOnPart>
      </sentry>
      
      <sentry id="escalateToLevel2">
        <planItemOnPart sourceRef="level1Support">
          <standardEvent>complete</standardEvent>
        </planItemOnPart>
        <ifPart>
          <condition>${escalate == true}</condition>
        </ifPart>
      </sentry>
      
      <sentry id="escalateToLevel3">
        <planItemOnPart sourceRef="level2Support">
          <standardEvent>complete</standardEvent>
        </planItemOnPart>
        <ifPart>
          <condition>${escalate == true}</condition>
        </ifPart>
      </sentry>
      
      <sentry id="needsCustomerUpdate">
        <planItemOnPart sourceRef="level1Support">
          <standardEvent>complete</standardEvent>
        </planItemOnPart>
        <planItemOnPart sourceRef="level2Support">
          <standardEvent>complete</standardEvent>
        </planItemOnPart>
        <ifPart>
          <condition>${escalate == true}</condition>
        </ifPart>
      </sentry>
      
      <sentry id="slaBreachWarning">
        <timerEventListener>
          <timerExpression>${slaWarningTime}</timerExpression>
        </timerEventListener>
      </sentry>
      
      <sentry id="issueResolved">
        <planItemOnPart sourceRef="level1Support">
          <standardEvent>complete</standardEvent>
        </planItemOnPart>
        <planItemOnPart sourceRef="level2Support">
          <standardEvent>complete</standardEvent>
        </planItemOnPart>
        <planItemOnPart sourceRef="level3Support">
          <standardEvent>complete</standardEvent>
        </planItemOnPart>
        <ifPart>
          <condition>${resolved == true}</condition>
        </ifPart>
      </sentry>
      
      <sentry id="resolutionConfirmed">
        <planItemOnPart sourceRef="resolution">
          <standardEvent>complete</standardEvent>
        </planItemOnPart>
        <ifPart>
          <condition>${customerSatisfied == true}</condition>
        </ifPart>
      </sentry>

    </casePlanModel>
  </case>
</definitions>
```

## 2. Insurance Claim Processing (Advanced)

### Business Requirements
- Multi-step approval workflow
- Fraud detection integration
- Document management
- Regulatory compliance
- Payment processing

```java
// Service Delegates for Integration
@Component
public class FraudDetectionDelegate implements JavaDelegate {
    
    private final FraudDetectionService fraudService;
    
    @Override
    public void execute(DelegateExecution execution) {
        String claimId = (String) execution.getVariable("claimId");
        Long claimAmount = (Long) execution.getVariable("claimAmount");
        String customerHistory = (String) execution.getVariable("customerHistory");
        
        FraudRiskScore riskScore = fraudService.assessRisk(
            claimId, claimAmount, customerHistory);
        
        execution.setVariable("fraudRiskScore", riskScore.getScore());
        execution.setVariable("fraudRiskLevel", riskScore.getLevel());
        execution.setVariable("requiresInvestigation", riskScore.getScore() > 70);
    }
}

@Component
public class DocumentValidationDelegate implements JavaDelegate {
    
    private final DocumentService documentService;
    
    @Override
    public void execute(DelegateExecution execution) {
        String caseInstanceId = execution.getProcessInstanceId();
        List<String> documentIds = (List<String>) execution.getVariable("documentIds");
        
        DocumentValidationResult result = documentService.validateDocuments(
            caseInstanceId, documentIds);
        
        execution.setVariable("documentsValid", result.isValid());
        execution.setVariable("missingDocuments", result.getMissingDocuments());
        execution.setVariable("invalidDocuments", result.getInvalidDocuments());
    }
}
```

## 3. Project Management Case

### Features
- Dynamic task creation
- Resource allocation
- Milestone tracking
- Risk management
- Stakeholder communication

```java
// Dynamic Task Creation Service
@Service
public class ProjectTaskService {
    
    private final CmmnRuntimeService runtimeService;
    private final CmmnTaskService taskService;
    
    public void createProjectTask(String caseInstanceId, CreateTaskRequest request) {
        // Validate project phase
        CaseInstance caseInstance = runtimeService.createCaseInstanceQuery()
            .caseInstanceId(caseInstanceId)
            .singleResult();
        
        if (caseInstance == null) {
            throw new CaseNotFoundException("Case not found: " + caseInstanceId);
        }
        
        // Create dynamic task
        PlanItemInstance taskInstance = runtimeService.createPlanItemInstanceBuilder()
            .caseInstanceId(caseInstanceId)
            .planItemDefinitionId("dynamicProjectTask")
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK)
            .name(request.getTaskName())
            .variable("taskType", request.getTaskType())
            .variable("estimatedHours", request.getEstimatedHours())
            .variable("assignedTo", request.getAssignedTo())
            .variable("dueDate", request.getDueDate())
            .create();
        
        // Set task properties
        Task task = taskService.createTaskQuery()
            .planItemInstanceId(taskInstance.getId())
            .singleResult();
        
        if (task != null) {
            taskService.setAssignee(task.getId(), request.getAssignedTo());
            taskService.setDueDate(task.getId(), request.getDueDate());
            taskService.setPriority(task.getId(), request.getPriority());
        }
        
        // Notify stakeholders
        notifyTaskCreated(caseInstanceId, taskInstance.getId(), request);
    }
    
    public void updateProjectMilestone(String caseInstanceId, String milestoneId, 
                                     MilestoneUpdate update) {
        // Update milestone variables
        runtimeService.setVariables(caseInstanceId, Map.of(
            milestoneId + "_status", update.getStatus(),
            milestoneId + "_completionDate", update.getCompletionDate(),
            milestoneId + "_notes", update.getNotes()
        ));
        
        // Trigger milestone evaluation
        runtimeService.evaluateActivePlanItemInstances(caseInstanceId);
    }
}
```

## 4. Medical Treatment Case

### Features
- Treatment plan management
- Medication tracking
- Appointment scheduling
- Care team coordination
- Patient monitoring

```xml
<!-- Medical Treatment CMMN -->
<case id="medicalTreatment" name="Medical Treatment Case">
  <casePlanModel id="treatmentPlan" name="Treatment Plan" flowable:autoComplete="false">
    
    <!-- Initial Assessment -->
    <planItem id="initialAssessment" definitionRef="initialAssessmentTask"/>
    
    <!-- Treatment Planning -->
    <planItem id="treatmentPlanning" definitionRef="treatmentPlanningStage">
      <entryCriterion sentryRef="assessmentComplete"/>
    </planItem>
    
    <!-- Treatment Execution -->
    <planItem id="treatmentExecution" definitionRef="treatmentExecutionStage">
      <entryCriterion sentryRef="planApproved"/>
    </planItem>
    
    <!-- Monitoring -->
    <planItem id="patientMonitoring" definitionRef="monitoringTask">
      <entryCriterion sentryRef="treatmentStarted"/>
    </planItem>
    
    <!-- Emergency Response -->
    <planItem id="emergencyResponse" definitionRef="emergencyResponseTask">
      <entryCriterion sentryRef="emergencyDetected"/>
    </planItem>
    
    <!-- Recovery Milestone -->
    <planItem id="recoveryMilestone" definitionRef="recovery">
      <entryCriterion sentryRef="treatmentSuccessful"/>
    </planItem>

    <!-- Task Definitions -->
    <humanTask id="initialAssessmentTask" name="Initial Assessment" 
               flowable:candidateGroups="doctors">
      <extensionElements>
        <flowable:formField id="diagnosis" name="Diagnosis" type="string"/>
        <flowable:formField id="severity" name="Severity" type="enum">
          <flowable:value id="mild" name="Mild"/>
          <flowable:value id="moderate" name="Moderate"/>
          <flowable:value id="severe" name="Severe"/>
        </flowable:value>
        <flowable:formField id="urgency" name="Urgency" type="enum">
          <flowable:value id="routine" name="Routine"/>
          <flowable:value id="urgent" name="Urgent"/>
          <flowable:value id="emergency" name="Emergency"/>
        </flowable:value>
      </extensionElements>
    </humanTask>
    
    <!-- Treatment Planning Stage -->
    <stage id="treatmentPlanningStage" name="Treatment Planning">
      <planItem id="createTreatmentPlan" definitionRef="createTreatmentPlanTask"/>
      <planItem id="reviewTreatmentPlan" definitionRef="reviewTreatmentPlanTask"/>
      <planItem id="approveTreatmentPlan" definitionRef="approveTreatmentPlanTask"/>
      
      <humanTask id="createTreatmentPlanTask" name="Create Treatment Plan" 
                 flowable:candidateGroups="doctors"/>
      <humanTask id="reviewTreatmentPlanTask" name="Review Treatment Plan" 
                 flowable:candidateGroups="senior-doctors"/>
      <humanTask id="approveTreatmentPlanTask" name="Approve Treatment Plan" 
                 flowable:candidateGroups="department-heads"/>
    </stage>
    
    <!-- Treatment Execution Stage -->
    <stage id="treatmentExecutionStage" name="Treatment Execution">
      <planItem id="medicationAdministration" definitionRef="medicationTask"/>
      <planItem id="procedures" definitionRef="procedureTask"/>
      <planItem id="therapy" definitionRef="therapyTask"/>
      
      <humanTask id="medicationTask" name="Administer Medication" 
                 flowable:candidateGroups="nurses"/>
      <humanTask id="procedureTask" name="Perform Procedure" 
                 flowable:candidateGroups="doctors"/>
      <humanTask id="therapyTask" name="Conduct Therapy" 
                 flowable:candidateGroups="therapists"/>
    </stage>
    
    <!-- Monitoring Task -->
    <serviceTask id="monitoringTask" name="Patient Monitoring"
                 flowable:class="com.hospital.PatientMonitoringDelegate">
      <extensionElements>
        <flowable:field name="monitoringInterval">
          <flowable:expression>PT1H</flowable:expression>
        </flowable:field>
      </extensionElements>
    </serviceTask>
    
    <!-- Emergency Response -->
    <humanTask id="emergencyResponseTask" name="Emergency Response" 
               flowable:candidateGroups="emergency-team">
      <extensionElements>
        <flowable:formField id="emergencyType" name="Emergency Type" type="string"/>
        <flowable:formField id="responseAction" name="Response Action" type="string"/>
      </extensionElements>
    </humanTask>
    
    <milestone id="recovery" name="Patient Recovery"/>
    
    <!-- Sentries -->
    <sentry id="assessmentComplete">
      <planItemOnPart sourceRef="initialAssessment">
        <standardEvent>complete</standardEvent>
      </planItemOnPart>
    </sentry>
    
    <sentry id="planApproved">
      <planItemOnPart sourceRef="treatmentPlanning">
        <standardEvent>complete</standardEvent>
      </planItemOnPart>
    </sentry>
    
    <sentry id="treatmentStarted">
      <planItemOnPart sourceRef="treatmentExecution">
        <standardEvent>start</standardEvent>
      </planItemOnPart>
    </sentry>
    
    <sentry id="emergencyDetected">
      <variableOnPart variableName="emergencyAlert">
        <variableEvent>create</variableEvent>
      </variableOnPart>
      <ifPart>
        <condition>${emergencyAlert == true}</condition>
      </ifPart>
    </sentry>
    
    <sentry id="treatmentSuccessful">
      <planItemOnPart sourceRef="treatmentExecution">
        <standardEvent>complete</standardEvent>
      </planItemOnPart>
      <ifPart>
        <condition>${treatmentOutcome == 'successful'}</condition>
      </ifPart>
    </sentry>
    
  </casePlanModel>
</case>
```

## 5. Legal Case Management

### Features
- Document discovery
- Evidence management
- Court date scheduling
- Client communication
- Billing integration

```java
// Legal Case Service
@Service
@Transactional
public class LegalCaseService {
    
    private final CmmnRuntimeService runtimeService;
    private final DocumentService documentService;
    private final CalendarService calendarService;
    private final BillingService billingService;
    
    public void initiateDiscovery(String caseInstanceId, DiscoveryRequest request) {
        // Create discovery tasks based on case type
        Map<String, Object> variables = Map.of(
            "discoveryType", request.getDiscoveryType(),
            "discoveryDeadline", request.getDeadline(),
            "opposingParty", request.getOpposingParty()
        );
        
        runtimeService.createPlanItemInstanceBuilder()
            .caseInstanceId(caseInstanceId)
            .planItemDefinitionId("discoveryTask")
            .variables(variables)
            .create();
        
        // Schedule discovery deadline reminder
        scheduleDiscoveryReminder(caseInstanceId, request.getDeadline());
    }
    
    public void scheduleCourtDate(String caseInstanceId, CourtScheduleRequest request) {
        // Check attorney availability
        boolean attorneyAvailable = calendarService.checkAvailability(
            request.getAttorneyId(), request.getProposedDate());
        
        if (!attorneyAvailable) {
            throw new SchedulingConflictException("Attorney not available");
        }
        
        // Create court appearance task
        runtimeService.setVariables(caseInstanceId, Map.of(
            "courtDate", request.getProposedDate(),
            "courtRoom", request.getCourtRoom(),
            "judge", request.getJudge(),
            "hearingType", request.getHearingType()
        ));
        
        // Trigger court preparation activities
        runtimeService.createPlanItemInstanceBuilder()
            .caseInstanceId(caseInstanceId)
            .planItemDefinitionId("courtPreparation")
            .create();
    }
    
    public void recordBillableTime(String caseInstanceId, String taskId, 
                                  BillableTimeEntry timeEntry) {
        // Record time entry
        billingService.recordTime(caseInstanceId, timeEntry);
        
        // Update case variables
        runtimeService.setVariable(caseInstanceId, 
            "totalBillableHours", 
            billingService.getTotalHours(caseInstanceId));
        
        // Check billing thresholds
        checkBillingThresholds(caseInstanceId);
    }
}
```

## Performance Considerations

### 1. Case Instance Optimization
```java
@Configuration
public class CmmnPerformanceConfig {
    
    @Bean
    public CmmnEngineConfiguration cmmnEngineConfiguration() {
        CmmnEngineConfiguration config = new CmmnEngineConfiguration();
        
        // Optimize for large case instances
        config.setMaxLengthStringVariableType(10000);
        config.setEnableSafeCmmnXml(true);
        
        // Async executor configuration
        config.setAsyncExecutorActivate(true);
        config.setAsyncExecutorDefaultAsyncJobAcquireWaitTime(10000);
        config.setAsyncExecutorDefaultTimerJobAcquireWaitTime(10000);
        config.setAsyncExecutorMaxAsyncJobsDuePerAcquisition(10);
        
        return config;
    }
}
```

### 2. Database Optimization
```sql
-- Custom indexes for better performance
CREATE INDEX idx_case_business_key ON act_ru_case_inst(business_key_);
CREATE INDEX idx_task_case_def_key ON act_ru_task(case_definition_key_);
CREATE INDEX idx_plan_item_case_inst ON act_ru_plan_item_inst(case_inst_id_);
CREATE INDEX idx_variable_case_inst ON act_ru_variable(scope_id_) WHERE scope_type_ = 'cmmn';

-- Partitioning for history tables (PostgreSQL example)
CREATE TABLE act_hi_case_inst_y2024 PARTITION OF act_hi_case_inst
FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
```

These advanced examples demonstrate real-world CMMN patterns that you can adapt for your production workflows. Each example includes proper error handling, monitoring, and integration patterns suitable for enterprise applications.