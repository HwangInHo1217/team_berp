package com.team.berp.plan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProdPlanController {
	
	@GetMapping("/plan")
	public String getPlanPage() {
		return "plan/plan";
	}
}
