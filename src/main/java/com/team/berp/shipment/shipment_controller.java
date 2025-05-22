package com.team.berp.shipment;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class shipment_controller {

	@GetMapping("pages/shipment/shipment.html")
	public String shipment() {
		
		return "shipment/shipment";
	}
}
