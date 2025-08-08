# Document Review Workflow System

A Spring Boot application using Flowable CMMN for document review workflow with role-based dashboards.

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+

### Setup & Run
1. **Clone/Download** the project
2. **Navigate** to project directory: `cd workflow-cmmn`
3. **Run setup** (Windows): `setup.bat`
4. **Start application**: `mvn spring-boot:run`
5. **Open browser**: http://localhost:8080

## Features
- External API integration for user management
- Role-based access (Admin, Uploader, Reviewer)
- Document upload and review workflow
- Workflow audit and monitoring
- Thymeleaf-based frontend

## User Roles
- **Admin**: Create workflows, view all documents and tasks, system monitoring
- **Uploader**: Upload documents, complete upload/preparation tasks
- **Reviewer**: Review documents, approve/reject with feedback

## How to Use

### 1. Login
- Go to http://localhost:8080
- Select a user from dropdown (fetched from JSONPlaceholder API)
- Click "Login"

### 2. Admin - Create Workflow
- Login as User ID 1 (Admin)
- Fill "Create New Workflow" form
- Assign uploader, preparator, and reviewer
- Click "Start Workflow"

### 3. Complete Workflow Tasks
- **Uploader**: Upload Excel files for pending UPLOAD tasks
- **Preparator**: Download, process, and re-upload files for PREPARE tasks
- **Reviewer**: Review both files and approve/reject for REVIEW tasks

### 4. Monitor Progress
- Use Dashboard button to navigate between pages
- Check notifications dropdown for updates
- Admin can view all task details

## File Requirements
- **Format**: Excel files only (.xlsx, .xls)
- **Size**: Maximum 10MB
- **Storage**: Files saved in `uploads/` directory

## Technology Stack
- Spring Boot 3.5.4
- Flowable CMMN 7.1.0
- Thymeleaf
- H2 Database
- Bootstrap 5

## Documentation
See `USER_GUIDE.md` for detailed instructions and troubleshooting.

## Database Access
- H2 Console: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`, Password: (empty)
