package com.team.berp.stock.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.Getter;

@Controller
public class stock_controller {

	@GetMapping("/stock")
public String warehouse() {
		
		
		
		return "stock/stock";
	}
	
	
}
