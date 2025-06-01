// File: /Team_BERP/src/main/java/com/team/berp/mrp/dto/MrpDetailDto.java
package com.team.berp.mrp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MrpDetailDto {
    // ───────────────────────────────────────────
    // A. 기본 정보
    private Long   mrpId;           // MRP 번호 (PK)
    private String createDate;      // 요청 일자 (기준일자)
    private String dueDate;         // 필요 일자
    private String planType;        // 계획 타입 (예: PRODUCTION)
    private String status;          // MRP 상태 (예: PLANNED, RELEASED, CLOSED)
    private String ownerName;       // 담당자/부서

    // ───────────────────────────────────────────
    // B. 품목 정보
    private String itemCode;        // 품목 코드
    private String itemName;        // 품목명
    private String itemType;        // 품목 유형 (raw / product)
    private String unit;            // 단위
    private String spec;            // 사양 (Spec)
    private int    safetyStock;     // 안전 재고
    private int    stockQty;        // 현재 가용 재고
    private String location;        // 재고 위치

    // ───────────────────────────────────────────
    // C. 수량·리드타임
    private int    requiredQty;         // 요청 수량
    private int    shortageQty;         // 부족 수량
    private int    purchaseLeadTime;    // 구매 리드타임(일)
    private int    productionLeadTime;  // 생산 리드타임(일)
    private String orderableDate;       // 주문 가능 일자

    // ───────────────────────────────────────────
    // D. BOM 구성
    private List<MrpBomComponent> bomComponents;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MrpBomComponent {
        private String childCode;       // 부품 코드
        private String childName;       // 부품명
        private int    perParentQty;    // 구성 수량 per 모품
        private int    totalQty;        // 총 필요 수량 (perParentQty * requiredQty)
        private int    stockQty;        // 가용 재고
        private int    shortageQty;     // 부족 수량
        private int    leadTime;        // 리드 타임(구매)
        private String supplier;        // 공급처
    }

    // ───────────────────────────────────────────
    // E. 연계 오더 현황
    private List<MrpPurchaseOrder> purchaseOrders;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MrpPurchaseOrder {
        private String poNo;       // PO 번호
        private String itemCode;   // 품목 코드
        private int    qty;        // 수량
        private String dueDate;    // 납기일
        private String status;     // 상태
    }

    private List<MrpWorkOrder> workOrders;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MrpWorkOrder {
        private String woNo;       // WO 번호
        private String itemCode;   // 품목 코드
        private int    qty;        // 수량
        private String startDate;  // 시작일
        private String endDate;    // 종료 예정일
        private String status;     // 상태
    }

    // ───────────────────────────────────────────
    // F. 스케줄·이력
    private List<MrpHistory> history;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MrpHistory {
        private String timestamp;  // 타임스탬프
        private String message;    // 로그 메시지
    }
}
