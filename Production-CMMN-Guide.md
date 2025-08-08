# Production-Grade Spring Boot Flowable CMMN Guide

## Prerequisites Covered
✅ Spring Boot (4 years experience)  
✅ REST APIs, JPA, Security  
✅ Database Management  
✅ Production Deployment  

## CMMN vs BPMN - When to Use What

### Use CMMN When:
- **Unpredictable workflows** (customer support, legal cases)
- **Data-driven decisions** (insurance claims, medical diagnosis)
- **Knowledge worker tasks** (research, analysis)
- **Flexible execution order** (project management)

### Use BPMN When:
- **Structured processes** (order fulfillment, onboarding)
- **Sequential workflows** (approval chains)
- **Automated processes** (batch jobs, integrations)

## Production Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Frontend      │    │   Spring Boot   │    │   Database      │
│   (React/Vue)   │◄──►│   CMMN Engine   │◄──►│   PostgreSQL    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                              │
                              ▼
                       ┌─────────────────┐
                       │   External      │
                       │   Services      │
                       └─────────────────┘
```

## Core Dependencies (Production)

```xml
<dependencies>
    <!-- Flowable CMMN -->
    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-spring-boot-starter-cmmn</artifactId>
        <version>7.0.1</version>
    </dependency>
    
    <!-- Database -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    
    <!-- Security -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    
    <!-- Monitoring -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

## Production Configuration

```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/flowable_db
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

flowable:
  cmmn:
    enabled: true
    deploy-resources: true
    history-level: full
  database-schema-update: true
  async-executor-activate: true
  async-executor:
    default-async-job-acquire-wait-time: 10000
    default-timer-job-acquire-wait-time: 10000

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,flowable
  endpoint:
    health:
      show-details: always

logging:
  level:
    org.flowable: INFO
    com.yourcompany: DEBUG
```

## Production-Ready Service Layer

```java
@Service
@Transactional
@Slf4j
public class CaseManagementService {
    
    private final CmmnRuntimeService runtimeService;
    private final CmmnTaskService taskService;
    private final CmmnHistoryService historyService;
    private final CaseAuditService auditService;
    
    public CaseManagementService(CmmnRuntimeService runtimeService,
                                CmmnTaskService taskService,
                                CmmnHistoryService historyService,
                                CaseAuditService auditService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.historyService = historyService;
        this.auditService = auditService;
    }
    
    @Retryable(value = {FlowableException.class}, maxAttempts = 3)
    public CaseInstanceDto startCase(StartCaseRequest request) {
        try {
            validateCaseRequest(request);
            
            CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                .caseDefinitionKey(request.getCaseDefinitionKey())
                .businessKey(request.getBusinessKey())
                .name(request.getCaseName())
                .variables(request.getVariables())
                .start();
            
            auditService.logCaseStarted(caseInstance.getId(), request.getStartedBy());
            
            return CaseInstanceDto.from(caseInstance);
            
        } catch (Exception e) {
            log.error("Failed to start case: {}", request, e);
            throw new CaseManagementException("Failed to start case", e);
        }
    }
    
    public Page<TaskDto> getTasks(TaskQuery query, Pageable pageable) {
        org.flowable.task.api.TaskQuery taskQuery = taskService.createTaskQuery();
        
        // Apply filters
        if (query.getAssignee() != null) {
            taskQuery.taskAssignee(query.getAssignee());
        }
        if (query.getCandidateGroups() != null) {
            taskQuery.taskCandidateGroupIn(query.getCandidateGroups());
        }
        if (query.getCaseDefinitionKey() != null) {
            taskQuery.caseDefinitionKey(query.getCaseDefinitionKey());
        }
        
        // Apply pagination
        List<Task> tasks = taskQuery
            .orderByTaskCreateTime()
            .desc()
            .listPage((int) pageable.getOffset(), pageable.getPageSize());
        
        long total = taskQuery.count();
        
        List<TaskDto> taskDtos = tasks.stream()
            .map(TaskDto::from)
            .collect(Collectors.toList());
        
        return new PageImpl<>(taskDtos, pageable, total);
    }
    
    @PreAuthorize("hasPermission(#taskId, 'TASK', 'COMPLETE')")
    public void completeTask(String taskId, CompleteTaskRequest request) {
        Task task = taskService.createTaskQuery()
            .taskId(taskId)
            .singleResult();
        
        if (task == null) {
            throw new TaskNotFoundException("Task not found: " + taskId);
        }
        
        validateTaskCompletion(task, request);
        
        taskService.complete(taskId, request.getVariables());
        
        auditService.logTaskCompleted(taskId, request.getCompletedBy());
    }
}
```

