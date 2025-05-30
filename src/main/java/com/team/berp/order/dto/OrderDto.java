// OrderDto.java
package com.team.berp.order.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

/**
 * 주문과 주문품목 정보를 모두 포함하는 DTO
 */
@Data
public class OrderDto {
    // 주문 기본 정보
    private Long orderId;                     // 주문 고유 ID
    private String orderNum;                  // 주문 번호 (cus-001 등)
    private LocalDate orderDate;              // 주문일자
    private Long companyId;                   // 고객사 ID
    private String companyName;               // 고객사 이름
    private String empName;                   // 담당자 이름
    private String companyEmpName;            // 거래처 담당자 이름
    private String orderType = "CUSTOMER";  // 주문 타입 (고정: CUSTOMER)
    private String itemType = "product";    // 품목 타입 (고정: product)
    private Integer orderQty;                 // 품목 개수
    private Long amount;                     // 총합계
    private String remark;                    // 비고 (선택)

    // 주문품목 리스트
    private List<LineItem> items;

    @Data
    public static class LineItem {
        private Long orderLineItemId;        // 품목별 고유 ID
        private Long itemId;                 // 품목 ID
        private String itemName;             // 품목 이름
        private String unit;                 // 단위
        private Long unitPrice;             // 단가
        private Integer unitQty;             // 수량
        private Long unitPriceAll;          // 합계
    }
}
