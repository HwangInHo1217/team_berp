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
	private Long itemPrice;
	private Integer safetyStock;
	private Integer purchaseLeadTime;
	public Item toEntity(String code) {
	    return Item.builder()
	            .code(code)
	            .name(name)
	            .type(ItemType.valueOf(this.type))
	            .spec(spec)
	            .unit(unit)
	            .use(use)
	            .itemPrice(itemPrice)
	            .safetyStock(safetyStock)
	            .purchaseLeadTime(purchaseLeadTime)
	            .build();
	}
}

	