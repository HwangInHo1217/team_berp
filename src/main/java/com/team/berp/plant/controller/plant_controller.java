package com.team.berp.plant.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.team.berp.plant.DTO.plant_DTO;
import com.team.berp.plant.mapper.plant_DAO;
import com.team.berp.plant.service.plant_service;

import jakarta.annotation.Resource;

@Controller
public class plant_controller {

	@Resource(name="plant_DTO")
	plant_DTO dto;
	
	@Autowired
	private plant_service ps;
	
	@GetMapping("/pages/plant.html") //url 경로
	public String plant() {
			ps.workplace_info(dto);
		return "plant/plant"; //파일 경로 - src/main/resources/templates/plant/plant(.html)
	}
	
//	@PostMapping("")
	
}
