// File: src/main/java/com/team/berp/order/dto/OrderLineItemDto.java
package com.team.berp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 주문 라인 아이템 DTO */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderLineItemDto {
    private Long orderLineItemId; // PK
    private String itemName;      // 품목명
    private String unit;          // 단위
    private Long unitPrice;       // 단가
    private Long unitQty;         // 수량
    private Long unitPriceall;    // 합계 (unitPrice * unitQty)
}