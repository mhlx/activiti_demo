package com.acrel.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.runtime.api.query.impl.PageImpl;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.acrel.Applicant;
import com.acrel.ApplicantRepository;
import com.acrel.ApplicantTask;
import com.acrel.ApplicationProcess;
import com.acrel.ApplicationProcessRepository;

@Service
public class ApplicantService {

	private static final String PROCESS_DEFINETION_KEY = "hireProcessWithJpa";
	private static final String BUSINESS_MODEL = "applicant";

	private final ApplicantRepository repo;
	private final ApplicationProcessRepository processRepo;
	private final ProcessEngine engine;
	private final TaskRuntime taskRuntime;
	private final ProcessRuntime processRuntime;

	public ApplicantService(ApplicantRepository repo, ApplicationProcessRepository processRepo, ProcessEngine engine,
			TaskRuntime taskRuntime, ProcessRuntime processRuntime) {
		super();
		this.repo = repo;
		this.engine = engine;
		this.processRepo = processRepo;
		this.taskRuntime = taskRuntime;
		this.processRuntime = processRuntime;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public long saveApplicant(Applicant applicant) {
		applicant.setStatus("NONE");
		repo.save(applicant);
		return applicant.getId();
	}

	@Transactional(readOnly = true)
	public List<Applicant> getAllApplicants() {
		List<Applicant> list = repo.findAll();
		return list;
	}

	@Transactional(readOnly = true)
	public Page<ApplicantTask> getNeedChecks(int startIndex, int maxItems) {
		Page<Task> page = taskRuntime.tasks(Pageable.of(startIndex, maxItems));
		List<ApplicantTask> tasks = page.getContent().stream().map(t -> {
			String businessKey = processRuntime.processInstance(t.getProcessInstanceId()).getBusinessKey();
			String definitionKey = engine.getTaskService().createTaskQuery().taskId(t.getId()).singleResult()
					.getTaskDefinitionKey();
			return new ApplicantTask(repo.getOne(Long.parseLong(businessKey)), t, definitionKey);
		}).collect(Collectors.toList());
		return new PageImpl<ApplicantTask>(tasks, page.getTotalItems());
	}

	public void printHistory(long id) {
		ApplicationProcess process = processRepo.findAll().get(0);
		List<HistoricTaskInstance> tasks = engine.getHistoryService().createHistoricTaskInstanceQuery()
				.processInstanceId(process.getProcess()).orderByTaskCreateTime().asc().list();
		for (HistoricTaskInstance task : tasks) {
			System.out.println(task.getTaskDefinitionKey() + "....." + task.getAssignee());
			for (HistoricVariableInstance var : engine.getHistoryService().createHistoricVariableInstanceQuery()
					.taskId(task.getId()).list()) {
				System.out.println(var.getVariableName() + "..." + var.getValue());
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void claimTask(String taskId) {
		taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(taskId).build());
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void complete(String taskId, String signal, String message) {
		TaskService ts = engine.getTaskService();
		// TODO getTaskDefinitionKey 有没有其他方法？？
		org.activiti.engine.task.Task task = ts.createTaskQuery().taskId(taskId).singleResult();
		if (task == null) {
			throw new RuntimeException("任务不存在");
		}
		if (!StringUtils.hasLength(task.getAssignee())) {
			throw new RuntimeException("请先认领该任务");
		}
		ts.addComment(taskId, task.getProcessInstanceId(), message);
		taskRuntime.complete(TaskPayloadBuilder.complete().withTaskId(taskId)
				.withVariable(task.getTaskDefinitionKey() + "_signal", signal).build());
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void startProcess(long id) {
		Applicant applicant = repo.getOne(id);
		if (!"NONE".equals(applicant.getStatus())) {
			throw new RuntimeException("已经提交审批");
		}
		applicant.setStatus("STARTED");
		repo.save(applicant);

		ProcessInstance instance = processRuntime.start(ProcessPayloadBuilder.start()
				.withBusinessKey(String.valueOf(id)).withProcessDefinitionKey(PROCESS_DEFINETION_KEY).build());

		ApplicationProcess process = new ApplicationProcess();
		process.setBusinessKey(String.valueOf(id));
		process.setBusinessModel(BUSINESS_MODEL);
		process.setTime(LocalDateTime.now());
		process.setProcess(instance.getId());

		processRepo.save(process);
	}

	@EventListener(classes = BpmnEvent.class)
	public void listenBpmnEvent(BpmnEvent event) {
		if (PROCESS_DEFINETION_KEY.equals(event.getDefKey())) {
			long id = Long.parseLong(event.getBusinessKey());
			Applicant applicant = repo.getOne(id);
			applicant.setStatus(event.getStatus());
			repo.save(applicant);
		}
	}
}