package com.acrel;

import org.activiti.api.task.model.Task;

public class ApplicantTask extends Applicant {

	public ApplicantTask(Applicant cant, Task task, String taskKey) {
		super(cant.getName(), cant.getEmail(), cant.getPhoneNumber());
		super.setId(cant.getId());
		super.setStatus(cant.getStatus());
		this.task = task;
		this.taskKey = taskKey;
	}

	private final Task task;
	private final String taskKey;

	public Task getTask() {
		return task;
	}

	public String getTaskKey() {
		return taskKey;
	}

}