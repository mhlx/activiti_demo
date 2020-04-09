package com.acrel;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.task.Comment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import com.acrel.service.ApplicantService;

@SuppressWarnings("deprecation")
@RunWith(SpringRunner.class)
@WebAppConfiguration
@SpringBootTest(classes = ActivitiApplication.class)
public class HireProcessTest {

	@Autowired
	private ApplicantService service;
	@Autowired
	private SecurityUtil util;
	@Autowired
	private ProcessEngine engine;
	@Autowired
	private TaskRuntime taskRuntime;
	@Autowired
	private ProcessRuntime processRuntime;

	@Test
	public void testHappyPath() {
		util.logInAs("user");
		service.saveApplicant(new Applicant("1", "2", "3"));
		String id = processRuntime.start(ProcessPayloadBuilder.start().withProcessDefinitionKey("hireProcessWithJpa")
				.withBusinessKey("1").build()).getId();
		util.logInAs("dev");
		complete(id, "approve", "同意");
		util.logInAs("devm");
		complete(id, "approve", "同意");
		util.logInAs("finance");
		complete(id, "revoke", "退回重新审核");
		util.logInAs("dev");
		complete(id, "approve", "同意");
		util.logInAs("devm");
		complete(id, "approve", "同意");
		util.logInAs("finance");
		complete(id, "approve", "同意");

		HistoryService hs = engine.getHistoryService();
		List<HistoricTaskInstance> list = hs.createHistoricTaskInstanceQuery().processInstanceId(id)
				.orderByTaskCreateTime().asc().orderByTaskAssignee().asc().list();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		for (HistoricTaskInstance ins : list) {
			if (ins.getAssignee() == null)
				continue;
			String date = ins.getEndTime() == null ? "" : sdf.format(ins.getEndTime());
			HistoricVariableInstance hvi = hs.createHistoricVariableInstanceQuery().taskId(ins.getId())
					.variableName(ins.getTaskDefinitionKey() + "_signal").singleResult();
			String status = hvi == null ? "未知" : getStatus(hvi.getValue());
			List<Comment> comments = engine.getTaskService().getTaskComments(ins.getId());
			String commentStr = comments.stream().map(Comment::getFullMessage)
					.collect(Collectors.joining(System.lineSeparator()));
			System.out.println(date + "   " + ins.getAssignee() + "   " + status + "   " + commentStr);
		}
	}

	private String getStatus(Object v) {
		String vs = Objects.toString(v);
		if ("approve".equalsIgnoreCase(vs))
			return "通过";
		if ("reject".equalsIgnoreCase(vs))
			return "拒绝";
		if ("revoke".equalsIgnoreCase(vs))
			return "驳回";
		return "未知";
	}

	private void complete(String id, String signal, String comment) {
		String taskId = taskRuntime.tasks(Pageable.of(0, 10)).getContent().get(0).getId();
		String taskKey = engine.getTaskService().createTaskQuery().taskId(taskId).singleResult().getTaskDefinitionKey();
		engine.getTaskService().addComment(taskId, id, comment);
		taskRuntime.claim(TaskPayloadBuilder.claim().withTaskId(taskId).build());
		taskRuntime.complete(
				TaskPayloadBuilder.complete().withTaskId(taskId).withVariable(taskKey + "_signal", signal).build());

	}
}