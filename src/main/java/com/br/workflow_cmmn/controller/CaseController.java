package com.br.workflow_cmmn.controller;

import com.br.workflow_cmmn.model.*;
import com.br.workflow_cmmn.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CaseController - Main web controller for the Document Review Workflow System
 * 
 * CONTROLLER OVERVIEW:
 * This controller serves as the main entry point for all web interactions in the
 * document review workflow system. It handles user authentication, role-based
 * access control, and coordinates between the frontend templates and backend services.
 * 
 * SUPPORTED USER ROLES:
 * - ADMIN (User ID 1): Full system access, can create workflows, view all data
 * - UPLOADER (User IDs 2,3,4...): Can upload documents and complete preparation tasks
 * - REVIEWER (User IDs 2,3,4...): Can review documents and make approval decisions
 * 
 * KEY RESPONSIBILITIES:
 * 1. User Authentication: Login/logout and role determination
 * 2. Dashboard Routing: Direct users to appropriate role-based dashboards
 * 3. Workflow Management: Create and monitor workflow instances
 * 4. File Operations: Handle document uploads for tasks
 * 5. Task Completion: Process task state transitions
 * 6. Data Presentation: Prepare data models for Thymeleaf templates
 * 
 * EXTERNAL DEPENDENCIES:
 * - JSONPlaceholder API: Provides user data for authentication
 * - File System: Stores uploaded documents in 'uploads/' directory
 * - H2 Database: Persists workflow and task data
 * 
 * SECURITY NOTES:
 * - No actual authentication - uses external API user selection
 * - File uploads restricted to Excel formats (.xlsx, .xls)
 * - Maximum file size: 10MB per upload
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CaseController {

    private final DocumentService documentService;
    private final UserService userService;
    private final WorkflowService workflowService;
    private final WorkflowExecutionService workflowExecutionService;



    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
    
    /**
     * Displays the login page with list of available users from external API
     * Users are fetched from JSONPlaceholder API and assigned roles automatically
     * 
     * @param model - Thymeleaf model for template rendering
     * @return login template name
     */
    /**
     * Displays the login page with available users from external API
     * 
     * LOGIN PROCESS:
     * 1. Fetch users from JSONPlaceholder API (https://jsonplaceholder.typicode.com/users)
     * 2. Automatically assign roles based on user ID (1=ADMIN, others=UPLOADER/REVIEWER)
     * 3. Display users in dropdown for selection
     * 4. Handle API failures gracefully with empty user list
     * 
     * @param model Thymeleaf model for template data binding
     * @return login template name
     */
    @GetMapping("/login")
    public String login(Model model) {
        log.info("=== DISPLAYING LOGIN PAGE ===");
        
        try {
            log.debug("Fetching users from JSONPlaceholder API...");
            // Fetch users from external API (JSONPlaceholder)
            List<User> users = userService.getAllUsers().collectList().block();
            
            if (users != null && !users.isEmpty()) {
                log.info("Successfully fetched {} users from external API", users.size());
                log.debug("Available users: {}", users.stream()
                    .map(u -> u.getId() + ":" + u.getName())
                    .collect(Collectors.joining(", ")));
            } else {
                log.warn("No users returned from external API");
                users = java.util.Collections.emptyList();
            }
            
            // Add users to model for dropdown display
            model.addAttribute("users", users);
            log.debug("Added {} users to login page model", users.size());
            
        } catch (Exception e) {
            log.error("CRITICAL: Failed to fetch users from external API", e);
            log.error("Error details: {}", e.getMessage());
            
            // Add empty list to prevent template errors
            model.addAttribute("users", java.util.Collections.emptyList());
            log.warn("Login page will display with no available users due to API failure");
        }
        
        log.debug("Rendering login template");
        return "login";
    }
    
    /**
     * Processes login form submission and redirects to appropriate dashboard
     * Based on user role, redirects to admin, uploader, or reviewer dashboard
     * 
     * @param userId - Selected user ID from login form
     * @return redirect URL to role-specific dashboard
     */
    /**
     * Processes login form submission and redirects to appropriate dashboard
     * 
     * LOGIN PROCESSING:
     * 1. Validate user ID exists in external API
     * 2. Retrieve user details including auto-assigned role
     * 3. Redirect to role-specific dashboard with user ID parameter
     * 4. Handle errors with appropriate error messages
     * 
     * ROLE-BASED ROUTING:
     * - ADMIN (ID=1) -> /dashboard/admin
     * - UPLOADER -> /dashboard/uploader  
     * - REVIEWER -> /dashboard/reviewer
     * 
     * @param userId Selected user ID from login form dropdown
     * @return Redirect URL to role-specific dashboard or login page with error
     */
    @PostMapping("/login")
    public String doLogin(@RequestParam String userId) {
        log.info("=== PROCESSING LOGIN ===");
        log.info("Attempting login for user ID: {}", userId);
        
        try {
            log.debug("Fetching user details from external API for ID: {}", userId);
            // Fetch user details from external API
            User user = userService.getUserById(userId).block();
            
            if (user == null) {
                log.error("LOGIN FAILED: User not found for ID: {}", userId);
                log.warn("Redirecting to login page with userNotFound error");
                return "redirect:/login?error=userNotFound";
            }
            
            log.info("LOGIN SUCCESS: User '{}' (ID: {}) logged in with role: {}", 
                    user.getName(), user.getId(), user.getRole());
            
            // Redirect to role-specific dashboard
            String dashboardUrl = "/dashboard/" + user.getRole().toLowerCase() + "?userId=" + userId;
            log.info("Redirecting user to role-specific dashboard: {}", dashboardUrl);
            
            return "redirect:" + dashboardUrl;
            
        } catch (Exception e) {
            log.error("LOGIN EXCEPTION: Login failed for user ID: {}", userId, e);
            log.error("Exception details: {}", e.getMessage());
            log.warn("Redirecting to login page with loginFailed error");
            return "redirect:/login?error=loginFailed";
        }
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard(@RequestParam String userId, Model model) {
        User user = userService.getUserById(userId).block();
        List<Document> documents = documentService.getAllDocuments();
        List<Workflow> workflows = workflowService.getAllWorkflows();
        List<User> allUsers = userService.getAllUsers().collectList().block();
        List<Notification> notifications = workflowExecutionService.getNotifications(userId);
        List<WorkflowTask> allTasks = documentService.getAllTasks();
        List<WorkflowInstance> workflowInstances = workflowExecutionService.getAllWorkflowInstances();
        
        // Create user ID to name mapping
        Map<String, String> userNames = allUsers.stream()
            .collect(Collectors.toMap(User::getId, User::getName));
        
        model.addAttribute("user", user);
        model.addAttribute("documents", documents);
        model.addAttribute("workflows", workflows);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("notifications", notifications);
        model.addAttribute("allTasks", allTasks);
        model.addAttribute("workflowInstances", workflowInstances);
        model.addAttribute("userNames", userNames);
        model.addAttribute("activeCases", java.util.Collections.emptyList());
        model.addAttribute("totalTasks", allTasks.size());
        
        return "admin-dashboard";
    }

    @GetMapping("/dashboard/uploader")
    public String uploaderDashboard(@RequestParam String userId, Model model) {
        log.info("Loading uploader dashboard for user: {}", userId);
        
        User user = userService.getUserById(userId).block();
        List<Document> documents = documentService.getDocumentsByUploader(userId);
        List<User> reviewers = userService.getAllUsers().filter(u -> "REVIEWER".equals(u.getRole())).collectList().block();
        List<User> processors = userService.getAllUsers().filter(u -> "UPLOADER".equals(u.getRole())).collectList().block();
        
        // Get both active and upcoming tasks
        List<WorkflowTask> activeTasks = workflowExecutionService.getActiveTasksForUser(userId);
        List<WorkflowInstance> upcomingTasks = workflowExecutionService.getUpcomingTasksForUser(userId);
        List<Notification> notifications = workflowExecutionService.getNotifications(userId);
        
        log.info("Found {} active tasks and {} upcoming workflows for user {}", 
                activeTasks.size(), upcomingTasks.size(), userId);
        
        model.addAttribute("user", user);
        model.addAttribute("documents", documents);
        model.addAttribute("reviewers", reviewers);
        model.addAttribute("processors", processors);
        model.addAttribute("tasks", activeTasks);
        model.addAttribute("upcomingTasks", upcomingTasks);
        model.addAttribute("notifications", notifications);
        
        return "uploader-dashboard";
    }

    @GetMapping("/dashboard/reviewer")
    public String reviewerDashboard(@RequestParam String userId, Model model) {
        log.info("Loading reviewer dashboard for user: {}", userId);
        
        User user = userService.getUserById(userId).block();
        List<Document> documents = documentService.getDocumentsByReviewer(userId);
        
        // Get both active review tasks and upcoming tasks
        List<WorkflowTask> activeTasks = workflowExecutionService.getActiveReviewTasksForUser(userId);
        List<WorkflowInstance> upcomingTasks = workflowExecutionService.getUpcomingTasksForUser(userId);
        List<Notification> notifications = workflowExecutionService.getNotifications(userId);
        
        log.info("Found {} active review tasks and {} upcoming workflows for reviewer {}", 
                activeTasks.size(), upcomingTasks.size(), userId);
        
        model.addAttribute("user", user);
        model.addAttribute("documents", documents);
        model.addAttribute("tasks", activeTasks);
        model.addAttribute("upcomingTasks", upcomingTasks);
        model.addAttribute("notifications", notifications);
        
        return "reviewer-dashboard";
    }
    
    @GetMapping("/dashboard/preparator")
    public String preparatorDashboard(@RequestParam String userId, Model model) {
        log.info("Loading preparator dashboard for user: {}", userId);
        
        User user = userService.getUserById(userId).block();
        
        // Get both active and upcoming tasks
        List<WorkflowTask> activeTasks = workflowExecutionService.getActiveTasksForUser(userId);
        List<WorkflowInstance> upcomingTasks = workflowExecutionService.getUpcomingTasksForUser(userId);
        List<Notification> notifications = workflowExecutionService.getNotifications(userId);
        
        log.info("Found {} active tasks and {} upcoming workflows for preparator {}", 
                activeTasks.size(), upcomingTasks.size(), userId);
        
        model.addAttribute("user", user);
        model.addAttribute("tasks", activeTasks);
        model.addAttribute("upcomingTasks", upcomingTasks);
        model.addAttribute("notifications", notifications);
        
        return "preparator-dashboard";
    }

    /**
     * Handles file upload for workflow tasks
     * Saves the uploaded Excel file to server and completes the upload task
     * 
     * @param taskId - ID of the upload task being completed
     * @param file - Uploaded Excel file from user
     * @param userId - ID of user performing the upload
     * @return redirect to uploader dashboard
     */
    @PostMapping("/task/upload/{taskId}")
    public String uploadFile(@PathVariable Long taskId, @RequestParam MultipartFile file, @RequestParam String userId) {
        log.info("Processing file upload for task: {} by user: {}", taskId, userId);
        
        try {
            // Validate file is present
            if (file.isEmpty()) {
                log.warn("Empty file uploaded for task: {}", taskId);
                return "redirect:/dashboard/uploader?userId=" + userId + "&error=emptyFile";
            }
            
            String fileName = file.getOriginalFilename();
            log.debug("Uploading file: {} (size: {} bytes)", fileName, file.getSize());
            
            // Validate file format (Excel files only)
            if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
                log.warn("Invalid file format uploaded: {}", fileName);
                return "redirect:/dashboard/uploader?userId=" + userId + "&error=invalidFormat";
            }
            
            // Create uploads directory if it doesn't exist
            Path uploadPath = Paths.get("uploads/" + fileName);
            Files.createDirectories(uploadPath.getParent());
            
            // Save file to server
            Files.write(uploadPath, file.getBytes());
            log.info("File saved successfully: {}", uploadPath.toString());
            
            // Complete the upload task in workflow
            workflowExecutionService.completeUploadTask(taskId, uploadPath.toString(), "File uploaded successfully");
            log.info("Upload task {} completed successfully", taskId);
            
        } catch (Exception e) {
            log.error("File upload failed for task: {} by user: {}", taskId, userId, e);
            return "redirect:/dashboard/uploader?userId=" + userId + "&error=uploadFailed";
        }
        
        return "redirect:/dashboard/uploader?userId=" + userId + "&success=fileUploaded";
    }
    
    @PostMapping("/task/prepare/{taskId}")
    public String prepareFile(@PathVariable Long taskId, @RequestParam MultipartFile file, @RequestParam String userId) {
        log.info("Processing file preparation for task: {} by user: {}", taskId, userId);
        
        try {
            if (file.isEmpty()) {
                log.warn("Empty file uploaded for prepare task: {}", taskId);
                return "redirect:/dashboard/uploader?userId=" + userId + "&error=emptyFile";
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
                log.warn("Invalid file format for prepare task: {}", fileName);
                return "redirect:/dashboard/uploader?userId=" + userId + "&error=invalidFormat";
            }
            
            Path path = Paths.get("uploads/prepared_" + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
            
            workflowExecutionService.completePrepareTask(taskId, path.toString(), "File prepared successfully");
            log.info("Prepare task {} completed successfully", taskId);
            
        } catch (Exception e) {
            log.error("File preparation failed for task: {} by user: {}", taskId, userId, e);
            return "redirect:/dashboard/uploader?userId=" + userId + "&error=prepareFailed";
        }
        return "redirect:/dashboard/uploader?userId=" + userId + "&success=filePrepared";
    }
    
    @PostMapping("/task/review/{taskId}")
    public String reviewTask(@PathVariable Long taskId, @RequestParam String decision, 
                            @RequestParam String message, @RequestParam String userId) {
        log.info("Processing review for task: {} by user: {} with decision: {}", taskId, userId, decision);
        
        try {
            if (message == null || message.trim().isEmpty()) {
                log.warn("Empty review message for task: {}", taskId);
                return "redirect:/dashboard/reviewer?userId=" + userId + "&error=emptyMessage";
            }
            
            if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
                log.warn("Invalid review decision: {} for task: {}", decision, taskId);
                return "redirect:/dashboard/reviewer?userId=" + userId + "&error=invalidDecision";
            }
            
            workflowExecutionService.completeReviewTask(taskId, decision, message);
            log.info("Review task {} completed with decision: {}", taskId, decision);
            
        } catch (Exception e) {
            log.error("Review task completion failed for task: {} by user: {}", taskId, userId, e);
            return "redirect:/dashboard/reviewer?userId=" + userId + "&error=reviewFailed";
        }
        
        return "redirect:/dashboard/reviewer?userId=" + userId + "&success=reviewCompleted";
    }

    @PostMapping("/workflow/start")
    public String startWorkflow(@RequestParam String name, @RequestParam String startedBy,
                               @RequestParam String scheduledStart, @RequestParam String frequency,
                               @RequestParam String uploader, @RequestParam String preparator,
                               @RequestParam String reviewer, @RequestParam String instructions) {
        log.info("Starting new workflow: {} by admin: {}", name, startedBy);
        
        try {
            if (name == null || name.trim().isEmpty()) {
                log.warn("Empty workflow name provided by admin: {}", startedBy);
                return "redirect:/dashboard/admin?userId=" + startedBy + "&error=emptyName";
            }
            
            if (uploader.equals(preparator) || uploader.equals(reviewer) || preparator.equals(reviewer)) {
                log.warn("Duplicate user assignments in workflow: {}", name);
                return "redirect:/dashboard/admin?userId=" + startedBy + "&error=duplicateUsers";
            }
            
            LocalDateTime startTime = LocalDateTime.parse(scheduledStart, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
            workflowExecutionService.startWorkflow(name, startedBy, startTime, frequency, uploader, preparator, reviewer, instructions);
            log.info("Workflow '{}' created successfully", name);
            
        } catch (Exception e) {
            log.error("Failed to create workflow: {} by admin: {}", name, startedBy, e);
            return "redirect:/dashboard/admin?userId=" + startedBy + "&error=workflowFailed";
        }
        
        return "redirect:/dashboard/admin?userId=" + startedBy + "&success=workflowCreated";
    }
    
    @GetMapping("/audit")
    @ResponseBody
    public List<Map<String, Object>> getAuditData() {
        return java.util.Collections.emptyList();
    }
    
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam String userId) {
        User user = userService.getUserById(userId).block();
        if (user != null) {
            return "redirect:/dashboard/" + user.getRole().toLowerCase() + "?userId=" + userId;
        }
        return "redirect:/login";
    }
    
    @GetMapping("/workflow/{workflowId}/tasks")
    @ResponseBody
    public List<WorkflowTask> getWorkflowTasks(@PathVariable Long workflowId) {
        return workflowExecutionService.getTasksByWorkflowId(workflowId);
    }
    
    private String getUserName(String userId, List<User> allUsers) {
        return allUsers.stream()
            .filter(user -> userId.equals(user.getId()))
            .map(User::getName)
            .findFirst()
            .orElse("User " + userId);
    }
    
    @PostMapping("/upload")
    public String uploadDocument(@RequestParam String title, @RequestParam String fileName,
                                @RequestParam String uploadedBy, @RequestParam String processor,
                                @RequestParam String reviewer) {
        documentService.uploadDocument(title, fileName, uploadedBy, processor, reviewer);
        return "redirect:/dashboard/uploader?userId=" + uploadedBy;
    }
}
