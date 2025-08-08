package com.br.workflow_cmmn.controller;

import com.br.workflow_cmmn.model.Document;
import com.br.workflow_cmmn.model.User;
import com.br.workflow_cmmn.service.DocumentService;
import com.br.workflow_cmmn.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CaseController {

    private final DocumentService documentService;
    private final UserService userService;



    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }
    
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("users", userService.getAllUsers().collectList().block());
        return "login";
    }
    
    @PostMapping("/login")
    public String doLogin(@RequestParam String userId) {
        User user = userService.getUserById(userId).block();
        return "redirect:/dashboard/" + user.getRole().toLowerCase() + "?userId=" + userId;
    }

    @GetMapping("/dashboard/admin")
    public String adminDashboard(@RequestParam String userId, Model model) {
        User user = userService.getUserById(userId).block();
        List<Document> documents = documentService.getAllDocuments();
        model.addAttribute("user", user);
        model.addAttribute("documents", documents);
        model.addAttribute("activeCases", java.util.Collections.emptyList());
        model.addAttribute("totalTasks", 0L);
        
        return "admin-dashboard";
    }

    @GetMapping("/dashboard/uploader")
    public String uploaderDashboard(@RequestParam String userId, Model model) {
        User user = userService.getUserById(userId).block();
        List<Document> documents = documentService.getDocumentsByUploader(userId);
        List<User> reviewers = userService.getAllUsers().filter(u -> "REVIEWER".equals(u.getRole())).collectList().block();
        
        model.addAttribute("user", user);
        model.addAttribute("documents", documents);
        model.addAttribute("reviewers", reviewers);
        
        return "uploader-dashboard";
    }

    @GetMapping("/dashboard/reviewer")
    public String reviewerDashboard(@RequestParam String userId, Model model) {
        User user = userService.getUserById(userId).block();
        List<Document> documents = documentService.getDocumentsByReviewer(userId);
        model.addAttribute("user", user);
        model.addAttribute("documents", documents);
        model.addAttribute("tasks", java.util.Collections.emptyList());
        
        return "reviewer-dashboard";
    }

    @PostMapping("/upload")
    public String uploadDocument(@RequestParam String title, @RequestParam String fileName, 
                                @RequestParam String uploadedBy, @RequestParam String assignedReviewer) {
        documentService.uploadDocument(title, fileName, uploadedBy, assignedReviewer);
        return "redirect:/dashboard/uploader?userId=" + uploadedBy;
    }
    
    @PostMapping("/review/{documentId}")
    public String reviewDocument(@PathVariable Long documentId, @RequestParam String status, 
                                @RequestParam String comments, @RequestParam String reviewerId) {
        documentService.reviewDocument(documentId, status, comments);
        return "redirect:/dashboard/reviewer?userId=" + reviewerId;
    }

    @GetMapping("/audit")
    @ResponseBody
    public List<Map<String, Object>> getAuditData() {
        return java.util.Collections.emptyList();
    }
}
