package com.team.berp.place.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value="/place")
public class PlaceController {

	@GetMapping
	public String placePage() {
		return "place/place";
	}
}
