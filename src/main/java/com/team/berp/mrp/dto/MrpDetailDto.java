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
    private Long   mrpId;
    private String createDate;
    private String dueDate;
    private String planType;
    private String status;

    // ───────────────────────────────────────────
    // B. 품목 정보
    private String itemCode;
    private String itemName;
    private String itemType;
    private String unit;
    private String spec;
    private int    safetyStock;
    private int    stockQty;
    private String location;

    // ───────────────────────────────────────────
    // C. 수량·리드타임
    private int    requiredQty;
    private int    shortageQty;
    private int    purchaseLeadTime;
    private int    productionLeadTime;
    private String orderableDate;

    // ───────────────────────────────────────────
    // D. BOM 구성
    private List<MrpBomComponent> bomComponents;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MrpBomComponent {
        private String childCode;
        private String childName;
        private int    perParentQty;
        private int    totalQty;
        private int    stockQty;
        private int    shortageQty;
        private int    leadTime;
        // [수정] 아래 두 필드를 추가합니다.
        private int    safetyStock;
        private int    purchaseQty;
    }

    // ───────────────────────────────────────────
    // E. 연계 오더 현황
    private List<MrpPurchaseOrder> purchaseOrders;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MrpPurchaseOrder {
        private String poNo;
        private String itemCode;
        private int    qty;
        private String dueDate;
        private String status;
    }

    private List<MrpWorkOrder> workOrders;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MrpWorkOrder {
        private String woNo;
        private String itemCode;
        private int    qty;
        private String startDate;
        private String endDate;
        private String status;
    }

    // ───────────────────────────────────────────
    // F. 스케줄·이력
    private List<MrpHistory> history;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MrpHistory {
        private String timestamp;
        private String message;
    }
}