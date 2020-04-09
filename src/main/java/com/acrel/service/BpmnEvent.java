package com.acrel.service;

import org.springframework.context.ApplicationEvent;

public class BpmnEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String defKey;
	private final String businessKey;
	private final String status;

	public BpmnEvent(Object source, String defKey, String businessKey, String status) {
		super(source);
		this.defKey = defKey;
		this.businessKey = businessKey;
		this.status = status;
	}

	public String getDefKey() {
		return defKey;
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public String getStatus() {
		return status;
	}

}