## DTOs and Validation

```java
@Data
@Builder
public class StartCaseRequest {
    
    @NotBlank(message = "Case definition key is required")
    private String caseDefinitionKey;
    
    @NotBlank(message = "Business key is required")
    private String businessKey;
    
    @NotBlank(message = "Case name is required")
    private String caseName;
    
    @NotBlank(message = "Started by is required")
    private String startedBy;
    
    @Valid
    private Map<String, Object> variables = new HashMap<>();
}

@Data
@Builder
public class CaseInstanceDto {
    private String id;
    private String businessKey;
    private String name;
    private String state;
    private LocalDateTime startTime;
    private String startedBy;
    
    public static CaseInstanceDto from(CaseInstance caseInstance) {
        return CaseInstanceDto.builder()
            .id(caseInstance.getId())
            .businessKey(caseInstance.getBusinessKey())
            .name(caseInstance.getName())
            .state(caseInstance.getState())
            .startTime(LocalDateTime.ofInstant(
                caseInstance.getStartTime().toInstant(), 
                ZoneId.systemDefault()))
            .build();
    }
}
```

## Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/cases/**").hasRole("CASE_MANAGER")
                .requestMatchers("/api/tasks/**").hasRole("USER")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        return http.build();
    }
    
    @Bean
    public PermissionEvaluator permissionEvaluator() {
        return new CmmnPermissionEvaluator();
    }
}

@Component
public class CmmnPermissionEvaluator implements PermissionEvaluator {
    
    private final CmmnTaskService taskService;
    
    @Override
    public boolean hasPermission(Authentication auth, Object targetId, 
                               Object targetType, Object permission) {
        if ("TASK".equals(targetType) && "COMPLETE".equals(permission)) {
            return canCompleteTask(auth, (String) targetId);
        }
        return false;
    }
    
    private boolean canCompleteTask(Authentication auth, String taskId) {
        Task task = taskService.createTaskQuery()
            .taskId(taskId)
            .singleResult();
        
        if (task == null) return false;
        
        String username = auth.getName();
        
        // Check if user is assignee
        if (username.equals(task.getAssignee())) {
            return true;
        }
        
        // Check if user is in candidate groups
        List<String> userGroups = getUserGroups(auth);
        List<IdentityLink> candidates = taskService.getIdentityLinksForTask(taskId);
        
        return candidates.stream()
            .filter(link -> IdentityLinkType.CANDIDATE.equals(link.getType()))
            .anyMatch(link -> userGroups.contains(link.getGroupId()));
    }
}
```

## Advanced CMMN Patterns

### 1. Dynamic Task Creation
```java
@Component
public class DynamicTaskCreator {
    
    private final CmmnRuntimeService runtimeService;
    
    public void createAdHocTask(String caseInstanceId, CreateTaskRequest request) {
        PlanItemInstanceBuilder builder = runtimeService
            .createPlanItemInstanceBuilder()
            .caseInstanceId(caseInstanceId)
            .planItemDefinitionId("adhocTask")
            .planItemDefinitionType(PlanItemDefinitionType.HUMAN_TASK);
        
        if (request.getVariables() != null) {
            builder.variables(request.getVariables());
        }
        
        builder.create();
    }
}
```

### 2. Case Event Listeners
```java
@Component
public class CaseEventListener implements FlowableCaseInstanceStateChangedEventListener {
    
    private final NotificationService notificationService;
    private final MetricsService metricsService;
    
