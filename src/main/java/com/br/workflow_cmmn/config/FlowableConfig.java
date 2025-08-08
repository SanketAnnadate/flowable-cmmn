package com.br.workflow_cmmn.config;

import com.br.workflow_cmmn.listener.WorkflowEventListener;
import org.flowable.cmmn.spring.SpringCmmnEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class FlowableConfig {

    @Bean
    public EngineConfigurationConfigurer<SpringCmmnEngineConfiguration> cmmnEngineConfigurer(WorkflowEventListener eventListener) {
        return engineConfiguration -> {
            engineConfiguration.setAsyncExecutorActivate(true);
            engineConfiguration.setDatabaseSchemaUpdate("drop-create");
            engineConfiguration.setJdbcMaxActiveConnections(20);
            engineConfiguration.setJdbcMaxIdleConnections(10);
            engineConfiguration.setEventListeners(Collections.singletonList(eventListener));
//            engineConfiguration.setXmlValidation(false);
            engineConfiguration.setEnableSafeCmmnXml(false);
        };
    }
}