package com.team.berp.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderController {
	Logger log = LoggerFactory.getLogger(this.getClass());
	/*
	@Autowired
	private order order;
	*/
	
	@GetMapping("/order")
	public String orderReturn(Model model) {
		
        return "order/order";
	}
}
