package com.br.workflow_cmmn.controller;

import lombok.RequiredArgsConstructor;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.impl.cfg.StandaloneInMemCmmnEngineConfiguration;
import org.flowable.task.api.Task;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/cases")
public class CaseController {
    private CmmnEngine cmmnEngine;
    private CmmnRepositoryService cmmnRepositoryService;
    private CmmnRuntimeService cmmnRuntimeService;
    private CmmnTaskService taskService;

    @PostConstruct
    public void init() {
        cmmnEngine = new StandaloneInMemCmmnEngineConfiguration().buildCmmnEngine();
        cmmnRepositoryService = cmmnEngine.getCmmnRepositoryService();
        cmmnRepositoryService.createDeployment()
                .addClasspathResource("user-name-case.cmmn")
                .name("User Name Input Case Deployment")
                .deploy();
        cmmnRuntimeService = cmmnEngine.getCmmnRuntimeService();
        taskService = cmmnEngine.getCmmnTaskService();
    }

    @PostMapping("/start")
    public String startCase() {
        return cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("userNameInputCase")
                .start()
                .getId();
    }

    @GetMapping("/tasks")
    public List<Map<String, Object>> getTasks(@RequestParam(defaultValue = "users") String candidateGroup) {
        return taskService.createTaskQuery()
                .taskCandidateGroup(candidateGroup)
                .list()
                .stream()
                .map(this::taskToMap)
                .collect(Collectors.toList());
    }

    @GetMapping("/tasks/all")
    public List<Map<String, Object>> getAllTasks() {
        return taskService.createTaskQuery()
                .list()
                .stream()
                .map(this::taskToMap)
                .collect(Collectors.toList());
    }

    @PostMapping("/tasks/{taskId}/complete")
    public String completeTask(@PathVariable String taskId, @RequestBody Map<String, Object> variables) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return "Task not found: " + taskId;
        }
        taskService.complete(taskId, variables);
        return "Task completed successfully";
    }

    @GetMapping("/tasks/{taskId}")
    public Map<String, Object> getTask(@PathVariable String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        return task != null ? taskToMap(task) : Map.of("error", "Task not found");
    }

    private Map<String, Object> taskToMap(Task task) {
        return Map.of(
            "id", task.getId(),
            "name", task.getName(),
            "assignee", task.getAssignee() != null ? task.getAssignee() : "",
            "createTime", task.getCreateTime(),
            "caseInstanceId", task.getScopeId()
        );
    }

    @GetMapping("/debug")
    public Map<String, Object> debug() {
        return Map.of(
            "totalTasks", taskService.createTaskQuery().count(),
            "activeCases", cmmnRuntimeService.createCaseInstanceQuery().count()
        );
    }
}
