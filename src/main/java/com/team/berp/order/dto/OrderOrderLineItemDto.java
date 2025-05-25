package com.team.berp.order.dto;

import lombok.Data;

//서버(DB)에서 OrderLineItem 데이터를 가져오는 DTO
//기본적으로 OrderLineItem 엔티티를 중심으로 하여, 외래키로 참조하는 엔티티들의 칼럼을 모두 가져옴

@Data
public class OrderOrderLineItemDto {
	private Integer order_line_item_id;
	private Integer order_id;
	private Integer item_id;
	private Integer order_qty;
	private Integer unit_price;
	private String item_code;
}
