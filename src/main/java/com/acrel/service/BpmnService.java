package com.acrel.service;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class BpmnService {

	private final ProcessEngine engine;
	private final ApplicationEventPublisher publisher;

	public BpmnService(ProcessEngine engine, ApplicationEventPublisher publisher) {
		super();
		this.engine = engine;
		this.publisher = publisher;
	}

	public void finish(ExecutionEntity execution, String status) {
		ProcessInstance instance = engine.getRuntimeService().createProcessInstanceQuery()
				.processInstanceId(execution.getProcessInstanceId()).singleResult();
		publisher.publishEvent(new BpmnEvent(this,
				engine.getRepositoryService().getProcessDefinition(instance.getProcessDefinitionId()).getKey(),
				instance.getBusinessKey(), status));
	}

	public void approve(ExecutionEntity execution) {
		finish(execution, "APPROVE");
	}

	public void reject(ExecutionEntity execution) {
		finish(execution, "REJECT");
	}
}
