package com.team.berp.plant.DTO;

import org.springframework.stereotype.Repository;

import lombok.Data;

@Data
@Repository("plant_DTO")
public class plant_DTO {
 
	//번호(자동 순번)
	private int workplace_id;
	
	//사업자 명, 사업자 대표, 사업자 번호, 업태
	private String workplace_name, workplace_president_nm, workplace_no, workplace_cond;
	
	//품목(종목), 전화번호, 팩스, 주소
	private String workplace_item, workplace_tel, workplace_fax, workplace_addr;
	
	//회사 담당자명, 이메일, 번호(직통번호)
	private String workplace_manname, workplace_manemail, workplace_mantel;
}
