package com.team.berp.plant.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.team.berp.plant.DTO.plant_DTO;

@Mapper
public interface plant_mapper {

	//사업장 정보 insert
	int workplace_info(plant_DTO dto);
	
	//사업장 정보 select - 단일 select
	//쿼리 결과가 한 건이면 plant_DTO / 여러 건이면 List<plant_DTO>
	//단일 문자열을 조건으로 하나만 조회할 때
//	plant_DTO workplace_list(String workplace_id); 
	
	//단일 문자열을 조건으로 여러 건 조회할 때
	List<plant_DTO> workplace_list(String workplace_id);
	
	//전체 리스트 select
	List<plant_DTO> workplace_list_all();
	
	//목록 삭제
	int workplace_list_del(long workplace_id);
	
	//목록 수정
	int updateWorkplace(plant_DTO dto);
}
