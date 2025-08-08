package com.br.workflow_cmmn.controller;

import com.br.workflow_cmmn.model.User;
import com.br.workflow_cmmn.service.UserService;
import com.br.workflow_cmmn.service.FlowableCmmnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CaseController {

    private final UserService userService;
    private final FlowableCmmnService flowableCmmnService;

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model) {
        try {
            List<User> users = userService.getAllUsers().collectList().block();
            model.addAttribute("users", users != null ? users : Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to fetch users", e);
            model.addAttribute("users", Collections.emptyList());
        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String userId) {
        try {
            User user = userService.getUserById(userId).block();
            if (user == null) {
                return "redirect:/login?error=userNotFound";
            }
            return "redirect:/dashboard/" + user.getRole().toLowerCase() + "?userId=" + userId;
        } catch (Exception e) {
            log.error("Login failed for user: {}", userId, e);
            return "redirect:/login?error=loginFailed";
        }
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard(@RequestParam String userId, Model model) {
        try {
            User user = userService.getUserById(userId).block();
            List<User> allUsers = userService.getAllUsers().collectList().block();
            List<org.flowable.task.api.Task> allTasks = flowableCmmnService.getActiveTasks();
            List<org.flowable.cmmn.api.runtime.CaseInstance> caseInstances = flowableCmmnService.getAllCaseInstances();
            
            Map<String, String> userNames = allUsers.stream()
                .collect(Collectors.toMap(User::getId, User::getName));
            
            model.addAttribute("user", user);
            model.addAttribute("allUsers", allUsers);
            model.addAttribute("userNames", userNames);
            model.addAttribute("totalTasks", allTasks.size());
            model.addAttribute("caseInstances", caseInstances);
            model.addAttribute("allTasks", allTasks);
            model.addAttribute("notifications", Collections.emptyList());
            model.addAttribute("activeCases", caseInstances.size());
            model.addAttribute("documents", Collections.emptyList());
            model.addAttribute("workflows", Collections.emptyList());
            
        } catch (Exception e) {
            log.error("Error loading admin dashboard", e);
            model.addAttribute("error", "Failed to load dashboard data");
        }
        
        return "admin-dashboard";
    }

    @GetMapping("/dashboard/uploader")
    public String uploaderDashboard(@RequestParam String userId, Model model) {
        try {
            User user = userService.getUserById(userId).block();
            List<org.flowable.task.api.Task> userTasks = flowableCmmnService.getTasksForUser(userId);
            
            model.addAttribute("user", user);
            model.addAttribute("tasks", userTasks);
            model.addAttribute("notifications", Collections.emptyList());
            model.addAttribute("upcomingTasks", Collections.emptyList());
            
        } catch (Exception e) {
            log.error("Error loading uploader dashboard for user: {}", userId, e);
            model.addAttribute("error", "Failed to load dashboard data");
        }
        
        return "uploader-dashboard";
    }

    @GetMapping("/dashboard/reviewer")
    public String reviewerDashboard(@RequestParam String userId, Model model) {
        try {
            User user = userService.getUserById(userId).block();
            List<org.flowable.task.api.Task> reviewTasks = flowableCmmnService.getTasksForUser(userId);
            
            model.addAttribute("user", user);
            model.addAttribute("tasks", reviewTasks);
            model.addAttribute("notifications", Collections.emptyList());
            model.addAttribute("upcomingTasks", Collections.emptyList());
            
        } catch (Exception e) {
            log.error("Error loading reviewer dashboard for user: {}", userId, e);
            model.addAttribute("error", "Failed to load dashboard data");
        }
        
        return "reviewer-dashboard";
    }
    
    @GetMapping("/dashboard/preparator")
    public String preparatorDashboard(@RequestParam String userId, Model model) {
        try {
            User user = userService.getUserById(userId).block();
            List<org.flowable.task.api.Task> prepareTasks = flowableCmmnService.getTasksForUser(userId);
            
            model.addAttribute("user", user);
            model.addAttribute("tasks", prepareTasks);
            model.addAttribute("notifications", Collections.emptyList());
            model.addAttribute("upcomingTasks", Collections.emptyList());
            
        } catch (Exception e) {
            log.error("Error loading preparator dashboard for user: {}", userId, e);
            model.addAttribute("error", "Failed to load dashboard data");
        }
        
        return "preparator-dashboard";
    }

    @PostMapping("/task/upload/{taskId}")
    public String uploadFile(@PathVariable String taskId, @RequestParam MultipartFile file, @RequestParam String userId) {
        try {
            if (file.isEmpty()) {
                return "redirect:/dashboard/uploader?userId=" + userId + "&error=emptyFile";
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
                return "redirect:/dashboard/uploader?userId=" + userId + "&error=invalidFormat";
            }
            
            Path uploadPath = Paths.get("uploads/" + System.currentTimeMillis() + "_" + fileName);
            Files.createDirectories(uploadPath.getParent());
            Files.write(uploadPath, file.getBytes());
            
            flowableCmmnService.completeUploadTask(taskId, uploadPath.toString(), "File uploaded: " + fileName);
            return "redirect:/dashboard/uploader?userId=" + userId + "&success=fileUploaded";
            
        } catch (Exception e) {
            log.error("File upload failed", e);
            return "redirect:/dashboard/uploader?userId=" + userId + "&error=uploadFailed";
        }
    }
    
    @PostMapping("/task/prepare/{taskId}")
    public String prepareFile(@PathVariable String taskId, @RequestParam MultipartFile file, @RequestParam String userId) {
        try {
            if (file.isEmpty()) {
                return "redirect:/dashboard/preparator?userId=" + userId + "&error=emptyFile";
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
                return "redirect:/dashboard/preparator?userId=" + userId + "&error=invalidFormat";
            }
            
            Path path = Paths.get("uploads/prepared_" + System.currentTimeMillis() + "_" + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());
            
            flowableCmmnService.completePrepareTask(taskId, path.toString(), "File prepared: " + fileName);
            return "redirect:/dashboard/preparator?userId=" + userId + "&success=filePrepared";
            
        } catch (Exception e) {
            log.error("File preparation failed", e);
            return "redirect:/dashboard/preparator?userId=" + userId + "&error=prepareFailed";
        }
    }
    
    @PostMapping("/task/review/{taskId}")
    public String reviewTask(@PathVariable String taskId, @RequestParam String decision, 
                            @RequestParam String message, @RequestParam String userId) {
        try {
            if (message == null || message.trim().isEmpty()) {
                return "redirect:/dashboard/reviewer?userId=" + userId + "&error=emptyMessage";
            }
            
            if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
                return "redirect:/dashboard/reviewer?userId=" + userId + "&error=invalidDecision";
            }
            
            flowableCmmnService.completeReviewTask(taskId, "APPROVED".equals(decision), message);
            return "redirect:/dashboard/reviewer?userId=" + userId + "&success=reviewCompleted";
            
        } catch (Exception e) {
            log.error("Review task failed", e);
            return "redirect:/dashboard/reviewer?userId=" + userId + "&error=reviewFailed";
        }
    }

    @PostMapping("/workflow/start")
    public String startWorkflow(@RequestParam String name, @RequestParam String startedBy,
                               @RequestParam(required = false) String scheduledStart, 
                               @RequestParam(required = false) String frequency,
                               @RequestParam String uploader, @RequestParam String preparator,
                               @RequestParam String reviewer, @RequestParam String instructions) {
        try {
            if (name == null || name.trim().isEmpty()) {
                return "redirect:/dashboard/admin?userId=" + startedBy + "&error=emptyName";
            }
            
            if (uploader.equals(preparator) || uploader.equals(reviewer) || preparator.equals(reviewer)) {
                return "redirect:/dashboard/admin?userId=" + startedBy + "&error=duplicateUsers";
            }
            
            flowableCmmnService.startWorkflow(name, startedBy, uploader, preparator, reviewer, instructions);
            return "redirect:/dashboard/admin?userId=" + startedBy + "&success=workflowCreated";
            
        } catch (Exception e) {
            log.error("Failed to create workflow", e);
            return "redirect:/dashboard/admin?userId=" + startedBy + "&error=workflowFailed";
        }
    }
    
    @GetMapping("/dashboard")
    public String dashboard(@RequestParam String userId) {
        User user = userService.getUserById(userId).block();
        return user != null ? "redirect:/dashboard/" + user.getRole().toLowerCase() + "?userId=" + userId : "redirect:/login";
    }
}