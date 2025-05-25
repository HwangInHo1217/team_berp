package com.team.berp.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderItemFormDto {
	private Integer item_id;
	
	@NotBlank
    @Size(max = 100)
    private String item_name;
}
