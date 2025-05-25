package com.team.berp.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

//서버(DB)에 OrderLineItem 데이터를 입력하는 DTO,  즉 사용자의 입력값을 받는 DTO
//기본적으로 OrderLineItem 엔티티를 중심으로 하여, 외래키로 참조하는 엔티티들의 칼럼을 모두 가져옴

@Data
public class OrderOrderLineItemFormDto {
	private Integer order_line_item_id;
	
	private Integer order_id;
	
	private Integer item_id;
	
	@NotNull @Min(1)
    private Integer order_qty;
}
