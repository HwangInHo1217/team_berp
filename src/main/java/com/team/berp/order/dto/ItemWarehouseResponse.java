package com.team.berp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemWarehouseResponse {// 창고 ID, 창고 이름, 아이템 Id, 아이템 이름, 보유 수량 정보를 담는 DTO
	private Long itemId;
	private String itemName;
    private Long warehouseId;
    private String warehouseName;
    private Integer quantity;

  
}
