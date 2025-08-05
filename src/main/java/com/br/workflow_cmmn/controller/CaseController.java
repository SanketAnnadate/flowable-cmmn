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
    public List<Task> getTasks(@RequestParam(defaultValue = "users") String candidateGroup) {
        return taskService.createTaskQuery()
                .taskCandidateGroup(candidateGroup)
                .list();
    }

    @GetMapping("/tasks/all")
    public List<Task> getAllTasks() {
        return taskService.createTaskQuery().list();
    }

    @PostMapping("/tasks/{taskId}/complete")
    public void completeTask(@PathVariable String taskId, @RequestBody Map<String, Object> variables) {
        taskService.complete(taskId, variables);
    }
}
