package com.team.berp.item.dto;

import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UpdateItemRequestDTO {
	private Long id;
	private String name;
	private String type;
	private String unit;
	private String spec;
	private String use;
	private Integer safetyStock;       // nullable
	private Integer purchaseLeadTime;  // nullable
}