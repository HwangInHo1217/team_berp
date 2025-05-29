package com.team.berp.order.dto;

/**
 * 주문 한 건의 라인 아이템(품목) 정보를 담는 DTO
 */
public class OrderLineItemDto {
    private Long    orderLineItemId; // 라인아이템 PK
    private String  itemName;        // 품목명
    private String  unit;            // 단위(EA 등)
    private Long    unitQty;         // 수량
    private Long    unitPrice;       // 단가
    private Long    unitPriceAll;    // 합계(수량*단가)

    public OrderLineItemDto() {}

    public OrderLineItemDto(Long orderLineItemId,
                            String itemName,
                            String unit,
                            Long unitQty,
                            Long unitPrice,
                            Long unitPriceAll) {
        this.orderLineItemId = orderLineItemId;
        this.itemName        = itemName;
        this.unit            = unit;
        this.unitQty         = unitQty;
        this.unitPrice       = unitPrice;
        this.unitPriceAll    = unitPriceAll;
    }

    // Getter/Setter
    public Long getOrderLineItemId() { return orderLineItemId; }
    public void setOrderLineItemId(Long orderLineItemId) {
        this.orderLineItemId = orderLineItemId;
    }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getUnit() { return unit; }
    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getUnitQty() { return unitQty; }
    public void setUnitQty(Long unitQty) {
        this.unitQty = unitQty;
    }

    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Long getUnitPriceAll() { return unitPriceAll; }
    public void setUnitPriceAll(Long unitPriceAll) {
        this.unitPriceAll = unitPriceAll;
    }
}
