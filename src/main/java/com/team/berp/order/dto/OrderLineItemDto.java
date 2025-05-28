package com.example.order.dto;

/**
 * 주문 품목 정보용 DTO
 */
public class OrderLineItemDto {
    private Long orderLineItemId;
    private String itemName;
    private String unit;
    private int unitPrice;
    private int unitQty;
    private int unitPriceAll;

    public OrderLineItemDto(Long orderLineItemId, String itemName,
                            String unit, int unitPrice,
                            int unitQty, int unitPriceAll) {
        this.orderLineItemId = orderLineItemId;
        this.itemName = itemName;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.unitQty = unitQty;
        this.unitPriceAll = unitPriceAll;
    }

    // Getters and setters omitted for brevity
}