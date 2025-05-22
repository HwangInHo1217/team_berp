package com.team.berp.plant.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.team.berp.plant.DTO.plant_DTO;
import com.team.berp.plant.service.plant_service;

@Repository("plant_DAO")
public class plant_DAO implements plant_service {

	@Autowired
	PlantMapper pm;
	
	@Override
	public String workplace_info(plant_DTO dto) {
		return pm.workplace_info(dto);
	}
}
