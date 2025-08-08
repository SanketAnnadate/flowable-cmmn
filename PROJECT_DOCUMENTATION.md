# Document Review Workflow System - Complete Project Documentation

## 📋 Project Overview

This is a **Spring Boot application** that implements a **document review workflow** using **Flowable CMMN** (Case Management Model and Notation). The system manages Excel document processing through a structured workflow with role-based access control.

### 🎯 Business Purpose
- **Automate document review processes** in organizations
- **Ensure quality control** through multi-stage validation
- **Track workflow progress** with audit trails
- **Manage user roles** and responsibilities
- **Provide notifications** for task assignments and completions

---

## 🏗️ System Architecture

### Technology Stack
- **Backend**: Spring Boot 3.5.4, Java 17
- **Workflow Engine**: Flowable CMMN 6.8.0
- **Database**: H2 (in-memory for demo)
- **Frontend**: Thymeleaf + Bootstrap 5
- **External Integration**: JSONPlaceholder API for user management
- **Security**: Spring Security (disabled for demo)

### Key Components
1. **Workflow Engine**: Flowable CMMN for case management
2. **User Management**: External API integration
3. **File Handling**: Excel file upload/download system
4. **Notification System**: Real-time user notifications
5. **Role-Based Dashboards**: Admin, Uploader, Reviewer interfaces

---

## 👥 User Roles & Responsibilities

### 🔧 Admin User
**Capabilities:**
- Create and schedule new workflows
- View all documents across the system
- Monitor workflow status and audit trails
- Assign participants (uploader, preparator, reviewer)
- Set workflow instructions and schedules

**Dashboard Features:**
- Workflow creation form with scheduling
- System statistics (total documents, active cases, pending tasks)
- Complete document listing with status
- Notification center

### 📤 Uploader/Preparator User
**Capabilities:**
- Upload initial Excel documents
- Download original files for processing
- Upload processed/prepared versions
- View assigned tasks and their status
- Receive task notifications

**Dashboard Features:**
- File upload interface with Excel validation
- Task list showing upload and preparation tasks
- File download links for processing
- Progress tracking for submitted documents

### ✅ Reviewer User
**Capabilities:**
- Review both original and processed documents
- Approve or reject document submissions
- Provide feedback messages for rejections
- View review history and decisions

**Dashboard Features:**
- Document comparison interface
- Approval/rejection forms with comment fields
- File download links for both versions
- Review history and status tracking

---

## 🔄 Workflow Process Flow

### Stage 1: Workflow Initiation
```
Admin creates workflow → Sets schedule → Assigns participants → Workflow starts
```

### Stage 2: Document Upload
```
Upload task created → Uploader receives notification → File uploaded → Validation occurs
```

### Stage 3: Document Preparation
```
Prepare task created → Preparator downloads original → Processes document → Uploads prepared version
```

### Stage 4: Review & Decision
```
Review task created → Reviewer examines both files → Makes decision (Approve/Reject)
```

### Stage 5A: Approval Path
```
Document approved → Workflow completes → All participants notified → End
```

### Stage 5B: Rejection Path
```
Document rejected → Back to Stage 3 → Preparator receives feedback → Process repeats
```

---

## 📁 Project Structure

```
workflow-cmmn/
├── src/main/java/com/br/workflow_cmmn/
│   ├── controller/
│   │   └── CaseController.java          # Main web controller
│   ├── service/
│   │   ├── WorkflowExecutionService.java # Core workflow logic
│   │   ├── DocumentService.java         # Document management
│   │   ├── UserService.java            # External API integration
│   │   └── WorkflowService.java        # Workflow metadata
│   ├── model/
│   │   ├── User.java                   # User entity
│   │   ├── Document.java               # Document entity
│   │   ├── WorkflowInstance.java       # Workflow metadata
│   │   ├── WorkflowTask.java           # Task tracking
│   │   └── Notification.java           # User notifications
│   ├── repository/
│   │   ├── DocumentRepository.java     # Document data access
│   │   ├── WorkflowInstanceRepository.java
│   │   ├── WorkflowTaskRepository.java
│   │   └── NotificationRepository.java
│   ├── delegate/
│   │   ├── FileValidationDelegate.java # File format validation
│   │   ├── ProcessValidationDelegate.java
│   │   └── NotificationDelegate.java   # Workflow notifications
│   ├── listener/
│   │   ├── TaskCreationListener.java   # Task lifecycle events
│   │   └── ReviewCompleteListener.java # Review completion handling
│   └── config/
│       ├── SecurityConfig.java         # Security configuration
│       ├── WebConfig.java             # Web MVC configuration
│       └── DataLoader.java            # Sample data loading
├── src/main/resources/
│   ├── templates/                      # Thymeleaf templates
│   │   ├── login.html                 # User selection page
│   │   ├── admin-dashboard.html       # Admin interface
│   │   ├── uploader-dashboard.html    # Uploader interface
│   │   └── reviewer-dashboard.html    # Reviewer interface
│   ├── document-review-workflow.cmmn  # CMMN workflow definition
│   └── application.properties         # Configuration
└── uploads/                           # File storage directory
```

