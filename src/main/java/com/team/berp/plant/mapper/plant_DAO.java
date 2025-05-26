package com.team.berp.plant.mapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.team.berp.plant.DTO.plant_DTO;
import com.team.berp.plant.service.plant_service;

@Repository("plant_DAO")
public class plant_DAO implements plant_service {

	@Autowired
	plant_mapper pm;
	
	@Override
	public int workplace_info(plant_DTO dto) {
		return pm.workplace_info(dto);
	}
	
	@Override
	public List<plant_DTO> workplace_list(String workplace_id) {
		return pm.workplace_list(workplace_id);
	}
	
	@Override
	public List<plant_DTO> workplace_list_all() {
		return pm.workplace_list_all();
	}
	
	@Override
	public int workplace_list_del(long workplace_id) {
		return pm.workplace_list_del(workplace_id);
	}
}
