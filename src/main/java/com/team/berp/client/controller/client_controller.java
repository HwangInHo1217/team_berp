package com.team.berp.client.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class client_controller {

	@GetMapping("/pages/client/client.html")
	public String client() {
		
		return "client/client";
	}
}
