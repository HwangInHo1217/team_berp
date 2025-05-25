package com.team.berp.plant.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.team.berp.plant.DTO.plant_DTO;

@Service
public interface plant_service {
	
	public int workplace_info(plant_DTO dto);
	List<plant_DTO> workplace_list(String workplace_id);
	List<plant_DTO> workplace_list_all();
	public int workplace_list_del(long workplace_id);
}
