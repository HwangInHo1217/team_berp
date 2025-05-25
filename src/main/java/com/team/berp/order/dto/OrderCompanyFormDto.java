package com.team.berp.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderCompanyFormDto {
	private Integer company_id;
	
	@NotNull
	@Size(max = 100)
    private String company_name;
	
	private Integer employee_id;
}
