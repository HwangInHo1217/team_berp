package com.team.berp.plant.DTO;

import org.springframework.stereotype.Repository;

import lombok.Data;

@Data
@Repository("plant_DTO")
public class plant_DTO {
 
	String workId, workName, workPreName, workNo, workCond;
	String workItem, workTel, workFax, workAddr;
	String workManName, workManEmail, workManTel;
}
