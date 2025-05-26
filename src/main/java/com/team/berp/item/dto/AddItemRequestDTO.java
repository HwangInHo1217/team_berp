package com.team.berp.item.dto;

import com.team.berp.domain.Item;
import com.team.berp.domain.ItemType;

import lombok.AllArgsConstructor;
import lombok.Data;

import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class AddItemRequestDTO {
	private String name;
	private String type;
	private String unit;
	private String spec;
	private String use;

	public Item toEntity(String code) {
		System.out.println("spec확인" + spec);
		return Item.builder()
				.code(code)
				.name(name)
				.type(ItemType.valueOf(this.type))
				.spec(spec)
				.unit(unit)
				.use(use)
				.build();
	}
}

	