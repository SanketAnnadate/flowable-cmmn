# Document Review Workflow System - User Guide

## Overview
This is a Spring Boot application that manages document review workflows using Flowable CMMN. The system supports role-based access with three user types: Admin, Uploader, and Reviewer.

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6 or higher

### Running the Application

1. **Navigate to project directory:**
   ```bash
   cd workflow-cmmn
   ```

2. **Start the application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Access the application:**
   - Open browser and go to: `http://localhost:8080`
   - You'll be redirected to the login page

## User Roles & Capabilities

### Admin Users
- **Access:** Full system oversight
- **Capabilities:**
  - Create new workflows
  - View all documents and tasks
  - Monitor workflow progress
  - Access system statistics
  - View audit information

### Uploader Users  
- **Access:** Document upload and preparation
- **Capabilities:**
  - Upload new documents
  - Complete upload tasks (file upload)
  - Complete preparation tasks (process and re-upload files)
  - View assigned tasks

### Reviewer Users
- **Access:** Document review and approval
- **Capabilities:**
  - Review uploaded and prepared documents
  - Approve or reject documents
  - Provide feedback messages
  - View assigned review tasks

## How to Use the System

### Step 1: Login
1. Go to `http://localhost:8080`
2. Select a user from the dropdown (users are fetched from JSONPlaceholder API)
3. Click "Login"
4. You'll be redirected to your role-specific dashboard

### Step 2: Admin - Create Workflow
1. Login as an Admin user (User ID 1)
2. Fill out the "Create New Workflow" form:
   - **Workflow Name:** Enter a descriptive name
   - **Scheduled Start:** Set start date/time
   - **Frequency:** Choose ONCE, DAILY, WEEKLY, or MONTHLY
   - **Uploader:** Select user who will upload documents
   - **Preparator:** Select user who will process documents  
   - **Reviewer:** Select user who will review documents
   - **Instructions:** Provide detailed instructions
3. Click "Start Workflow"

### Step 3: Uploader - Upload Document
1. Login as the assigned Uploader
2. In "My Tasks" section, find the UPLOAD task
3. Click "Choose File" and select an Excel file (.xlsx or .xls)
4. Click "Upload"
5. Task will be marked as COMPLETED and a PREPARE task will be created

### Step 4: Preparator - Process Document
1. Login as the assigned Preparator
2. In "My Tasks" section, find the PREPARE task
3. Download the original file using the provided link
4. Process the file (make necessary changes)
5. Upload the prepared file using "Choose File"
6. Click "Submit Prepared File"
7. Task will be completed and a REVIEW task will be created

### Step 5: Reviewer - Review & Approve
1. Login as the assigned Reviewer
2. In "Review Tasks" section, find the REVIEW task
3. Download both original and prepared files to compare
4. Enter review comments in the message box
5. Click either:
   - **"Approve"** - Completes the workflow successfully
   - **"Reject"** - Sends back to preparator for rework

## Navigation
- **Dashboard Button:** Available on all pages to return to your role-specific dashboard
- **Notifications:** Click the notification dropdown to see recent updates
- **User Info:** Your name and role are displayed in the top navigation

## File Requirements
- **Supported Formats:** Excel files only (.xlsx, .xls)
- **File Size Limit:** Maximum 10MB per file
- **Storage:** Files are saved in the `uploads/` directory

## Workflow States
- **ACTIVE:** Workflow is running with pending tasks
- **COMPLETED:** All tasks approved and workflow finished
- **SCHEDULED:** Workflow created but not yet started

## Task States
- **PENDING:** Task waiting for user action
- **COMPLETED:** Task successfully finished
- **REJECTED:** Task rejected and needs rework

## Troubleshooting

### Common Issues
1. **White Label Error:** Usually indicates missing endpoint or server error
2. **File Upload Failed:** Check file format (must be .xlsx or .xls) and size (max 10MB)
3. **User Not Found:** Ensure external API (JSONPlaceholder) is accessible

### Database Access
- **H2 Console:** `http://localhost:8080/h2-console`
- **JDBC URL:** `jdbc:h2:mem:testdb`
- **Username:** `sa`
- **Password:** (empty)

## Sample Workflow Process

1. **Admin creates workflow:** "Monthly Report Review"
2. **System creates upload task** for assigned uploader
3. **Uploader uploads** original Excel file
4. **System creates prepare task** for assigned preparator  
5. **Preparator downloads** original file, processes it, uploads prepared version
6. **System creates review task** for assigned reviewer
7. **Reviewer compares** both files and either:
   - **Approves:** Workflow completes, all participants notified
   - **Rejects:** New prepare task created, preparator notified with feedback

## API Endpoints
- `GET /` - Redirects to login
- `GET /login` - Login page
- `POST /login` - Process login
- `GET /dashboard/{role}` - Role-specific dashboards
- `POST /workflow/start` - Create new workflow
- `POST /task/upload/{taskId}` - Upload file for task
- `POST /task/prepare/{taskId}` - Submit prepared file
- `POST /task/review/{taskId}` - Submit review decision

## Support
For technical issues, check the application logs in the console where you started the application with `mvn spring-boot:run`.