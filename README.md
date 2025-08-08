# Document Review Workflow System - Flowable CMMN Implementation

A comprehensive Spring Boot application implementing Flowable CMMN (Case Management Model and Notation) for document review workflows with role-based dashboards and real-time monitoring.

## Architecture Overview

### CMMN Implementation
- **Case-based workflow management** using Flowable CMMN 7.1.0
- **Dynamic task activation** based on completion conditions
- **Milestone-driven completion** with approval/rejection paths
- **Rework loops** for rejected documents
- **Event-driven notifications** and state tracking

### Core Components
1. **FlowableCmmnService** - Core CMMN operations and case management
2. **WorkflowEventListener** - Real-time event monitoring and logging
3. **CaseController** - Web interface and REST API endpoints
4. **Role-based Dashboards** - Specialized views for each user type

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+

### Setup & Run
1. **Clone/Download** the project
2. **Navigate** to project directory: `cd workflow-cmmn`
3. **Start application**: `mvn spring-boot:run`
4. **Open browser**: http://localhost:8080

## CMMN Workflow Design

### Workflow Structure
```
[Start] → [Upload Task] → [Prepare Task] → [Review Task] → [Complete/Reject]
                                ↑              ↓
                            [Rework Loop] ←────┘
```

### Task Sequencing
- **Upload Task**: Manually activated when case starts
- **Prepare Task**: Auto-activated when upload completes
- **Review Task**: Auto-activated when preparation completes
- **Rework Flow**: Triggered when review is rejected with rework flag
- **Completion**: Milestone reached when review is approved

### Case Variables
- `workflowName`, `startedBy`, `uploader`, `preparator`, `reviewer`
- `instructions`, `approved`, `needsRework`, `status`
- `originalFilePath`, `preparedFilePath`, `*Comments`, `*Time`

## User Roles & Capabilities

### Admin (User ID 1)
- Create and monitor all workflows
- View comprehensive dashboard with case summaries
- Terminate workflows
- Access all system data and statistics

### Uploader (User IDs 2,6,10...)
- Complete upload tasks with Excel file uploads
- View assigned upload and prepare tasks
- Track task completion status

### Preparator (User IDs 3,7,11...)
- Process uploaded documents
- Complete preparation tasks with processed files
- Access original files and upload comments

### Reviewer (User IDs 4,8,12...)
- Review both original and prepared documents
- Approve/reject with detailed feedback
- Trigger rework or completion flows

## API Endpoints

### Web Interface
- `GET /` - Redirect to login
- `GET /login` - User selection page
- `POST /login` - Process login
- `GET /dashboard/{role}` - Role-specific dashboards
- `POST /workflow/start` - Create new workflow
- `POST /task/{type}/{taskId}` - Complete tasks

### REST API
- `GET /api/workflow/{caseId}/status` - Get workflow status
- `GET /api/user/{userId}/tasks` - Get user tasks
- `POST /api/workflow/{caseId}/terminate` - Terminate workflow

## Technical Implementation

### CMMN Features Used
- **Human Tasks** with assignee expressions
- **Sentries** for entry conditions
- **Plan Item Lifecycle** management
- **Case Variables** for data flow
- **Milestones** for completion tracking
- **Conditional Expressions** for decision logic

### Flowable Configuration
- **Async Executor** enabled for performance
- **History Level** set to AUDIT for complete tracking
- **Event Listeners** for real-time monitoring
- **Database Schema** auto-update enabled

### File Management
- **Upload Directory**: `uploads/`
- **File Types**: Excel (.xlsx, .xls) only
- **Size Limit**: 10MB maximum
- **Naming**: Timestamped to prevent conflicts

## Monitoring & Debugging

### Database Access
- **H2 Console**: http://localhost:8080/h2-console
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`, **Password**: (empty)

### Key Tables
- `ACT_CMMN_CASE_INST` - Case instances
- `ACT_RU_TASK` - Active tasks
- `ACT_RU_VARIABLE` - Case variables
- `ACT_HI_CASEINST` - Case history

### Logging
- **Flowable Engine**: INFO level
- **Application**: DEBUG level
- **Event Tracking**: Real-time via WorkflowEventListener

## Technology Stack
- **Spring Boot** 3.5.4
- **Flowable CMMN** 7.1.0
- **Thymeleaf** for templating
- **H2 Database** for persistence
- **Bootstrap 5** for UI
- **WebFlux** for external API calls

## Best Practices Implemented
- **Transaction Management** with proper boundaries
- **Error Handling** with graceful degradation
- **Event-Driven Architecture** for loose coupling
- **RESTful API Design** for external integration
- **Role-Based Security** for access control
- **Comprehensive Logging** for troubleshooting

## Development Notes
- Uses Flowable 7.x API (not 6.x)
- CMMN XML follows OMG specification
- Event listeners implement both FlowableEventListener and PlanItemInstanceLifecycleListener
- Case variables used for cross-task data sharing
- Manual task activation for controlled workflow start
