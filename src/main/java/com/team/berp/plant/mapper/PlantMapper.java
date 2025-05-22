package com.team.berp.plant.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.team.berp.plant.DTO.plant_DTO;

@Mapper
public interface PlantMapper {

	String workplace_info(plant_DTO dto);
	
}
