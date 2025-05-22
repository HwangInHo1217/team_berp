package com.team.berp.plant.service;

import org.springframework.stereotype.Service;

import com.team.berp.plant.DTO.plant_DTO;

@Service
public interface plant_service {
	
	public String workplace_info(plant_DTO dto);
}
