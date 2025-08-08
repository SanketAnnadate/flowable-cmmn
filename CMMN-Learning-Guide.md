# Spring Boot Flowable CMMN Learning Guide

## What is CMMN?
Case Management Model and Notation (CMMN) is a standard for modeling case management processes. Unlike BPMN (which is process-oriented), CMMN is data-driven and handles unpredictable, knowledge-intensive work.

## Key CMMN Concepts

### 1. Case
A case represents a single instance of work (e.g., insurance claim, customer support ticket)

### 2. Case Plan Model
The template that defines how a case should be handled

### 3. Plan Items
Activities within a case:
- **Human Task**: Work done by people
- **Process Task**: Automated work
- **Case Task**: Sub-cases
- **Milestone**: Achievement markers
- **Stage**: Groups of related activities

### 4. Sentries
Rules that control when plan items become available or complete:
- **Entry Criterion**: When an item becomes available
- **Exit Criterion**: When an item should terminate

## Basic CMMN Structure

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/CMMN/20151109/MODEL"
             xmlns:flowable="http://flowable.org/cmmn"
             targetNamespace="http://www.flowable.org/casedef">

  <case id="myCase" name="My Case">
    <casePlanModel id="casePlanModel1" name="Case Plan">
      
      <!-- Plan Items -->
      <planItem id="task1" definitionRef="humanTask1"/>
      <planItem id="milestone1" definitionRef="milestone1"/>
      
      <!-- Task Definitions -->
      <humanTask id="humanTask1" name="Review Document"/>
      <milestone id="milestone1" name="Document Reviewed"/>
      
    </casePlanModel>
  </case>
</definitions>
```

## Spring Boot Integration

### Dependencies (pom.xml)
```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter-cmmn</artifactId>
    <version>6.8.0</version>
</dependency>
```

### Configuration (application.yml)
```yaml
flowable:
  cmmn:
    enabled: true
    deploy-resources: true
  database-schema-update: true
```

## Common CMMN Patterns

### 1. Simple Human Task
```xml
<planItem id="reviewTask" definitionRef="review"/>
<humanTask id="review" name="Review Application" flowable:candidateGroups="reviewers">
  <extensionElements>
    <flowable:formField id="decision" name="Decision" type="enum">
      <flowable:value id="approve" name="Approve"/>
      <flowable:value id="reject" name="Reject"/>
    </flowable:formField>
  </extensionElements>
</humanTask>
```

### 2. Conditional Task (with Entry Criterion)
```xml
<planItem id="approvalTask" definitionRef="approval">
  <entryCriterion sentryRef="needsApproval"/>
</planItem>
<sentry id="needsApproval">
  <planItemOnPart sourceRef="reviewTask">
    <standardEvent>complete</standardEvent>
  </planItemOnPart>
  <ifPart>
    <condition>${decision == 'approve'}</condition>
  </ifPart>
</sentry>
```

### 3. Milestone
```xml
<planItem id="completedMilestone" definitionRef="completed">
  <entryCriterion sentryRef="allTasksDone"/>
</planItem>
<milestone id="completed" name="Case Completed"/>
<sentry id="allTasksDone">
  <planItemOnPart sourceRef="approvalTask">
    <standardEvent>complete</standardEvent>
  </planItemOnPart>
</sentry>
```

### 4. Stage (Grouping Activities)
```xml
<planItem id="reviewStage" definitionRef="reviewStageTask"/>
<stage id="reviewStageTask" name="Review Stage">
  <planItem id="initialReview" definitionRef="initialReviewTask"/>
  <planItem id="detailedReview" definitionRef="detailedReviewTask"/>
  <humanTask id="initialReviewTask" name="Initial Review"/>
  <humanTask id="detailedReviewTask" name="Detailed Review"/>
</stage>
```

## Java API Examples

### Starting a Case
```java
@Autowired
private CmmnRuntimeService runtimeService;

public String startCase(Map<String, Object> variables) {
    return runtimeService.createCaseInstanceBuilder()
        .caseDefinitionKey("myCase")
        .variables(variables)
        .start()
        .getId();
}
```

### Querying Tasks
```java
@Autowired
private CmmnTaskService taskService;

public List<Task> getTasksForUser(String userId) {
    return taskService.createTaskQuery()
        .taskAssignee(userId)
        .active()
        .list();
}
```

### Completing Tasks
```java
public void completeTask(String taskId, Map<String, Object> variables) {
    taskService.complete(taskId, variables);
}
```

### Case Variables
```java
// Set variables
runtimeService.setVariables(caseInstanceId, variables);

// Get variables
Map<String, Object> vars = runtimeService.getVariables(caseInstanceId);
```

## Best Practices

1. **Use Stages** to group related activities
2. **Leverage Milestones** to track progress
3. **Use Case Variables** for data sharing
4. **Design for Flexibility** - CMMN excels at handling unpredictable workflows
5. **Use Sentries Wisely** - Don't over-constrain the case flow

## Common Use Cases

- **Customer Support**: Ticket handling with various resolution paths
- **Insurance Claims**: Complex approval processes with multiple stakeholders
- **Legal Cases**: Document review, evidence gathering, court proceedings
- **Medical Treatment**: Patient care with various treatment options
- **Project Management**: Flexible project execution with changing requirements

## Next Steps

1. Create simple cases with human tasks
2. Add conditional logic with sentries
3. Implement stages and milestones
4. Build REST APIs for case management
5. Add forms and user interfaces
6. Integrate with external systems