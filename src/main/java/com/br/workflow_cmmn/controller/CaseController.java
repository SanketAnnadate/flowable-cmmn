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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cases")
@RequiredArgsConstructor
public class CaseController {
    CmmnEngine cmmnEngine = new StandaloneInMemCmmnEngineConfiguration().buildCmmnEngine();
    CmmnRepositoryService cmmnRepositoryService = cmmnEngine.getCmmnRepositoryService();
    CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeployment()
            .addClasspathResource("user-name-case.cmmn")
            .name("User Name Input Case Deployment")
            .deploy();
    CmmnRuntimeService cmmnRuntimeService = cmmnEngine.getCmmnRuntimeService();
    CmmnTaskService taskService = cmmnEngine.getCmmnTaskService();

    @PostMapping("/start")
    public String startCase() {
        return cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("userNameInputCase")
                .start()
                .getId();
    }

    @GetMapping("/tasks")
    public List<Task> getTasks(@RequestParam String candidateGroup) {
        return taskService.createTaskQuery()
                .taskCandidateGroup(candidateGroup)
                .list();
    }

    @PostMapping("/tasks/{taskId}/complete")
    public void completeTask(@PathVariable String taskId, @RequestBody Map<String, Object> variables) {
        taskService.complete(taskId, variables);
    }
}
