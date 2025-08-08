package com.br.workflow_cmmn.listener;

import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WorkflowEventListener implements FlowableEventListener {

    @Override
    public void onEvent(FlowableEvent event) {
        try {
            log.info("CMMN Event: {}", event.getType());
        } catch (Exception e) {
            log.debug("Event processing: {}", event.getType());
        }
    }

    @Override
    public boolean isFailOnException() {
        return false;
    }

    @Override
    public boolean isFireOnTransactionLifecycleEvent() {
        return false;
    }

    @Override
    public String getOnTransaction() {
        return null;
    }
}