    @Override
    public void onStateChanged(FlowableCaseInstanceStateChangedEvent event) {
        String caseInstanceId = event.getCaseInstanceId();
        String newState = event.getNewState();
        
        switch (newState) {
            case CaseInstanceState.ACTIVE:
                metricsService.incrementCaseStarted();
                break;
            case CaseInstanceState.COMPLETED:
                metricsService.incrementCaseCompleted();
                notificationService.sendCaseCompletedNotification(caseInstanceId);
                break;
            case CaseInstanceState.TERMINATED:
                metricsService.incrementCaseTerminated();
                break;
        }
    }
}
```

### 3. Custom Expression Functions
```java
@Component("caseHelper")
public class CaseExpressionHelper {
    
    public boolean isHighPriority(Object priority) {
        if (priority instanceof Number) {
            return ((Number) priority).intValue() >= 8;
        }
        return false;
    }
    
    public boolean isBusinessHours() {
        LocalTime now = LocalTime.now();
        return now.isAfter(LocalTime.of(9, 0)) && 
               now.isBefore(LocalTime.of(17, 0));
    }
}
```

## Monitoring and Metrics

```java
@Component
public class CmmnMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter casesStarted;
    private final Counter casesCompleted;
    private final Timer taskCompletionTime;
    
    public CmmnMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.casesStarted = Counter.builder("cmmn.cases.started")
            .description("Number of cases started")
            .register(meterRegistry);
        this.casesCompleted = Counter.builder("cmmn.cases.completed")
            .description("Number of cases completed")
            .register(meterRegistry);
        this.taskCompletionTime = Timer.builder("cmmn.task.completion.time")
            .description("Task completion time")
            .register(meterRegistry);
    }
    
    @EventListener
    public void handleCaseStarted(CaseStartedEvent event) {
        casesStarted.increment(
            Tags.of("caseDefinitionKey", event.getCaseDefinitionKey())
        );
    }
}
```

## Testing Strategy

```java
@SpringBootTest
@Testcontainers
class CaseManagementServiceTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @Autowired
    private CaseManagementService caseService;
    
    @Autowired
    private CmmnRuntimeService runtimeService;
    
    @Test
    void shouldStartCaseSuccessfully() {
        // Given
        StartCaseRequest request = StartCaseRequest.builder()
            .caseDefinitionKey("testCase")
            .businessKey("TEST-001")
            .caseName("Test Case")
            .startedBy("testuser")
            .build();
        
        // When
        CaseInstanceDto result = caseService.startCase(request);
        
        // Then
        assertThat(result.getId()).isNotNull();
        assertThat(result.getBusinessKey()).isEqualTo("TEST-001");
        
        // Verify case is active
        CaseInstance caseInstance = runtimeService
            .createCaseInstanceQuery()
            .caseInstanceId(result.getId())
            .singleResult();
        
        assertThat(caseInstance.getState()).isEqualTo("active");
    }
}
```

## Deployment Considerations

### 1. Database Migration
```sql
-- V1__Initial_flowable_schema.sql
-- Flowable will create tables automatically, but you might want custom indexes

CREATE INDEX idx_act_ru_case_inst_business_key ON act_ru_case_inst(business_key_);
CREATE INDEX idx_act_ru_task_case_inst_id ON act_ru_task(scope_id_);
```

### 2. Docker Configuration
```dockerfile
FROM openjdk:17-jre-slim

COPY target/cmmn-app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### 3. Kubernetes Deployment
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cmmn-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: cmmn-app
  template:
    metadata:
      labels:
        app: cmmn-app
    spec:
      containers:
      - name: cmmn-app
        image: your-registry/cmmn-app:latest
        ports:
        - containerPort: 8080
        env:
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: username
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
```

## Performance Optimization

1. **Database Indexing**: Add indexes on frequently queried columns
2. **Connection Pooling**: Configure HikariCP properly
3. **Async Processing**: Use async executors for long-running tasks
4. **Caching**: Cache case definitions and user permissions
5. **Monitoring**: Use APM tools like New Relic or Datadog

## Next Steps for Production

1. **Implement comprehensive error handling**
2. **Add distributed tracing (Zipkin/Jaeger)**
3. **Set up centralized logging (ELK stack)**
4. **Create deployment pipelines (CI/CD)**
5. **Add comprehensive monitoring dashboards**
6. **Implement backup and disaster recovery**
7. **Performance testing and optimization**