//package com.br.workflow_cmmn.service;
//
//import lombok.RequiredArgsConstructor;
//import org.flowable.cmmn.api.CmmnRuntimeService;
//import org.flowable.cmmn.api.CmmnRepositoryService;
//import org.flowable.cmmn.api.CmmnTaskService;
//import org.flowable.cmmn.api.runtime.CaseInstance;
//import org.flowable.task.api.Task;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class CmmnCaseService {
//
//    private final CmmnRepositoryService repositoryService;
//    private final CmmnRuntimeService runtimeService;
//    private final CmmnTaskService taskService;
//
//    public void deployCaseModel() {
//        repositoryService.createDeployment()
//                .addClasspathResource("case-models/simple-review.cmmn.xml")
//                .name("Simple Review Deployment")
//                .deploy();
//    }
//
//    public CaseInstance startCaseInstance() {
//        return runtimeService.createCaseInstanceBuilder()
//                .caseDefinitionKey("simpleReviewCase")
//                .start();
//    }
//
//    public List<Task> listTasks() {
//        return taskService.createTaskQuery().list();
//    }
//
//    public void completeTask(String taskId) {
//        taskService.complete(taskId);
//    }
//}
