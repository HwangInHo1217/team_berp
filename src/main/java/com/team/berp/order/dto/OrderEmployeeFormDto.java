package com.team.berp.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderEmployeeFormDto {
	private Integer employee_id;
	
	@NotBlank
    @Size(max = 50)
    private String emp_name;
}
