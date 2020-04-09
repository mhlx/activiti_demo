package com.acrel;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.acrel.service.ApplicantService;

@Controller
public class ActivitiController {

	private final ApplicantService service;

	public ActivitiController(ApplicantService service) {
		super();
		this.service = service;
	}

	@GetMapping
	public String index(Model model) {
		model.addAttribute("cants", service.getAllApplicants());
		return "applicants";
	}

	@GetMapping("createApplicant")
	public String create() {
		return "applicant";
	}

	@GetMapping("start")
	public String create(@RequestParam("id") long id) {
		service.startProcess(id);
		return "redirect:/";
	}

	@PostMapping("saveApplicant")
	public String save(Applicant cant) {
		service.saveApplicant(cant);
		return "redirect:/";
	}

	@GetMapping("check")
	public String my(Model model) {
		model.addAttribute("cants", service.getNeedChecks(0, 10).getContent());
		return "check";
	}

	@GetMapping("claim")
	public String claim(@RequestParam("taskId") String taskId) {
		service.claimTask(taskId);
		return "redirect:/check";
	}

	@GetMapping("approve")
	public String approve(@RequestParam("taskId") String taskId) {
		service.complete(taskId, "approve", "");
		return "redirect:/check";
	}

	@GetMapping("reject")
	public String reject(@RequestParam("taskId") String taskId) {
		service.complete(taskId, "reject", "");
		return "redirect:/check";
	}

	@GetMapping("revoke")
	public String revoke(@RequestParam("taskId") String taskId) {
		service.complete(taskId, "revoke", "");
		return "redirect:/check";
	}

	@GetMapping("print")
	@ResponseBody
	public void print() {
		service.printHistory(1);
	}
}