---

## 🔧 Configuration & Setup

### Application Properties
```properties
# Database Configuration (H2 in-memory)
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.h2.console.enabled=true

# Flowable CMMN Engine
flowable.cmmn.enabled=true

# File Upload Limits
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# External API
external.api.users.url=https://jsonplaceholder.typicode.com/users
```

### Running the Application
```bash
# Using Maven
mvn spring-boot:run

# Or using JAR
java -jar target/workflow-cmmn-0.0.1-SNAPSHOT.jar
```

### Access URLs
- **Application**: http://localhost:8080
- **H2 Console**: http://localhost:8080/h2-console
- **Audit API**: http://localhost:8080/audit

---

## 📊 Database Schema

### Core Tables
- **workflow_instance**: Stores workflow metadata and participants
- **workflow_task**: Tracks individual task states and assignments
- **document**: Stores document information and file paths
- **notification**: User notification system
- **workflow**: Workflow templates and definitions

### Flowable Tables
- **act_cmmn_***: Flowable CMMN engine tables for case management
- **act_ru_***: Runtime execution data
- **act_hi_***: Historical audit data

---

## 🔍 Logging & Monitoring

### Log Levels
- **INFO**: Workflow state changes, task completions, user actions
- **DEBUG**: Detailed execution flow, variable values, API calls
- **WARN**: Validation failures, missing data, recoverable errors
- **ERROR**: System failures, unrecoverable errors, exceptions

### Key Log Messages
```java
// Workflow lifecycle
log.info("Starting new workflow: {} by user: {}", name, startedBy);
log.info("Workflow {} completed successfully", workflowName);

// Task management
log.info("Upload task created with ID: {} for user: {}", taskId, userId);
log.info("Document APPROVED - completing workflow: {}", workflowName);

// File operations
log.info("File saved successfully: {}", filePath);
log.warn("Invalid file format uploaded: {}", fileName);

// Error handling
log.error("File upload failed for task: {} by user: {}", taskId, userId, e);
```

---

## 🚀 Deployment Considerations

### Production Checklist
- [ ] Replace H2 with production database (PostgreSQL/MySQL)
- [ ] Configure proper security authentication
- [ ] Set up file storage (AWS S3, network storage)
- [ ] Configure external user management system
- [ ] Set up monitoring and alerting
- [ ] Configure backup strategies
- [ ] Set up load balancing if needed

### Environment Variables
```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/workflow
DB_USERNAME=workflow_user
DB_PASSWORD=secure_password

# File Storage
FILE_STORAGE_PATH=/var/workflow/uploads
MAX_FILE_SIZE=50MB

# External APIs
USER_API_URL=https://company-api.com/users
USER_API_TOKEN=your_api_token
```

---

## 🧪 Testing Strategy

### Unit Tests
- Service layer business logic
- Validation delegates
- Repository operations
- Utility functions

### Integration Tests
- Workflow execution end-to-end
- File upload/download operations
- External API integration
- Database operations

### Manual Testing Scenarios
1. **Happy Path**: Upload → Process → Review → Approve
2. **Rejection Flow**: Upload → Process → Review → Reject → Reprocess
3. **Error Handling**: Invalid files, missing users, system failures
4. **Scheduling**: Automatic workflow triggering
5. **Notifications**: User notification delivery

---

## 📈 Future Enhancements

### Planned Features
- **Email Notifications**: SMTP integration for email alerts
- **Advanced Scheduling**: Cron expressions, holiday calendars
- **Document Versioning**: Track document changes over time
- **Bulk Operations**: Process multiple documents simultaneously
- **Analytics Dashboard**: Workflow performance metrics
- **Mobile Interface**: Responsive design for mobile devices
- **API Integration**: REST APIs for external system integration

### Scalability Improvements
- **Microservices Architecture**: Split into smaller services
- **Message Queues**: Async processing with RabbitMQ/Kafka
- **Caching Layer**: Redis for performance optimization
- **Container Deployment**: Docker and Kubernetes support

---

## 🆘 Troubleshooting Guide

### Common Issues

**Issue**: Workflow not starting
- Check user assignments are valid
- Verify CMMN file is properly deployed
- Check database connectivity

**Issue**: File upload failing
- Verify uploads directory exists and is writable
- Check file size limits in configuration
- Validate file format (Excel only)

**Issue**: Notifications not appearing
- Check notification repository for saved records
- Verify user ID mapping is correct
- Check browser console for JavaScript errors

**Issue**: External API integration failing
- Verify internet connectivity
- Check API endpoint availability
- Review API response format changes

### Debug Commands
```bash
# Check H2 database
curl http://localhost:8080/h2-console

# View audit data
curl http://localhost:8080/audit

# Check application logs
tail -f logs/application.log

# Verify file uploads
ls -la uploads/
```

---

This documentation provides a comprehensive understanding of the Document Review Workflow System, enabling developers and administrators to effectively use, maintain, and extend the application.