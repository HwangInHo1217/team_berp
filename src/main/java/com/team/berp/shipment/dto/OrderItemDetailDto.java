package com.team.berp.shipment.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 출고 등록·상세 모달에서 표시할 주문 품목 정보 DTO
 */
@Data
@AllArgsConstructor
public class OrderItemDetailDto {
    private String itemName;     // 품목명
    private String itemCode;     // 품목코드
    private String unit;         // 단위
    private BigDecimal unitPrice;// 단가
    private Long unitQty;        // 주문 수량
    private BigDecimal total;    // 단가 * 수량
    private String warehouseName;// 창고명
    private Long stockQty;       // 현재 재고량
}