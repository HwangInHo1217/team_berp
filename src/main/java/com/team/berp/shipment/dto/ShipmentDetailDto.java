package com.team.berp.shipment.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 출고 내역 리스트 / 상세 모달에서 쓰이는 DTO
 */
@Data
@NoArgsConstructor
public class ShipmentDetailDto {
    private Long shipmentId;            // InventoryLog.id
    private String useYn;               // 출고 여부 (Y/N)
    private LocalDateTime shippedAt;    // 출고일시 (lastStokedDate)
    private String companyName;         // 고객사명
    private Long orderQty;              // 주문 품목 수
    private Long amount;                // 총 금액
    private String empName;             // 담당자
    private String companyEmpName;      // 거래처 담당자
    private List<ShipmentDetailItemDto> items; // 실제 출고 상세 품목
}