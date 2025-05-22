package com.team.berp.receive;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class receive_controller {

	@GetMapping("pages/receive/receive.html")
	public String receive() {
		
		return "receive/receive"; 
	}
	
}	
