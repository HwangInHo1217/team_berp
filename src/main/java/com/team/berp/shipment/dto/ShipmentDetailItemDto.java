package com.team.berp.shipment.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 출고 내역 상세 모달에서 각 품목별 정보 DTO
 */
@Data
@AllArgsConstructor
public class ShipmentDetailItemDto {
    private String itemName;
    private String itemCode;
    private String unit;
    private BigDecimal unitPrice;
    private Long unitQty;
    private BigDecimal total;
    private String warehouseName;
    private Long stockQty;
    // (추가로 필요하다면, 이곳에 MRP 알림용 boolean flag 등을 둘 수 있습니다)
}