package com.team.berp.item.dto;

import com.team.berp.domain.Item;

import lombok.Getter;

@Getter
public class ItemListViewResponse {
	private final Long id;
	private final String code;
	private final String name;
	private final String type;
	private final String spec;
	private final String unit;
	private final String use;
	
	public ItemListViewResponse(Item item){
		this.id=item.getId();
		this.code=item.getCode();
		this.name=item.getName();
		this.type=item.getType().name();
		this.spec=item.getSpec();
		this.unit=item.getUnit();
		this.use=item.getUse();
		
	}
	
}
