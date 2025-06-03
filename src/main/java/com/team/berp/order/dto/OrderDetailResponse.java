package com.team.berp.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 주문 상세 조회 응답 DTO (주문 헤더 + 품목 목록)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse {

    // 주문 헤더 정보
    private Long orderId;                  // 주문 ID
    private String orderNum;               // 주문번호
    private LocalDate orderDate;           // 주문일자
    private Long companyId;                // 고객사 ID
    private String companyName;            // 고객사 이름
    private String empName;                // 담당자명
    private String companyEmpName;         // 거래처 담당자명
    private String orderType = "CUSTOMER"; // 주문 타입
    private String itemType = "product";   // 품목 타입
    private Integer orderQty;                 // 전체 수량
    private Long amount;                   // 전체 금액
    private String remark;                 // 비고
    private Long warehouseId;              // (선택) 전체 창고 ID

    // 주문 상세 목록
    private List<LineItem> items;

    @Data
    public static class LineItem {
        private Long orderLineItemId;      // 상세 항목 ID
        private Long itemId;               // 품목 ID
        private String itemName;           // 품목 이름 ✅ 추가
        private String unit;               // 단위 ✅ 추가
        private Long unitPrice;            // 단가
        private Integer unitQty;              // 수량
        private Long unitPriceAll;         // 금액
        private Long warehouseId;          // 창고 ID
        // → “이미 출고가 CONFIRMED 되어 있으면 true”
        private boolean alreadyShipped;
    }
}


