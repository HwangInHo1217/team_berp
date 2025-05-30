package com.team.berp.order.dto;
import lombok.Data;
@Data
public class OrderLineItemDto {
    private String itemName;
    private Long unitQty;
    private String unit;
    private Long unitPrice;
    private Long unitPriceAll;
}