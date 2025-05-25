package com.team.berp.order.dto;

import lombok.Data;

@Data
public class OrderItemDto {
	private Integer item_id;
	private String item_name;
	private String unit;
}
