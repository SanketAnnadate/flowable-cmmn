# Document Review Workflow System

A Spring Boot application using Flowable CMMN for document review workflow with role-based dashboards.

## Features
- External API integration for user management
- Role-based access (Admin, Uploader, Reviewer)
- Document upload and review workflow
- Workflow audit and monitoring
- Thymeleaf-based frontend

## Roles
- **Admin**: View all documents, workflow audits, and system statistics
- **Uploader**: Upload documents and assign reviewers
- **Reviewer**: Review assigned documents and provide feedback

## Usage
1. Start the application: `mvn spring-boot:run`
2. Access: http://localhost:8080
3. Select a user from the dropdown (fetched from external API)
4. Use role-specific dashboard features

## Technology Stack
- Spring Boot 3.5.4
- Flowable CMMN 6.8.0
- Thymeleaf
- H2 Database
- Bootstrap 5
