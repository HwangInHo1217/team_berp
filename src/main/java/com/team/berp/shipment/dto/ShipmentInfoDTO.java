package com.team.berp.shipment.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentInfoDTO {
    private Long logId;                // (순번) InventoryLog ID
    private LocalDateTime logDatetime; // (출고일자) InventoryLog.logDatetime
    private String companyName;        // (고객사명) CompanyOrder → Company → name
    private String itemCode;           // (품목코드) Item → code
    private String itemName;           // (품목명) Item → name
    private Integer quantity;          // (수량) InventoryLog.quantity
    private String unit;               // (단위) Item → unit (예: EA, KG, M 등)
    private String warehouseName;      // (창고) Warehouse → warehouseName
    private String companyEmpName;     // (담당자) InventoryLog.comment 혹은 CompanyOrder.employee → name
    private String comment;            // (비고) InventoryLog.comment
}