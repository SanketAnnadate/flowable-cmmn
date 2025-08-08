package com.br.workflow_cmmn.controller;

import com.br.workflow_cmmn.service.FlowableCmmnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final FlowableCmmnService flowableCmmnService;

    @GetMapping("/workflow/{caseId}/status")
    public ResponseEntity<Map<String, Object>> getWorkflowStatus(@PathVariable String caseId) {
        Map<String, Object> status = flowableCmmnService.getWorkflowStatus(caseId);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/user/{userId}/tasks")
    public ResponseEntity<?> getUserTasks(@PathVariable String userId) {
        return ResponseEntity.ok(flowableCmmnService.getTasksForUser(userId));
    }

    @PostMapping("/workflow/{caseId}/terminate")
    public ResponseEntity<String> terminateWorkflow(@PathVariable String caseId) {
        flowableCmmnService.terminateCase(caseId);
        return ResponseEntity.ok("Workflow terminated");
    }
}