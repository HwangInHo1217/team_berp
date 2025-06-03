// src/main/java/com/team/berp/order/dto/ItemWarehouseResponse.java
package com.team.berp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 특정 주문(orderId)에 속한 각 주문상품(orderLineItem)에 대해
 * “어떤 창고(warehouse)에 얼마(stockQty)가 남아 있는지” 알려주기 위한 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemWarehouseResponse {
    private Long orderLineItemId;   // OrderLineItem의 PK
    private Long itemId;            // Item의 PK
    private String itemCode;        // Item.code
    private String itemName;        // Item.name
    private Integer orderQty;       // 주문 수량(orderLineItem.quantity)

    private Long warehouseId;       // Warehouse PK
    private String warehouseName;   // Warehouse.warehouseName
    private Integer stockQty;       // 해당 창고에 남은 재고 수량
}
