package com.team.berp.order.dto;

import lombok.Data;

@Data
public class OrderItemRequest {
    private Long itemId;
    private String unit;
    private Long unitPrice;
    private Long unitQty;
}
