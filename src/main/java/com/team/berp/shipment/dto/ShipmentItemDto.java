/*
 * DTO: ShipmentItemDto.java
 * Purpose: Represents detailed information for each item in a shipment.
 */
package com.team.berp.shipment.dto;

/**
 * Shipment item detail data transfer object
 */
public class ShipmentItemDto {
    private String itemName;
    private String itemCode;
    private String unit;
    private Long unitQty;
    private Long unitPrice;
    private Long unitPriceAll;
    private String warehouseName;
    private Long stockQuantity;

    // Constructor
    public ShipmentItemDto(String itemName, String itemCode, String unit,
                           Long unitQty, Long unitPrice, Long unitPriceAll,
                           String warehouseName, Long stockQuantity) {
        this.itemName = itemName;
        this.itemCode = itemCode;
        this.unit = unit;
        this.unitQty = unitQty;
        this.unitPrice = unitPrice;
        this.unitPriceAll = unitPriceAll;
        this.warehouseName = warehouseName;
        this.stockQuantity = stockQuantity;
    }

    // Getters and setters
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Long getUnitQty() { return unitQty; }
    public void setUnitQty(Long unitQty) { this.unitQty = unitQty; }

    public Long getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Long unitPrice) { this.unitPrice = unitPrice; }

    public Long getUnitPriceAll() { return unitPriceAll; }
    public void setUnitPriceAll(Long unitPriceAll) { this.unitPriceAll = unitPriceAll; }

    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }

    public Long getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Long stockQuantity) { this.stockQuantity = stockQuantity; }
}
